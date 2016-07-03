package lib

import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source

class ServiceParserSpec extends PlaySpec with OneServerPerSuite {

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

  "latest production config" in {
    val uri = "https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-proxy/production.config"
    val contents = Source.fromURL(uri).mkString
    ServiceParser.parse(contents) match {
      case Left(errors) => {
        sys.error(s"Failed to parse config at URI[$uri]: $errors")
      }

      case Right(services) => {
        Seq("user", "organization", "catalog").foreach { name =>
          services.find(_.name == "user").getOrElse {
            sys.error(s"Failed to find service[$name]")
          }
        }

        val s = Services(services)
        Seq("/users", "/organizations", "/:organization/catalog").foreach { path =>
          s.findByPath(path).getOrElse {
            sys.error(s"Failed to resolve path[$path]")
          }
        }
      }
    }
  }

}
