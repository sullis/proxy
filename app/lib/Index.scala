package lib

case class Index(config: ProxyConfig) {

  /**
    * This is a map from path to service allowing us to quickly identify
    * to which service we route an incoming request to.
    */
  private[this] val routes: Seq[InternalRoute] = {
    config.services.flatMap { s =>
      s.routes.map { r =>
        InternalRoute(r, s)
      }
    }
  }

  def resolve(method: String, path: String): Option[InternalRoute] = {
    routes.find(_.matches(method.toUpperCase, path.toLowerCase.trim))
  }

  def resolveByHost(host: String, method: String, path: String): Option[InternalRoute] = {
    // routes.find(_.matches(method.toUpperCase, path.toLowerCase.trim))
    config.services.find(_.host == host).map { svc =>
      InternalRoute(
        route = Route(method, path),
        service = svc
      )
    }
  }

}
