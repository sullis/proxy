package controllers

import javax.inject.{Inject, Singleton}
import play.api.http.{HttpEntity, HttpRequestHandler}
import play.api.libs.ws.{WSClient, StreamedResponse}
import play.api.mvc._
import scala.concurrent.Future

class RequestHandler @Inject() (
  wsClient: WSClient,
  proxy: ReverseProxy
) extends HttpRequestHandler with Handler {

  def handlerForRequest(request: RequestHeader) = {
    (request.path) match {
      case ("/_internal_/healthcheck") => (request, proxy.reverseProxy)
      case _ => {
        println(s"No route for ${request.path}")
        (request, Action(Results.NotFound))
      }
    }
  }

}
