package compat

import javax.inject.{Inject, Singleton}

import play.api.libs.json._

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
      Json.toJson(
        incoming.value.map { case (key, v) =>
          key -> (
            key match {
              case "errors" => {
                v match {
                  case a: JsArray => translateErrors(a.value)
                  case _ => v
                }
              }
              case _ => v
            }
            )
        }
      ).asInstanceOf[JsObject]
    }

    private[this] def translateErrors(errors: Seq[JsValue]): JsValue = {
      JsArray(
        errors.map {
          case o: JsObject => translateError(o)
          case j => j
        }
      )
    }

    private[this] def translateError(js: JsObject): JsObject = {
      (js \ "code").asOpt[String] match {
        case None => js
        case Some(errorCode) => {
          // Inject our translated message here
          Json.toJson(
            js.value ++ Map(
              "message" -> JsString(
                catalog.translate(errorCode).getOrElse {
                  (js \ "message").asOpt[String].getOrElse(errorCode)
                }
              )
            )
          ).asInstanceOf[JsObject]
        }
      }
    }

  }

  case object RewriteErrors extends RewriteHandler {
    override def rewrite(incoming: JsObject): JsObject = {
      incoming.value.keys.toList match {
        case "errors" :: Nil => {
          incoming.value("errors") match {
            case a: JsArray => rewriteErrors(a.value)
            case _ => incoming

          }
        }
        case _ => incoming
      }
    }

    private[this] def rewriteErrors(errors: Seq[JsValue]): JsObject = {
      val codes: Seq[String] = errors.flatMap { js => (js \ "code").asOpt[String] }

      Json.obj(
        "code" -> JsString(codes.headOption.getOrElse("generic_error")),
        "messages" -> errors.flatMap { js => (js \ "message").asOpt[String] }
      )
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