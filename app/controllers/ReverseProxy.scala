package controllers

import javax.inject.{Inject, Singleton}
import lib.{Service, Services}
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.ws.{WSClient, StreamedResponse}
import play.api.mvc._
import scala.concurrent.Future

case class ReverseProxy(
  wsClient: WSClient,
  services: Services
) extends Controller {

  private[this] val VirtualHostName = "api.flow.io"

  // WS Client defaults to application/octet-stream. Given this proxy
  // is for APIs only, assume JSON if no content type header is
  // provided.
  private[this] val DefaultContentType = "application/json"

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def reverseProxy = Action.async(parse.raw) { request: Request[RawBuffer] =>
    services.findByPath(request.path) match {
      case Some(service) => {
        proxy(request, service)
      }

      case None => Future {
        Logger.info(s"Unrecognized path[${request.path}] - returning 404")
        NotFound
      }
    }
  }

  def proxy(request: Request[RawBuffer], service: Service) = {
    Logger.info(s"Proxying ${request.method} ${request.path} to service[${service.name}] ${service.host}${request.path}")

    // Create the request to the upstream server:
    val proxyRequest = wsClient.url(service.host + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withVirtualHost(VirtualHostName)
      .withHeaders(proxyHeaders(request.headers, service).headers: _*)
      .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
      .withBody(request.body.asBytes().get)

    proxyRequest.stream.map {
      case StreamedResponse(response, body) => {
        // Get the content type
        val contentType: Option[String] = response.headers.get("Content-Type").flatMap(_.headOption)

        // If there's a content length, send that, otherwise return the body chunked
        response.headers.get("Content-Length") match {
          case Some(Seq(length)) => {
            Ok.sendEntity(HttpEntity.Streamed(body, Some(length.toLong), contentType))
          }

          case _ => {
            contentType match {
              case None => Ok.chunked(body)
              case Some(ct) => Ok.chunked(body).as(ct)
            }
          }
        }
      }
    }
  }

  def proxyHeaders(headers: Headers, service: Service): Headers = {
    (
      headers.get("Content-Type") match {
        case None => headers.add("Content-Type" -> DefaultContentType)
        case Some(_) => headers
      }
    ).add("X-Flow-Proxy-Service" -> service.name)
  }
}
