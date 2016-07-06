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
