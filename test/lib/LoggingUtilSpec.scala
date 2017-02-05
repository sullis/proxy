package lib

import org.scalatestplus.play._
import play.api.libs.json._

class LoggingUtilSpec extends PlaySpec with OneServerPerSuite {

  "safeJson when type is not known" in {
    LoggingUtil.safeJson(JsNull) must equal(JsNull)
    LoggingUtil.safeJson(JsString("a")) must equal(JsString("a"))

    LoggingUtil.safeJson(Json.obj("foo" -> "bar")) must equal(Json.obj("foo" -> "bar"))

    LoggingUtil.safeJson(Json.obj("foo" -> "bar", "cvv" -> "123", "number" -> "1234567890")) must equal(
      Json.obj("foo" -> "bar", "cvv" -> "xxx", "number" -> "xxxxxxxxxx")
    )

    LoggingUtil.safeJson(
      JsArray(
        Seq(
          Json.obj("foo" -> "bar"),
          Json.obj("cvv" -> "123"),
          Json.obj("number" -> "1234567890")
        )
      )
    )must equal(
      JsArray(
        Seq(
          Json.obj("foo" -> "bar"),
          Json.obj("cvv" -> "xxx"),
          Json.obj("number" -> "xxxxxxxxxx")
        )
      )

    )
  }

  "safeJson with type whitelist" in {
    LoggingUtil.safeJson(Json.obj("number" -> "1234567890"), typ = Some("order_form")) must equal(
      Json.obj("number" -> "1234567890")
    )
  }
}
