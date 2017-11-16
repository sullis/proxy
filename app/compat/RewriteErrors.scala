package compat

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

sealed trait RewriteHandler {
  def rewrite(incoming: JsObject): JsObject
}

object RewriteHandler {

  case object PassthroughHandler extends RewriteHandler {
    override def rewrite(incoming: JsObject): JsObject = {
      incoming
    }
  }

  case class TranslationHandler(
    catalog: TranslationCatalog
  ) extends RewriteHandler {

    override def rewrite(incoming: JsObject): JsObject = {
      incoming
    }

  }

  case object RewriteErrors extends RewriteHandler {
    override def rewrite(incoming: JsObject): JsObject = {
      Json.toJson(
        incoming.value.map { case (key, v) =>
          key -> (
            key match {
              case "errors" => {
                v match {
                  case a: JsArray => rewriteErrors(a.value)
                  case _ => v
                }
              }
              case _ => v
            }
          )
        }
      ).asInstanceOf[JsObject]
    }

    private[this] def rewriteErrors(errors: Seq[JsValue]): JsValue = {
      val codes = errors.flatMap { js => (js \ "code").asOpt[String] }
      codes.toList match {
        case Nil => JsArray(errors)
        case firstCode :: _ => {
          Json.obj(
            "code" -> firstCode,
            "messages" -> JsArray(Nil)
          )
        }
      }
    }
  }
}

@Singleton
class RewriteErrors @Inject() (translations: Translations) {

  def rewrite(
    locale: String,
    incoming: JsValue
  ): JsValue = {
    incoming.asOpt[JsObject] match {
      case None => incoming
      case Some(o) => {
        val handlers = Seq(
          RewriteHandler.TranslationHandler(translations.locale(locale)),
          RewriteHandler.RewriteErrors
        )

        handlers.foldLeft(o) { case (js, handler) =>
          handler.rewrite(js)
        }
      }
    }
  }

}