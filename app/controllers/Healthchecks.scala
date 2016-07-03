package controllers

import javax.inject.{Inject, Singleton}
import play.api._
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Healthchecks @Inject() () extends Controller {

  private[this] val HealthyJson = Json.obj(
    "status" -> "healthy"
  )

  def getHealthcheck() = Action { request =>
    Ok(HealthyJson)
  }

}
