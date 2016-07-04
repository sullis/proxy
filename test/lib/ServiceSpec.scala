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
    s.findByMethodAndPath("GET", "") must be(None)
    s.findByMethodAndPath("GET", "/") must be(None)
    s.findByMethodAndPath("GET", "/tmp") must be(None)

    // static
    s.findByMethodAndPath("GET", "/organizations").map(_.name) must be(Some("organization"))
    s.findByMethodAndPath("POST", "/organizations").map(_.name) must be(Some("organization"))
    s.findByMethodAndPath("GET", "/users").map(_.name) must be(Some("user"))
    s.findByMethodAndPath("POST", "/users").map(_.name) must be(Some("user"))

    // dynamic
    s.findByMethodAndPath("GET", "/organizations/flow").map(_.name) must be(Some("organization"))
    s.findByMethodAndPath("PUT", "/organizations/flow").map(_.name) must be(Some("organization"))
    s.findByMethodAndPath("GET", "/users/usr-201606-128367123").map(_.name) must be(Some("user"))
    s.findByMethodAndPath("PUT", "/users/usr-201606-128367123").map(_.name) must be(Some("user"))
  }

  "route" in {
    Route("GET", "/foo").hasOrganization must be(false)
    Route("GET", "/users").hasOrganization must be(false)
    Route("GET", "/organization").hasOrganization must be(false)
    Route("GET", "/organization/catalog").hasOrganization must be(false)
    Route("GET", "/:organization").hasOrganization must be(true)
    Route("GET", "/:organization/catalog").hasOrganization must be(true)
  }

}
