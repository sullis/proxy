package lib

import play.api.libs.json._

object LoggingUtil {

  // All fields in this array will have their values redacted
  private[this] val GlobalFieldsToReplace = Seq("cvv", "number")

  // Map from apidoc model type to list of fields to whitelist
  private[this] val WhiteListByType = Map(
    "item_form" -> Seq("number"),
    "harmonized_item_form" -> Seq("number"),
    "order_form" -> Seq("number")
  )

  /**
    * Accepts a JsValue, redacting any fields that may contain sensitive data
    * @param body The JsValue itself
    * @param typ The type represented by the JsValue if resolved from the apidoc specification
    */
  def safeJson(
    body: JsValue,
    typ: Option[String] = None
  ): JsValue = {
    val allFieldsToReplace = typ.flatMap(WhiteListByType.get) match {
      case None => GlobalFieldsToReplace
      case Some(whitelist) => GlobalFieldsToReplace.filter { name => !whitelist.contains(name) }
    }

    body match {
      case o: JsObject => {
        JsObject(
          o.value.map { case (k, v) =>
            if (allFieldsToReplace.contains(k.toLowerCase.trim)) {
              k -> JsString("x" * stringLength(v))
            } else {
              k -> v
            }
          }
        )
      }
      case a: JsArray => {
        JsArray(
          a.value.map { v => safeJson(v) }
        )
      }
      case _ => body
    }
  }

  private[this] def stringLength(js: JsValue): Int = {
    js match {
      case JsNull => 0
      case v: JsString => v.value.length
      case v: JsNumber => v.value.toString().length
      case _ => js.toString().length
    }
  }
}
