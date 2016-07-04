package lib

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import scala.io.Source

case class Service(
  name: String,
  host: String,
  routes: Seq[Route]
)

case class Route(
  method: String,
  path: String
) {
  assert(method == method.toUpperCase, s"Method[$method] must be in upper case")
  assert(path == path.toLowerCase, s"Path[$path] must be in lower case")
}

@Singleton
class ServicesConfig @Inject() (
  configuration: Configuration
) {

  private[this] val Uri = configuration.getString("proxy.config.uri").getOrElse {
    sys.error("Missing configuration parameter[proxy.config.uri]")
  }

  /**
    * Loads service definitions from the specified URI
    */
  def load(uri: String): Either[Seq[String], Seq[Service]] = {
    val contents = Source.fromURL(uri).mkString
    ServiceParser.parse(contents)
  }

  private[this] def refresh(): Option[Services] = {
    load(Uri) match {
      case Left(errors) => {
        Logger.error(s"Failed to load proxy configuration from Uri[$Uri]: $errors")
        None
      }
      case Right(all) => {
        Option(Services(all))
      }
    }
  }

  private[this] var lastLoad: Services = refresh().getOrElse {
    Services(Nil)
  }

  def current(): Services = lastLoad

}

case class Services(all: Seq[Service]) {

  /**
    * This is a map from path to service allowing us to quickly identify
    * to which service we route an incoming request to.
    */
  private[this] val routes: Seq[InternalRoute] = {
    all.flatMap { s =>
      s.routes.map { r =>
        InternalRoute(r, s)
      }
    }
  }

  def resolve(method: String, path: String): Option[InternalRoute] = {
    routes.find(_.matches(method.toUpperCase, path.toLowerCase.trim))
  }

}
