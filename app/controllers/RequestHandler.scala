package controllers

import javax.inject.{Inject, Singleton}
import play.api.http.{HttpEntity, HttpRequestHandler}
import play.api.libs.ws.{WSClient, StreamedResponse}
import play.api.mvc._
import scala.concurrent.Future

class RequestHandler @Inject() (
  wsClient: WSClient,
  proxy: ReverseProxy,
  healthchecks: Healthchecks
) extends HttpRequestHandler with Handler {

  def handlerForRequest(request: RequestHeader) = {
    (request.path) match {
      case ("/_internal_/healthcheck") => (request, healthchecks.getHealthcheck)
      case _ => (request, proxy.reverseProxy)
    }
  }

}
