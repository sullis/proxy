package controllers

import io.flow.common.v0.models.Error
import io.flow.common.v0.models.json._
import io.flow.token.v0.{Client => TokenClient}
import io.flow.organization.v0.{Client => OrganizationClient}
import io.flow.token.v0.models.TokenReference
import javax.inject.{Inject, Singleton}
import lib.{Authorization, AuthorizationParser, Config, InternalRoute, Service, ServicesConfig}
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.ws.{WSClient, StreamedResponse}
import play.api.libs.json.Json
import play.api.mvc._
import org.apache.commons.codec.binary.Base64
import scala.concurrent.Future

@Singleton
class ReverseProxy @Inject () (
  authorizationParser: AuthorizationParser,
  config: Config,
  wsClient: WSClient,
  servicesConfig: ServicesConfig
) extends Controller {

  private[this] val organizationServiceUrl = config.requiredString("service.organization.uri")
  private[this] val organizationClient = new OrganizationClient(baseUrl = organizationServiceUrl)

  private[this] val tokenServiceUrl = config.requiredString("service.token.uri")
  private[this] val tokenClient = new TokenClient(baseUrl = tokenServiceUrl)

  private[this] val virtualHostName = config.requiredString("virtual.host.name")

  // WS Client defaults to application/octet-stream. Given this proxy
  // is for APIs only, assume JSON if no content type header is
  // provided.
  private[this] val DefaultContentType = "application/json"

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  private[this] val services = servicesConfig.current()

  def handle = Action.async(parse.raw) { request: Request[RawBuffer] =>
    services.resolve(request.method, request.path) match {
      case None => Future {
        Logger.info(s"Unrecognized path[${request.path}] - returning 404")
        NotFound
      }

      case Some(internalRoute) => {
        authorizationParser.parse(request.headers.get("Authorization")) match {
          case Authorization.NoCredentials => {
            proxyWithOrg(request, internalRoute, userId = None)
          }

          case Authorization.Unrecognized => Future {
            unauthorized(s"Authorization header value must start with one of: " + Authorization.Unrecognized.valid.mkString(", "))
          }

          case Authorization.InvalidApiToken => Future {
            unauthorized(s"API Token is not valid")
          }

          case Authorization.InvalidJwt => Future {
            unauthorized(s"JWT Token is not valid")
          }

          case Authorization.Token(token) => {
            resolveToken(token).flatMap {
              case None => Future {
                unauthorized(s"API Token is not valid")
              }
              case Some(ref) => {
                proxyWithOrg(request, internalRoute, userId = Some(ref.user.id))
              }
            }
          }

          case Authorization.User(userId) => {
            proxyWithOrg(request, internalRoute, userId = Some(userId))
          }
        }
      }
    }
  }

  private[this] def resolveToken(token: String): Future[Option[TokenReference]] = {
    tokenClient.tokens.getByToken(token).map { tokenReference =>
      Some(tokenReference)
    }.recover {
      case io.flow.token.v0.errors.UnitResponse(404) => {
        None
      }

      case ex: Throwable => {
        sys.error(s"Could not communicate with token service at[$tokenServiceUrl]: $ex")
      }
    }
  }

  private[this] def proxyWithOrg(request: Request[RawBuffer], internalRoute: InternalRoute, userId: Option[String]): Future[Result] = {
    internalRoute.organization(request.path) match {
      case None  => {
        proxy(
          request,
          internalRoute.service,
          userId = userId,
          organization = None
        )
      }

      case Some(org) => {
        userId match {
          case None => Future {
            unauthorized("You must set a valid Authorization header")
          }

          case Some(uid) => {
            Logger.info(s"Checking membership of user[$uid] in organization[$org]")
            organizationClient.memberships.get(
              user = Some(uid),
              organization = Some(org),
              limit = 1,
              requestHeaders = Seq("Authorization" -> request.headers.get("Authorization").getOrElse(""))
            ).flatMap { memberships =>
              memberships.headOption match {
                case None => Future {
                  unauthorized(s"Not authorized to access $org or the organization does not exist")
                }

                case Some(membership) => {
                  Logger.info(s"Authorized: user[$uid] is a ${membership.role} of organization[$org]")
                  proxy(
                    request,
                    internalRoute.service,
                    userId = Some(uid),
                    organization = Some(org)
                  )
                }
              }
            }.recover {
              case io.flow.organization.v0.errors.UnitResponse(401) => {
                unauthorized(s"This API key is either not authorized to access $org or the organization does not exist")
              }

              case ex: Throwable => {
                sys.error(s"Could not communicate with token service at[$tokenServiceUrl]: $ex")
              }
            }
          }
        }
      }
    }
  }

  private[this] def proxy(request: Request[RawBuffer], service: Service, userId: Option[String], organization: Option[String]) = {
    Logger.info(s"Proxying ${request.method} ${request.path} to service[${service.name}] ${service.host}${request.path} userId[${userId.getOrElse("none")}] organization[${organization.getOrElse("none")}]")
    val finalHeaders = proxyHeaders(
      request.headers,
      Seq(
        "X-Flow-Proxy-Service" -> Some(service.name),
        "X-Flow-User-Id" -> userId,
        "X-Flow-Organization" -> organization
      ).flatMap { case (k, v) => v.map { (k, _) } }
    )

    // Create the request to the upstream server:
    val proxyRequest = wsClient.url(service.host + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withVirtualHost(virtualHostName)
      .withHeaders(finalHeaders.headers: _*)
      .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
      .withBody(request.body.asBytes().get)

    proxyRequest.stream.map {
      case StreamedResponse(response, body) => {
        // Get the content type
        val contentType: Option[String] = response.headers.get("Content-Type").flatMap(_.headOption)

        // If there's a content length, send that, otherwise return the body chunked
        response.headers.get("Content-Length") match {
          case Some(Seq(length)) => {
            Ok.sendEntity(HttpEntity.Streamed(body, Some(length.toLong), contentType))
          }

          case _ => {
            contentType match {
              case None => Ok.chunked(body)
              case Some(ct) => Ok.chunked(body).as(ct)
            }
          }
        }
      }
    }
  }

  /**
    * Modifies headers by:
    *   - adding a default content-type
    *   - adding all additional headers specified
    */
  private[this] def proxyHeaders(headers: Headers, additional: Seq[(String, String)]): Headers = {
    val all = (
      headers.get("Content-Type") match {
        case None => headers.add("Content-Type" -> DefaultContentType)
        case Some(_) => headers
      }
    )
    additional.foldLeft(all) { case (h, (k, v)) => h.add(k -> v) }
  }

  private[this] def unauthorized(message: String) = {
    Unauthorized(
      Json.toJson(
        Seq(
          Error("authorization_failed", "Please add an authorization header. Flow APIs expect a valid API token using basic authorization.")
        )
      )
    )
  }

}
