package lib

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.scanner.ScannerException
import play.api.Logger
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Parses the contents of the .delta file
  */
object ServiceParser {

  def parse(
    contents: String
  ): Either[Seq[String], Seq[Service]] = {
    contents.trim match {
      case "" => {
        Left(Seq("Nothing to parse"))
      }

      case value => {
        val yaml = new Yaml()

        Try {
          val y = Option(yaml.load(contents))

          val all = y match {
            case None => Map[String, Object]()
            case Some(data) => data.asInstanceOf[java.util.Map[String, Object]].asScala
          }

          val services: Seq[Service] = all.flatMap {
            case (name, objJs) => {
              val obj = toMap(objJs)

              toString(obj.get("host")).map(_.trim).getOrElse("") match {
                case "" => {
                  Logger.warn(s"Configuration error for app[$name]: Missing host")
                  None
                }

                case host => {
                  val operations = toStringArray(obj.get("operations"))
                  Some(
                    Service(
                      name = name.trim,
                      host = host,
                      routes = operations.flatMap { op =>
                        op.split("\\s").toList match {
                          case method :: path :: Nil => {
                            Some(
                              Route(
                                method = method,
                                path = path
                              )
                            )
                          }
                          case other => {
                            Logger.warn(s"Configuration error for app[$name]: Invalid operation[$op]")
                            None
                          }
                        }
                      }
                    )
                  )
                }
              }
            }
          }.toSeq

          services

        } match {
          case Success(services) => {
            Right(services)
          }

          case Failure(ex) => {
            Left(Seq(ex.getMessage))
          }
        }
      }
    }
  }

  private[this] def toString(obj: Any): Option[String] = {
    obj match {
      case Some(v: java.lang.String) => Some(v.trim)
      case v: java.lang.String => Some(v.trim)
      case _ => None
    }
  }

  private[this] def toStringArray(obj: Any): Seq[String] = {
    obj match {
      case Some(v: java.lang.String) => Seq(v)
      case v: java.lang.String => Seq(v)
      case Some(ar: java.util.ArrayList[_]) => ar.asScala.map(_.toString)
      case ar: java.util.ArrayList[_] => ar.asScala.map(_.toString)
      case _ => Nil
    }
  }

  private[this] def toMapString(value: Any): Map[String, String] = {
    toMap(value).map { case (key, value) => (key -> value.toString) }
  }

  private[this] def toMap(value: Any): Map[String, Any] = {
    value match {
      case map: java.util.HashMap[_, _] => {
        map.asScala.map { case (key, value) =>
          (key.toString -> value)
        }.toMap
      }

      case _ => {
        Map.empty
      }
    }
  }
}
