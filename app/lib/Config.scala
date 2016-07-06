package lib

import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class Config @Inject() (
  configuration: Configuration
) {

  lazy val jwtSalt = requiredString("jwt.salt")

  def requiredString(name: String): String = {
    configuration.getString(name).getOrElse {
      sys.error(s"Missing configuration parameter[$name]")
    }
  }

}
