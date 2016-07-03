package controllers

import javax.inject.{Inject, Singleton}
import play.api.http.{HttpEntity, HttpRequestHandler}
import play.api.libs.ws.{WSClient, StreamedResponse}
import play.api.mvc._
import scala.concurrent.Future

class RequestHandler @Inject() (
  wsClient: WSClient,
  healthchecks: Healthchecks
) extends HttpRequestHandler with Handler {

  private[this] val services = Services(
    Seq(
      Service(
        name = "token",
        host = "http://localhost:6151",
        routes = Seq(Route("POST", "/token-validations"))
      )
    )
  )

  private[this] val proxy = ReverseProxy(wsClient, services)

  def handlerForRequest(request: RequestHeader) = {
    (request.path) match {
      case ("/_internal_/healthcheck") => (request, healthchecks.getHealthcheck)
      case _ => (request, proxy.reverseProxy)
    }
  }

}
