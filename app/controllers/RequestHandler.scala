package controllers

import javax.inject.Inject
import play.api.http.HttpRequestHandler
import play.api.mvc._

class RequestHandler @Inject() (
  internal: Internal,
  proxy: ReverseProxy
) extends HttpRequestHandler {

  def handlerForRequest(request: RequestHeader) = {
    (request.method, request.path) match {
      case ("GET", "/_internal_/healthcheck") => (request, internal.getHealthcheck)
      case ("GET", "/_internal_/config") => (request, internal.getConfig)
      case _ => (request, proxy.handle)
    }
  }

}
