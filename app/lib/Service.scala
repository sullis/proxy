package lib

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

case class Services(config: ProxyConfig) {

  val all: Seq[Service] = config.services

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
