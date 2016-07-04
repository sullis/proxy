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

    val s = Services(services)

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

  "route" in {
    val service = Service(
      "organization",
      "https://organization.api.flow.io",
      routes = Seq(
        Route("GET", "/organizations"),
        Route("GET", "/:organization/memberships")
      )
    )

    InternalRoute.Static("GET", "/foo", service).hasOrganization must be(false)
    InternalRoute.Static("GET", "/users", service).hasOrganization must be(false)
    InternalRoute.Static("GET", "/organization", service).hasOrganization must be(false)
    InternalRoute.Static("GET", "/organization/catalog", service).hasOrganization must be(false)
    InternalRoute.Static("GET", "/:organization", service).hasOrganization must be(true)
    InternalRoute.Static("GET", "/:organization/catalog", service).hasOrganization must be(true)
  }

}
