package compat

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

sealed trait RewriteHandler {
  def rewrite(): JsValue
}

object RewriteHandler {

  case class Passthrough(incoming: JsObject) extends RewriteHandler {
    override def rewrite(): JsValue = incoming
  }

  case class SingleErrorsArray(incoming: JsValue, errors: Seq[JsObject]) extends RewriteHandler {
    override def rewrite(): JsValue = {
      val codes = errors.flatMap { js => (js \ "code").asOpt[String] }
      codes.toList match {
        case Nil => {
          incoming
        }

        case firstCode :: _ => {
          Json.obj(
            "code" -> firstCode,
            "messages" -> codes.map(translateErrorCode)
          )
        }
      }
    }
  }

  private[this] def translateErrorCode(code: String): String = {
    code match {
      case "invalid_cvn" => "CVN is not valid"
      case other => {
        Logger.warn(s"FlowProxyMissingTranslation Unhandled translation for error code[$code]")
        other
      }
    }
  }
}

@Singleton
class RewriteErrors @Inject() () {

  def rewrite(incoming: JsValue): JsValue = {
    incoming.asOpt[JsObject] match {
      case None => incoming
      case Some(o) => selectHandler(o).rewrite()
    }
  }

  def selectHandler(incoming: JsObject): RewriteHandler = {
    incoming.value.keys.toSeq.sorted.toList match {
      case Seq("code", "messages") => {
        RewriteHandler.Passthrough(incoming)
      }
      case Seq("errors") => {
        val values = (incoming \ "errors").asOpt[JsArray].map(_.value).getOrElse(Nil)
        val objects = values.flatMap(_.asOpt[JsObject])
        if (values.length == objects.length) {
          RewriteHandler.SingleErrorsArray(
            incoming = incoming,
            errors = objects
          )
        } else {
          Logger.warn(s"errors list contained elements that were NOT objects. Using passthrough handler: $values")
          RewriteHandler.Passthrough(incoming)
        }
      }

      case other => {
        Logger.warn(s"Could not identify explicit handler for object with keys[${other.mkString(", ")}] - Using passthrough")
        RewriteHandler.Passthrough(incoming)
      }
    }

  }
}