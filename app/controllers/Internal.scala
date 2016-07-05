package controllers

import javax.inject.{Inject, Singleton}
import lib.ServicesConfig
import play.api._
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Internal @Inject() (
  reverseProxy: ReverseProxy
) extends Controller {

  private[this] val HealthyJson = Json.obj(
    "status" -> "healthy"
  )

  def getHealthcheck() = Action { request =>
    reverseProxy.services.all.toList match {
      case Nil => {
        UnprocessableEntity(
          Json.toJson(
            Seq("No services are configured")
          )
        )
      }

      case _ => {
        Ok(HealthyJson)
      }
    }
  }

  def getConfig() = Action { request =>
    Ok(
      Json.toJson(
        reverseProxy.services.all.map { service =>
          Json.obj(
            "name" -> service.name,
            "host" -> service.host,
            "routes" -> service.routes.map { r =>
              Json.obj(
                "method" -> r.method,
                "path" -> r.path
              )
            }
          )
        }
      )
    )
  }

}
