package controllers

import authentikat.jwt.{JwtClaimsSet, JwtHeader, JsonWebToken}
import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import com.google.inject.assistedinject.{Assisted, FactoryModuleBuilder}
import concurrent.ExecutionContext
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject
import play.api.Logger
import play.api.http.Status
import play.api.inject.Module
import play.api.libs.ws.{StreamedResponse, WSClient, WSRequest}
import play.api.mvc._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import play.api.http.HttpEntity
import lib.{Config, Service}

/**
  * Service Proxy is responsible for proxying all requests to a given
  * service. The primary purpose of the proxy is to segment our thread
  * pools by service - so if one service is having difficulty, it is
  * less likely to impact other services.
  */
trait ServiceProxy {

  def proxy(
    request: Request[RawBuffer],
    userId: Option[String],
    organization: Option[String],
    role: Option[String]
  ): Future[play.api.mvc.Result]

}

object ServiceProxy {
  trait Factory {
    def apply(service: Service): ServiceProxy
  }
}

class ServiceProxyModule extends AbstractModule {
  def configure {
    install(new FactoryModuleBuilder()
      .implement(classOf[ServiceProxy], classOf[ServiceProxyImpl])
      .build(classOf[ServiceProxy.Factory])
    )
  }
}

class ServiceProxyImpl @Inject () (
  system: ActorSystem,
  ws: WSClient,
  config: Config,
  @Assisted service: Service
) extends ServiceProxy with Controller{

  private[this] val DefaultContextName = s"default-service-context"
  private[this] lazy val jwtSalt = config.requiredString("jwt.salt")

  implicit val ec = {
    val name = s"${service.name}-context"
    Try {
      system.dispatchers.lookup(name)
    } match {
      case Success(ec) => {
        Logger.info(s"ServiceProxy[${service.name}] using configured execution context[${name}]")
        ec
      }

      case Failure(_) => {
        Logger.info(s"ServiceProxy[${service.name}] execution context[${name}] not found - using $DefaultContextName")
        system.dispatchers.lookup(DefaultContextName)
      }
    }
  }

  // WS Client defaults to application/octet-stream. Given this proxy
  // is for APIs only, assume JSON if no content type header is
  // provided.
  private[this] val DefaultContentType = "application/json"

  override final def proxy(
    request: Request[RawBuffer],
    userId: Option[String],
    organization: Option[String],
    role: Option[String]
  ) = {
    //val requestId = UUID.randomUUID.toString()
    //Logger.info(s"[${service.name}] ${request.method} ${service.host}${request.path} userId[${userId.getOrElse("none")}] organization[${organization.getOrElse("none")}] role[${role.getOrElse("none")}] requestId[$requestId]")

    val finalHeaders = proxyHeaders(
      request.headers,
      (
        userId.map { uid =>
          "X-Flow-Auth" -> jwtValue(uid, organization = organization, role = role)
        } ++ Seq("X-Flow-Proxy-Service" -> service.name)
      )
    )

    val req = ws.url(service.host + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withHeaders(finalHeaders.headers: _*)
      .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
      .withBody(request.body.asBytes().get)

    //val startMs = System.currentTimeMillis

    req.stream.map {
      case StreamedResponse(response, body) => {
        //val timeToFirstByteMs = System.currentTimeMillis - startMs
        val contentType: Option[String] = response.headers.get("Content-Type").flatMap(_.headOption)
        val contentLength: Option[Long] = response.headers.get("Content-Length").flatMap(_.headOption).flatMap(toLongSafe(_))

        //Logger.info(s"[${service.name}] ${request.method} ${request.path} ${response.status} ${timeToFirstByteMs}ms requestId[$requestId]")

        // If there's a content length, send that, otherwise return the body chunked
        contentLength match {
          case Some(length) => {
            Status(response.status).sendEntity(HttpEntity.Streamed(body, Some(length), contentType))
          }

          case None => {
            contentType match {
              case None => Status(response.status).chunked(body)
              case Some(ct) => Status(response.status).chunked(body).as(ct)
            }
          }
        }
      }
      case other => {
        sys.error("Unhandled response: " + other)
      }
    }
  }

  /**
    * Modifies headers by:
    *   - adding a default content-type
    *   - adding all additional headers specified
    */
  private[this] def proxyHeaders(headers: Headers, additional: Iterable[(String, String)]): Headers = {
    val all = (
      headers.get("Content-Type") match {
        case None => headers.add("Content-Type" -> DefaultContentType)
        case Some(_) => headers
      }
    )
    additional.foldLeft(all) { case (h, (k, v)) => h.remove(k).add(k -> v) }
  }


  private[this] def toLongSafe(value: String): Option[Long] = {
    Try {
      value.toLong
    } match {
      case Success(v) => Some(v)
      case Failure(_) => None
    }
  }

  private[this] def jwtValue(
    userId: String,
    organization: Option[String],
    role: Option[String]
  ): String = {
    val header = JwtHeader("HS256")
    val claimsSet = JwtClaimsSet(
      Map(
        "user_id" -> Some(userId),
        "organization" -> organization,
        "role" -> role
      ).flatMap { case (k, v) => v.map { o => (k -> o)} }
    )
    JsonWebToken(header, claimsSet, jwtSalt)
  }
  
}
