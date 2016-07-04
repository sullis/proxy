package lib

/**
  * Manages paths, matching an incoming request to a known path
  */
sealed trait InternalRoute {

  def service: Service

  def matches(method: String, path: String): Boolean 

}

object InternalRoute {

  /**
    * Represents a static route (e.g. /organizations) with no wildcards
    */
  case class Static(method: String, path: String, override val service: Service) extends InternalRoute {
    assert(method == method.toUpperCase.trim, s"Method[$method] must be upper case trimmed")
    assert(path == path.toLowerCase.trim, s"path[$path] must be lower case trimmed")

    override def matches(incomingMethod: String, incomingPath: String): Boolean = {
      method == incomingMethod && path == incomingPath
    }

  }

  /**
    * Represents a dynamic route (e.g. /organizations/:id) with
    * wildcards. We implement this be building a regular expression
    * that replaces any ":xxx" with a pattern of one or more
    * characters that are not a '/'
    */
  case class Dynamic(method: String, path: String, override val service: Service) extends InternalRoute {
    assert(method == method.toUpperCase.trim, s"Method[$method] must be upper case trimmed")
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

    override def matches(incomingMethod: String, incomingPath: String): Boolean = {
      method == incomingMethod match {
        case true => incomingPath match {
          case pattern() => true
          case _ => false
        }
        case false => {
          false
        }
      }
    }

  }

  def apply(route: Route, service: Service): InternalRoute = {
    route.path.indexOf(":") >= 0 match {
      case true => Dynamic(route.method, route.path, service)
      case false => Static(route.method, route.path, service)
    }
  }

}
