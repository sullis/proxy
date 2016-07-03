package controllers

import lib.Services
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.http.{HttpEntity, HttpRequestHandler}
import play.api.libs.ws.{WSClient, StreamedResponse}
import play.api.mvc._
import scala.concurrent.Future

class RequestHandler @Inject() (
  configuration: Configuration,
  wsClient: WSClient,
  healthchecks: Healthchecks
) extends HttpRequestHandler with Handler {

  private[this] val Uri = configuration.getString("proxy.config.uri").getOrElse {
    sys.error("Missing configuration parameter[proxy.config.uri]")
  }

  private[this] val proxy = {
    Services.load(Uri) match {
      case Left(errors) => {
        sys.error(s"Failed to load configuration from URI[$Uri]: $errors")
      }

      case Right(services) => {
        ReverseProxy(wsClient, services)
      }
    }
  }

  def handlerForRequest(request: RequestHeader) = {
    (request.path) match {
      case ("/_internal_/healthcheck") => (request, healthchecks.getHealthcheck)
      case _ => (request, proxy.reverseProxy)
    }
  }

}
