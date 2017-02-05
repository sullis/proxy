package lib

import play.api.libs.json._

object LoggingUtil {

  // All fields in this array will have their values redacted
  private[this] val GlobalFieldsToReplace = Set("cvv", "number", "token", "email", "password")

  // Map from apidoc model type to list of fields to whitelist
  private[this] val WhiteListByType = Map(
    "item_form" -> Set("number"),
    "harmonized_item_form" -> Set("number"),
    "order_form" -> Set("number"),
    "order_put_form" -> Set("number")
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
              // TODO: This assumes that the type is a string for all replacements we make. Safe
              // for now but would be better to NOT assume
              k -> JsString("x" * stringLength(v))
            } else {
              k -> safeJson(v)
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
