package lib

import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._

class ServiceSpec extends PlaySpec with OneServerPerSuite {

  "resolves route" in {
    val services = Seq(
      Service(
        "organization",
        "https://organization.api.flow.io",
        routes = Seq(
          Route("GET", "/organizations"),
          Route("POST", "/organizations"),
          Route("GET", "/organizations/:id"),
          Route("PUT", "/organizations/:id")
        )
      ),
      Service(
        "user",
        "https://user.api.flow.io",
        routes = Seq(
          Route("GET", "/users"),
          Route("POST", "/users"),
          Route("GET", "/users/:id"),
          Route("PUT", "/users/:id")
        )
      )
    )

    val s = Index(
      ProxyConfig(
        version = "0.0.1",
        services = services
      )
    )

    // Undefined
    s.resolve("GET", "") must be(None)
    s.resolve("GET", "/") must be(None)
    s.resolve("GET", "/tmp") must be(None)

    // static
    s.resolve("GET", "/organizations").map(_.service.name) must be(Some("organization"))
    s.resolve("POST", "/organizations").map(_.service.name) must be(Some("organization"))
    s.resolve("GET", "/users").map(_.service.name) must be(Some("user"))
    s.resolve("POST", "/users").map(_.service.name) must be(Some("user"))

    // dynamic
    s.resolve("GET", "/organizations/flow").map(_.service.name) must be(Some("organization"))
    s.resolve("PUT", "/organizations/flow").map(_.service.name) must be(Some("organization"))
    s.resolve("GET", "/users/usr-201606-128367123").map(_.service.name) must be(Some("user"))
    s.resolve("PUT", "/users/usr-201606-128367123").map(_.service.name) must be(Some("user"))
  }

  "organization" in {
    val service = Service(
      "organization",
      "https://organization.api.flow.io",
      routes = Seq(
        Route("GET", "/organizations"),
        Route("GET", "/:organization/memberships")
      )
    )

    InternalRoute.Static("GET", "/foo", service).organization("/foo") must be(None)
    InternalRoute.Static("GET", "/users", service).organization("/foo") must be(None)
    InternalRoute.Static("GET", "/organization", service).organization("/foo") must be(None)
    InternalRoute.Static("GET", "/organization/catalog", service).organization("/foo") must be(None)
    InternalRoute.Static("GET", "/:organization", service).organization("/flow") must be(Some("flow"))
    InternalRoute.Static("GET", "/:organization/catalog", service).organization("/flow/catalog") must be(Some("flow"))

    InternalRoute.Dynamic("GET", "/foo/:id", service).organization("/foo") must be(None)
    InternalRoute.Dynamic("GET", "/users/:id", service).organization("/foo") must be(None)
    InternalRoute.Dynamic("GET", "/organization/:id", service).organization("/foo") must be(None)
    InternalRoute.Dynamic("GET", "/organization/catalog/:id", service).organization("/foo") must be(None)
    InternalRoute.Dynamic("GET", "/:organization/:id", service).organization("/flow/5") must be(Some("flow"))
    InternalRoute.Dynamic("GET", "/:organization/catalog/:id", service).organization("/flow/catalog/5") must be(Some("flow"))
  }

}
