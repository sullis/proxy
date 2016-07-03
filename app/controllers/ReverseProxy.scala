package controllers

import javax.inject.{Inject, Singleton}
import play.api.http.HttpEntity
import play.api.libs.ws.{WSClient, StreamedResponse}
import play.api.mvc._
import scala.concurrent.Future

@Singleton
class ReverseProxy @Inject() (
  wsClient: WSClient
) extends Controller {

  private[this] val VirtualHostName = "api.flow.io"

  // WS Client defaults to application/octet-stream. Given this proxy
  // is for APIs only, assume JSON if no content type header is
  // provided.
  private[this] val DefaultContentType = "application/json"

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def reverseProxy = Action.async(parse.raw) { request: Request[RawBuffer] =>
    request.path match {
      case "/token-validations" => proxy(request, Services.Token)
      case other => Future {
        println(s"reverseProxy Unrecognized path[${request.path}] - returning 404")
        NotFound
      }
    }
  }

  def proxy(request: Request[RawBuffer], service: Service) = {
    println(s"reverseProxy ${service.name} ${request.method} ${service.host}${request.path}")

    // Create the request to the upstream server:
    val proxyRequest = wsClient.url(service.host + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withVirtualHost(VirtualHostName)
      .withHeaders(proxyHeaders(request.headers).headers: _*)
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

  def proxyHeaders(headers: Headers): Headers = {
    headers.get("Content-Type") match {
      case None => headers.add("Content-Type" -> DefaultContentType)
      case Some(_) => headers
    }
  }
}
