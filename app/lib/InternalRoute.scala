package lib

/**
  * Manages paths, matching an incoming request to a known path
  */
sealed trait InternalRoute {

  def service: Service

  def matches(path: String): Boolean 

}

object InternalRoute {

  /**
    * Represents a static route (e.g. /organizations) with no wildcards
    */
  case class Static(path: String, override val service: Service) extends InternalRoute {
    assert(path == path.toLowerCase.trim, s"path[$path] must be lower case trimmed")

    override def matches(incomingPath: String): Boolean = {
      path == incomingPath
    }

  }

  /**
    * Represents a dynamic route (e.g. /organizations/:id) with
    * wildcards. We implement this be building a regular expression
    * that replaces any ":xxx" with a pattern of one or more
    * characters that are not a '/'
    */
  case class Dynamic(path: String, override val service: Service) extends InternalRoute {
    assert(path == path.toLowerCase.trim, s"path[$path] must be lower case trimmed")

    private[this] val pattern = (
      "^" +
        path.split("/").map { p =>
          p.startsWith(":") match {
            case true => {
              """[^\/]+"""
            }
            case false => {
              p
            }
          }
        }.mkString("""\/""") +
        "$"
    ).r

    override def matches(incomingPath: String): Boolean = {
      incomingPath match {
        case pattern() => true
        case _ => false
      }
    }

  }

  def apply(route: Route, service: Service): InternalRoute = {
    route.path.indexOf(":") >= 0 match {
      case true => Dynamic(route.path, service)
      case false => Static(route.path, service)
    }
  }

}
