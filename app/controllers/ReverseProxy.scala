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
  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  private[this] val DefaultContentType = "application/octet-stream"

  def reverseProxy = Action.async(parse.raw) { request: Request[RawBuffer] =>
    request.path match {
      case "/token-validations" => proxy(request, "token", "http://localhost:6151")
      case other => Future {
        println(s"reverseProxy Unrecognized path[${request.path}] - returning 404")
        NotFound
      }
    }
  }

  def proxy(request: Request[RawBuffer], appName: String, host: String) = {
    println(s"reverseProxy $appName ${request.method} $host${request.path}")

    // Create the request to the upstream server:
    val proxyRequest = wsClient.url(host + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withVirtualHost(VirtualHostName)
      //.withHeaders(flattenMultiMap(request.headers.toMap): _*)
      .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
      .withBody(request.body.asBytes().get)

    proxyRequest.stream.map {
      case StreamedResponse(response, body) => {
        // Get the content type
        val contentType: Option[String] = response.headers.get("Content-Type").flatMap(_.headOption)

        // If there's a content length, send that, otherwise return the body chunked
        response.headers.get("Content-Length") match {
          case Some(Seq(length)) =>
            Ok.sendEntity(HttpEntity.Streamed(body, Some(length.toLong), contentType))
          case _ =>
            Ok.chunked(body).as(contentType.getOrElse(DefaultContentType))
        }
      }
    }
  }


}
