package lib

import controllers.{ServiceProxy, ServiceProxyDefinition, ServiceProxyImpl}
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source

class ServiceParserSpec extends PlaySpec with OneServerPerSuite {

  private[this] lazy val serviceProxyFactory = play.api.Play.current.injector.instanceOf[ServiceProxy.Factory]

  val uri = "file:///test"

  val source = ProxyConfigSource(
    uri = uri,
    version = "0.0.1"
  )

  "empty" in {
    ServiceParser.parse(uri, "   ") must be(Left(Seq("Nothing to parse")))
  }

  "hostHeaderValue" in {
    Seq("http://user.api.flow.io", "https://user.api.flow.io").foreach { host =>
      ServiceProxyDefinition(host, "user").hostHeaderValue must be("user.api.flow.io")
    }
  }

  "single service w/ no operations" in {
    val spec = """
version: 0.0.1
services:
  test:
    host: https://test.api.flow.io
"""
    ServiceParser.parse(uri, spec) must be(
      Right(
        ProxyConfig(
          sources = Seq(source),
          services = Seq(
            Service("test", "https://test.api.flow.io", routes = Nil)
          )
        )
      )
    )
  }

  "single service w/ operations" in {
    val spec = """
version: 1.2.3
services:
  user:
    host: https://user.api.flow.io
    operations:
      - GET /users
      - POST /users
      - GET /users/:id
"""
    ServiceParser.parse(uri, spec) must be(
      Right(
        ProxyConfig(
          sources = Seq(source.copy(version = "1.2.3")),
          services = Seq(
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
    )
  }

  "latest production config" in {
    val uri = "https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-proxy/production.config"
    val contents = Source.fromURL(uri).mkString
    ServiceParser.parse(uri, contents) match {
      case Left(errors) => {
        sys.error(s"Failed to parse config at URI[$uri]: $errors")
      }

      case Right(config) => {
        Seq("user", "organization", "catalog").foreach { name =>
          val svc = config.services.find(_.name == name).getOrElse {
            sys.error(s"Failed to find service[$name]")
          }
          svc.host must be(s"https://$name.api.flow.io")
        }

        val index = Index(config)
        Seq(("GET", "/users"), ("GET", "/organizations"), ("GET", "/:organization/catalog")).foreach { case (method, path) =>
          val r = index.resolve(method, path).getOrElse {
            sys.error(s"Failed to resolve path[$path]")
          }
          r.method must be(method)
          r.path must be(path)
        }

        // make sure all services have a defined execution context
        config.services.filter { svc =>
          serviceProxyFactory(ServiceProxyDefinition(svc.host, svc.name)).asInstanceOf[ServiceProxyImpl].executionContextName == ServiceProxy.DefaultContextName
        }.map(_.name).toList match {
          case Nil => {}
          case names => {
            sys.error("All services must have their own execution context. Please update conf/base.conf to add contexts named: " + names.map { n => s"$n-context" }.sorted.mkString(", "))
          }
        }
      }
    }
  }

  "latest development config" in {
    val uri = "https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-proxy/development.config"
    val contents = Source.fromURL(uri).mkString
    ServiceParser.parse(uri, contents) match {
      case Left(errors) => {
        sys.error(s"Failed to parse config at URI[$uri]: $errors")
      }

      case Right(config) => {
        Map(
          "user" -> "http://localhost:6021",
          "organization" -> "http://localhost:6081",
          "catalog" -> "http://localhost:6071"
        ).foreach { case (name, host) =>
          val svc = config.services.find(_.name == name).getOrElse {
            sys.error(s"Failed to find service[$name]")
          }
          svc.host must be(host)
        }

        val index = Index(config)
        Seq(("GET", "/users"), ("GET", "/organizations"), ("GET", "/:organization/catalog")).foreach { case (method, path) =>
          val r = index.resolve(method, path).getOrElse {
            sys.error(s"Failed to resolve path[$path]")
          }
          r.method must be(method)
          r.path must be(path)
        }
      }
    }
  }

  "internal routes" in {
    val uris = Seq(
      "https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-proxy/development.config",
      "https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-internal-proxy/development.config"
    )
    val proxyConfigFetcher = play.api.Play.current.injector.instanceOf[ProxyConfigFetcher]
    val config = proxyConfigFetcher.load(uris).right.get

    Seq("currency", "currency-internal").foreach { name =>
      config.services.find(_.name == name).getOrElse {
        sys.error(s"Failed to find service[$name]")
      }
    }

    val index = Index(config)
    index.resolve("GET", "/test/currency/rates").get.path must be("/:organization/currency/rates")
    index.resolve("GET", "/internal/currency/rates/test").get.path must be("/internal/currency/rates/:organization")
  }
  
}
