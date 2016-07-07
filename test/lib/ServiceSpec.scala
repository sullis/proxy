package lib

import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._

class ServiceSpec extends PlaySpec with OneServerPerSuite {

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
