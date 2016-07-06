package controllers

import akka.actor.ActorSystem
import io.flow.common.v0.models.Error
import io.flow.common.v0.models.json._
import io.flow.token.v0.{Client => TokenClient}
import io.flow.organization.v0.{Client => OrganizationClient}
import io.flow.token.v0.models.TokenReference
import javax.inject.{Inject, Singleton}
import lib.{Authorization, AuthorizationParser, Config, Index, InternalRoute, Service, ProxyConfigFetcher}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import org.apache.commons.codec.binary.Base64
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class ReverseProxy @Inject () (
  system: ActorSystem,
  authorizationParser: AuthorizationParser,
  config: Config,
  proxyConfigFetcher: ProxyConfigFetcher,
  serviceProxyFactory: ServiceProxy.Factory
) extends Controller {

  val index: Index = proxyConfigFetcher.current()

  private[this] val organizationClient = {
    val svc = index.config.services.find(_.name == "organization").getOrElse {
      sys.error("There is no service named 'organization' in the current config: " + config)
    }
    Logger.info(s"Creating OrganizationClient w/ baseUrl[${svc.host}]")
    new OrganizationClient(baseUrl = svc.host)
  }

  private[this] val tokenServiceUrl = config.requiredString("service.token.uri")
  private[this] val tokenClient = new TokenClient(baseUrl = tokenServiceUrl)

  private[this] implicit val ec = system.dispatchers.lookup("reverse-proxy-context")

  private[this] val proxies: Map[String, ServiceProxy] = {
    Logger.info(s"ReverseProxy loading config version: ${index.config.version}")
    Map(
      index.config.services.map { s =>
        (s.name -> serviceProxyFactory(s))
      }: _*
    )
  }

  def handle = Action.async(parse.raw) { request: Request[RawBuffer] =>
    index.resolve(request.method, request.path) match {
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
        lookup(internalRoute.service).proxy(
          request,
          userId = userId,
          organization = None,
          role = None
        )
      }

      case Some(org) => {
        userId match {
          case None => Future {
            unauthorized("You must set a valid Authorization header")
          }

          case Some(uid) => {
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
                  lookup(internalRoute.service).proxy(
                    request,
                    userId = Some(uid),
                    organization = Some(org),
                    role = Some(membership.role.toString)
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

  private[this] def lookup(service: Service): ServiceProxy = {
    proxies.get(service.name).getOrElse {
      sys.error(s"Service[${service.name}] no proxy found")
    }
  }

  private[this] def unauthorized(message: String) = {
    Unauthorized(
      Json.toJson(
        Seq(
          Error("authorization_failed", message)
        )
      )
    )
  }
}
