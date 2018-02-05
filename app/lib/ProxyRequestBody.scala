package lib

import java.nio.charset.Charset

import akka.util.ByteString
import play.api.libs.json.JsValue

sealed trait ProxyRequestBody extends Product with Serializable

object ProxyRequestBody {

  val Utf8: Charset = Charset.forName("UTF-8")

  case class Bytes(bytes: ByteString) extends ProxyRequestBody

  case class Json(js: JsValue) extends ProxyRequestBody

  case class File(file: java.io.File) extends ProxyRequestBody

}
