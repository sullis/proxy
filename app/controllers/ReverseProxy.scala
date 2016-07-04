package controllers

import io.flow.common.v0.models.Error
import io.flow.common.v0.models.json._
import io.flow.token.v0.{Client => TokenClient}
import io.flow.organization.v0.{Client => OrganizationClient}
import javax.inject.{Inject, Singleton}
import lib.{Service, ServicesConfig}
import play.api.{Configuration, Logger}
import play.api.http.HttpEntity
import play.api.libs.ws.{WSClient, StreamedResponse}
import play.api.libs.json.Json
import play.api.mvc._
import org.apache.commons.codec.binary.Base64
import scala.concurrent.Future

@Singleton
class ReverseProxy @Inject () (
  configuration: Configuration,
  wsClient: WSClient,
  servicesConfig: ServicesConfig
) extends Controller {

  private[this] val organizationServiceUrl = configuration.getString("service.organization.uri").getOrElse {
    sys.error("Missing configuration parameter[service.organization.uri]")
  }
  private[this] val organizationClient = new OrganizationClient(baseUrl = organizationServiceUrl)

  private[this] val tokenServiceUrl = configuration.getString("service.token.uri").getOrElse {
    sys.error("Missing configuration parameter[service.token.uri]")
  }
  private[this] val tokenClient = new TokenClient(baseUrl = tokenServiceUrl)

  private[this] val VirtualHostName = "api.flow.io"

  // WS Client defaults to application/octet-stream. Given this proxy
  // is for APIs only, assume JSON if no content type header is
  // provided.
  private[this] val DefaultContentType = "application/json"

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  private[this] val services = servicesConfig.current()

  def handle = Action.async(parse.raw) { request: Request[RawBuffer] =>
    services.resolve(request.method, request.path) match {
      case Some(internalRoute) => {
        internalRoute.organization(request.path) match {
          case None  => {
            proxy(request, internalRoute.service)
          }

          case Some(org) => {
            parseToken(request.headers.get("Authorization")) match {
              case None => {
                Logger.info("Unauthorized request - no authorization header present")
                Future {
                  Unauthorized(Json.toJson(Seq(
                    Error("authorization_failed", "Please add an authorization header. Flow APIs expect a valid API token using basic authorization.")
                  )))
                }
              }

              case Some(token) => {
                tokenClient.tokens.getByToken(token).flatMap { tokenReference =>

                  Logger.info(s"Checking membership of user[${tokenReference.user.id}] in organization[$org]")                  

                  organizationClient.memberships.get(
                    user = Some(tokenReference.user.id),
                    organization = Some(org),
                    limit = 1,
                    requestHeaders = Seq("Authorization" -> request.headers.get("Authorization").getOrElse(""))
                  ).flatMap { memberships =>
                    memberships.headOption match {
                      case None => Future {
                        Logger.info(s"Unauthorized: user[${tokenReference.user.id}] is NOT a member of organization[$org]")
                        Unauthorized(Json.toJson(Seq(
                          Error("authorization_failed", s"This API key is either not authorized to access $org or the organization does not exist")
                        )))
                      }

                      case Some(membership) => {
                        Logger.info(s"Authorized: user[${tokenReference.user.id}] is a ${membership.role} of organization[$org]")
                        proxy(request, internalRoute.service)
                      }
                    }

                  }.recover {
                    case io.flow.organization.v0.errors.UnitResponse(401) => {
                      Unauthorized(Json.toJson(Seq(
                        Error("authorization_failed", s"This API key is either not authorized to access $org or the organization does not exist")
                      )))
                    }

                    case ex: Throwable => {
                      sys.error(s"Could not communicate with token service at[$tokenServiceUrl]: $ex")
                    }
                  }

                }.recover {
                  case io.flow.token.v0.errors.UnitResponse(404) => {
                    Unauthorized(Json.toJson(Seq(
                      Error("authorization_failed", s"The API key is not valid")
                    )))
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

      case None => Future {
        Logger.info(s"Unrecognized path[${request.path}] - returning 404")
        NotFound
      }
    }
  }

  private[this] def proxy(request: Request[RawBuffer], service: Service) = {
    Logger.info(s"Proxying ${request.method} ${request.path} to service[${service.name}] ${service.host}${request.path}")

    // Create the request to the upstream server:
    val proxyRequest = wsClient.url(service.host + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withVirtualHost(VirtualHostName)
      .withHeaders(proxyHeaders(request.headers, service).headers: _*)
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

  def proxyHeaders(headers: Headers, service: Service): Headers = {
    (
      headers.get("Content-Type") match {
        case None => headers.add("Content-Type" -> DefaultContentType)
        case Some(_) => headers
      }
    ).add("X-Flow-Proxy-Service" -> service.name)
  }

  def parseToken(value: Option[String]): Option[String] = {
    value match {
      case None => None
      case Some(v) => {
        v.split(" ").toList match {
          case "Basic" :: value :: Nil => {
            new String(Base64.decodeBase64(value.getBytes)).split(":").toList match {
              case Nil => None
              case token :: rest => Some(token)
            }
          }
          case _ => None
        }
      }
    }
  }

}
