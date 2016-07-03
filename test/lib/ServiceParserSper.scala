package lib

import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._
//import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

class ServiceParserSpec extends PlaySpec with OneServerPerSuite { // with ScalaFutures with IntegrationPatience {

  "empty" in {
    ServiceParser.parse("   ") must be(Left(Seq("Nothing to parse")))
  }

  "single service w/ no operations" in {
    val spec = """
test:
  host: https://test.api.flow.io
"""
    ServiceParser.parse(spec) must be(
      Right(
        Seq(
          Service("test", "https://test.api.flow.io", routes = Nil)
        )
      )
    )
  }

  "single service w/ operations" in {
    val spec = """
user:
  host: https://user.api.flow.io
  operations:
    - GET /users
    - POST /users
    - GET /users/:id
"""
    ServiceParser.parse(spec) must be(
      Right(
        Seq(
          Service(
            "user",
            "https://user.api.flow.io",
            routes = Seq(
              Route("GET", "/users"),
              Route("POST", "/users"),
              Route("GET", "/users/:id")
            )
          )
        )
      )
    )
  }

}
