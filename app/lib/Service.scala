package lib

import scala.io.Source

case class Service(
  name: String,
  host: String,
  routes: Seq[Route]
)

case class Route(
  method: String,
  path: String
)

object Services {

  /**
    * Loads service definitions from the specified URI
    */
  def load(uri: String): Either[Seq[String], Services] = {
    val contents = Source.fromURL(uri).mkString
    ServiceParser.parse(contents) match {
      case Left(errors) => Left(errors)
      case Right(services) => Right(Services(services))
    }
  }

}

case class Services(all: Seq[Service]) {

  /**
    * This is a map from path to service allowing us to quickly identify
    * to which service we route an incoming request to.
    */
  private[this] val byPath: Map[String, Service] = {
    Map(
      all.flatMap { s =>
        s.routes.map { r =>
          (r.path.toLowerCase -> s)
        }
      }: _*
    )
  }

  def findByPath(path: String): Option[Service] = {
    byPath.get(path.toLowerCase) match {
      case Some(s) => Some(s)
      case None => {
        println(s"Could not find path[$path]. Tried:")
        byPath.keys.toSeq.sorted.foreach { p =>
          println(s" - [$p]")
        }
        None
      }
    }
  }

}
