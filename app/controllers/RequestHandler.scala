package controllers

import javax.inject.Inject
import lib.Constants
import play.api.http.HttpRequestHandler
import play.api.mvc._

class RequestHandler @Inject() (
  internal: Internal,
  proxy: ReverseProxy
) extends HttpRequestHandler {

  def handlerForRequest(request: RequestHeader) = {
    (request.method, request.path, request.headers.get(Constants.Headers.FlowService)) match {
      case ("GET", "/_internal_/healthcheck", None) => (request, internal.getHealthcheck)
      case ("GET", "/_internal_/config", None) => (request, internal.getConfig)
      case _ => (request, proxy.handle)
    }
  }

}
