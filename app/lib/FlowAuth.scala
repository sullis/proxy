package lib

import authentikat.jwt.{JwtClaimsSet, JwtHeader, JsonWebToken}
import javax.inject.{Inject, Singleton}

/**
  * Defines the data that goes into the flow auth set by the proxy server.
  */
@Singleton
class FlowAuth @Inject () (
  config: Config
) {

  /**
    * Returns the string jwt token of the specified auth data.
    */
  def jwt(
    userId: String,
    organization: Option[String],
    role: Option[String]
  ): String = {
    val header = JwtHeader("HS256")
    val claimsSet = JwtClaimsSet(
      Map(
        "user_id" -> Some(userId),
        "organization" -> organization,
        "role" -> role
      ).flatMap { case (key, value) => value.map { v => (key -> v)} }
    )
    JsonWebToken(header, claimsSet, config.jwtSalt)
  }
  
}
