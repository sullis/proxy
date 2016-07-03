package controllers

import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import db.HealthchecksDao
import io.flow.payment.api.actors.{EncryptionActor, PassphraseActor}
import io.flow.payment.api.gateways.Spreedly
import io.flow.payment.api.util.EnvironmentVariables
import io.flow.play.util.Validation
import io.flow.common.v0.models.Healthcheck
import io.flow.common.v0.models.json._
import scala.concurrent.duration._

import play.api._
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class Healthchecks @javax.inject.Inject() (
  environmentVariables: EnvironmentVariables,
  healthchecksDao: HealthchecksDao,
  spreedly: Spreedly,
  @javax.inject.Named("encryption-actor") encryptionActor: akka.actor.ActorRef,
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef,
  @javax.inject.Named("passphrase-actor") passphraseActor: akka.actor.ActorRef
) extends Controller {

  implicit val timeout = Timeout(1.second)
  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] val HealthyJson = Json.toJson(Healthcheck(status = "healthy"))

  def getHealthcheck() = Action.async { request =>
    for {
      passphraseResponse <- passphraseActor ? PassphraseActor.Requests.Healthcheck
      encryptionResponse <- encryptionActor ? EncryptionActor.Requests.Healthcheck
    } yield {
      val checks = Map(
        "db" -> healthchecksDao.validate(),
        "passphrases" -> passphraseResponse.asInstanceOf[PassphraseActor.Responses.Healthcheck].errors,
        "encryption" -> encryptionResponse.asInstanceOf[EncryptionActor.Responses.Healthcheck].errors
      )

      val allErrors: Iterable[String] = checks.flatMap { case (name, errors) => errors }
      allErrors.toList match {
        case Nil => Ok(HealthyJson)
        case errors => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
      }
    }
  }

}
