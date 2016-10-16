package lib

import io.flow.common.v0.models.Environment
import io.flow.token.v0.models._
import play.api.Logger

case class ResolvedToken(
  userId: String,
  environment: Option[Environment] = None,
  organizationId: Option[String] = None,
  partnerId: Option[String] = None
)

object ResolvedToken {

  def fromUser(userId: String): ResolvedToken = {
    ResolvedToken(userId = userId)
  }

  def fromToken(token: TokenReference): Option[ResolvedToken] = {
    token match {
      case t: LegacyTokenReference => Some(
        ResolvedToken.fromUser(t.user.id)
      )

      case t: OrganizationTokenReference => Some(
        ResolvedToken(
          userId = t.user.id,
          environment = Some(t.environment),
          organizationId = Some(t.organization.id)
        )
      )

      case t: PartnerTokenReference => Some(
        ResolvedToken(
          userId = t.user.id,
          environment = Some(t.environment),
          partnerId = Some(t.partner.id)
        )
      )

      case TokenReferenceUndefinedType(other) => {
        Logger.warn(s"[proxy] TokenReferenceUndefinedType($other) - proceeding as unauthenticated")
        None
      }
    }
  }

}
