package controllers

import javax.inject.{Inject, Singleton}
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
    reverseProxy.index.config.services.toList match {
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
      Json.obj(
        "version" -> reverseProxy.index.config.version,
        "services" -> reverseProxy.index.config.services.map { service =>
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
