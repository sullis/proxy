package lib

/**
  * Manages paths, matching an incoming request to a known path
  */
sealed trait InternalRoute {

  def method: String
  def path: String
  def service: Service

  private[this] val hasOrganization: Boolean = path == "/:organization" || path.startsWith("/:organization/")

  /**
    * By naming convention, if the path starts with /:organization, we
    * know that we need to authenticate that the requesting user has
    * access to that organization.
    */
  def organization(requestPath: String): Option[String] = {
    hasOrganization match {
      case false => None
      case true => {
        requestPath.split("/").toList match {
          case empty :: org :: rest => Some(org)
          case _ => sys.error(s"Service[${service.name}] $method $path: Could not extract organization from path[$requestPath]")
        }
      }
    }
  }

}

object InternalRoute {

  /**
    * Represents a static route (e.g. /organizations) with no wildcards
    */
  case class Static(method: String, path: String, service: Service) extends InternalRoute {
    assert(method == method.toUpperCase.trim, s"Method[$method] must be upper case trimmed")
    assert(path == path.toLowerCase.trim, s"path[$path] must be lower case trimmed")
  }

  /**
    * Represents a dynamic route (e.g. /organizations/:id) with
    * wildcards. We implement this be building a regular expression
    * that replaces any ":xxx" with a pattern of one or more
    * characters that are not a '/'
    */
  case class Dynamic(method: String, path: String, service: Service) extends InternalRoute {
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

    def matches(incomingMethod: String, incomingPath: String): Boolean = {
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
