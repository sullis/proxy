package controllers

import javax.inject.{Inject, Singleton}
import play.api.http.HttpEntity
import play.api.libs.ws.{WSClient, StreamedResponse}
import play.api.mvc._
import scala.concurrent.Future

@Singleton
class Application @Inject() (
  wsClient: WSClient
) extends Controller {

  private[this] val VirtualHostName = "api.flow.io"
  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def reverseProxy = Action.async(parse.raw) { request: Request[RawBuffer] =>
    // Create the request to the upstream server:
    val proxyRequest = wsClient.url("http://localhost:6151" + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withVirtualHost(VirtualHostName)
      //.withHeaders(flattenMultiMap(request.headers.toMap): _*)
      .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
      .withBody(request.body.asBytes().get)

    proxyRequest.stream.map {
      case StreamedResponse(response, body) => {
        // Get the content type
        val contentType = response.headers.get("Content-Type").flatMap(_.headOption)
          .getOrElse("application/octet-stream")

        // If there's a content length, send that, otherwise return the body chunked
        response.headers.get("Content-Length") match {
          case Some(Seq(length)) =>
            Ok.sendEntity(HttpEntity.Streamed(body, Some(length.toLong), Some(contentType)))
          case _ =>
            Ok.chunked(body).as(contentType)
        }
      }
    }
  }


}
/*
package controllers

import javax.inject.{Inject, Singleton}
import play.api._
import play.api.libs.ws.WS
import play.api.mvc._
//import play.api.Play.current

@Singleton
class Application @Inject() (
  application: Application
) {


  def reverseProxy = Action.async(parse) {
    request: Request[RawBuffer] =>
    // Create the request to the upstream server:
    val proxyRequest = WS.url("http://localhost:8887" + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withVirtualHost("localhost:9000")
      //.withHeaders(flattenMultiMap(request.headers.toMap): _*)
      .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
      .withBody(request.body.asBytes().get)

    // Stream the response to the client:
    proxyRequest.stream.map {
      case response => Result(
        ResponseHeader(headers.status, headers.headers.mapValues(_.head)),
        enum)
    }
  }

}
 */
