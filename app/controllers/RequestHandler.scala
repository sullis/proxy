package controllers

import lib.ServicesConfig
import javax.inject.Inject
import play.api.http.{HttpEntity, HttpRequestHandler}
import play.api.libs.ws.{WSClient, StreamedResponse}
import play.api.mvc._
import scala.concurrent.Future

class RequestHandler @Inject() (
  wsClient: WSClient,
  internal: Internal,
  servicesConfig: ServicesConfig
) extends HttpRequestHandler {

  private[this] val proxy = ReverseProxy(wsClient, servicesConfig.current)

  def handlerForRequest(request: RequestHeader) = {
    (request.method, request.path) match {
      case ("GET", "/_internal_/healthcheck") => (request, internal.getHealthcheck)
      case ("GET", "/_internal_/config") => (request, internal.getConfig)
      case _ => (request, proxy.reverseProxy)
    }
  }

}
