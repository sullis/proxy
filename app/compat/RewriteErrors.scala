package compat

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

sealed trait RewriteHandler {
  def rewrite(): JsValue
}

object RewriteHandler {

  case class Passthrough(
    catalog: TranslationCatalog,
    incoming: JsObject
  ) extends RewriteHandler {
    override def rewrite(): JsValue = incoming
  }

  case class SingleErrorsArray(
    catalog: TranslationCatalog,
    incoming: JsValue,
    errors: Seq[JsObject]
  ) extends RewriteHandler {
    override def rewrite(): JsValue = {
      val codes = errors.flatMap { js => (js \ "code").asOpt[String] }
      codes.toList match {
        case Nil => {
          incoming
        }

        case firstCode :: _ => {
          Json.obj(
            "code" -> firstCode,
            "messages" -> codes.map { c =>
              catalog.lookup(c).getOrElse {
                Logger.warn(s"FlowProxyMissingTranslation Unhandled translation for error code[$c]")
                c
              }
            }
          )
        }
      }
    }
  }
}

@Singleton
class RewriteErrors @Inject() () {

  def rewrite(
    catalog: TranslationCatalog,
    incoming: JsValue
  ): JsValue = {
    incoming.asOpt[JsObject] match {
      case None => incoming
      case Some(o) => selectHandler(catalog, o).rewrite()
    }
  }

  def selectHandler(
    catalog: TranslationCatalog,
    incoming: JsObject
  ): RewriteHandler = {
    incoming.value.keys.toSeq.sorted.toList match {
      case Seq("code", "messages") => {
        RewriteHandler.Passthrough(catalog, incoming)
      }

      case Seq("errors") => {
        val values = (incoming \ "errors").asOpt[JsArray].map(_.value).getOrElse(Nil)
        val objects = values.flatMap(_.asOpt[JsObject])
        if (values.length == objects.length) {
          RewriteHandler.SingleErrorsArray(
            catalog = catalog,
            incoming = incoming,
            errors = objects
          )
        } else {
          Logger.warn(s"errors list contained elements that were NOT objects. Using passthrough handler: $values")
          RewriteHandler.Passthrough(
            catalog = catalog,
            incoming
          )
        }
      }

      case other => {
        Logger.warn(s"Could not identify explicit handler for object with keys[${other.mkString(", ")}] - Using passthrough")
        RewriteHandler.Passthrough(
          catalog = catalog,
          incoming
        )
      }
    }

  }
}