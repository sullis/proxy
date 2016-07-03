package controllers

import lib.Services
import javax.inject.Inject
import play.api.{Configuration, Logger}
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
        Logger.info(s"RequestHandler starting with ${services.all.size} services: " + services.all.map(_.name).sorted.mkString(", "))
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
