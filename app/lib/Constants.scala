package lib

object Constants {

  val StopWords: Set[String] = Set("undefined", "null")

  private[this] val DoNotLogSanitizedBodyURls = Set(
    "/:organization/catalog/items/:number",
    "/:organization/inventory_updates",
    "/shopify/webhook/events/shops/:shopify_shop_id/:topic/:action"
  )

  def logSanitizedBody(canonicalUrl: String): Boolean = {
    !DoNotLogSanitizedBodyURls.contains(canonicalUrl)
  }

  object Headers {

    val FlowAuth = "X-Flow-Auth"
    val FlowRequestId = "X-Flow-Request-Id"
    val FlowServer = "X-Flow-Server"
    val FlowHost = "X-Flow-Host"
    val FlowIp = "X-Flow-Ip"

    val ContentType = "Content-Type"
    val ContentLength = "Content-Length"
    val Host = "Host"
    val ForwardedHost = "X-Forwarded-Host"
    val ForwardedFor = "X-Forwarded-For"
    val Origin = "Origin"
    val ForwardedOrigin = "X-Forwarded-Origin"
    val ForwardedMethod = "X-Forwarded-Method"

    val CfRay = "CF-RAY"
    val CfConnectingIp = "CF-Connecting-IP"
    val CfTrueClientIp = "True-Client-IP"
    val CfIpCountry = "CF-IPCountry"
    val CfVisitor = "CF-Visitor"

    val namesToRemove: Set[String] = Set(
      ContentType,
      ContentLength,
      FlowAuth,
      FlowRequestId,
      FlowServer,
      FlowHost,
      Host,
      Origin,
      ForwardedOrigin,
      ForwardedMethod,
      // Remove any cloudflare headers when proxying to underlying service
      CfRay,
      CfConnectingIp,
      CfTrueClientIp,
      CfIpCountry,
      CfVisitor
    )

    val namesToWhitelist: Set[String] = Set(
      // cloudflare headers
      CfRay,
      CfConnectingIp,
      CfTrueClientIp,
      CfIpCountry,
      CfVisitor,

      // safe flow headers
      FlowRequestId,
      FlowServer,
      FlowHost,

      // standard stuff
      Host,
      Origin,
      ForwardedOrigin,
      ForwardedMethod
    )
  }

  /**
    * For some features (like specifying explicitly to which server
    * to route the request), we verify that the requesting user is a
    * member of this organization.
    */
  val FlowOrganizationId = "flow"

}
