package lib

case class ResolvedToken(
  userId: String,
  organizationId: Option[String] = None,
  partnerId: Option[String] = None
)
