package lib

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.scanner.ScannerException
import play.api.Logger
import play.api.libs.json.JsValue
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

          val obj = y match {
            case None => Map[String, JsValue]()
            case Some(data) => data.asInstanceOf[java.util.Map[String, JsValue]].asScala
          }

          val services: Seq[Service] = obj.flatMap {
            case (name, js) => {
              toString(js \ "host").map(_.trim).getOrElse("") match {
                case "" => {
                  Logger.warn(s"Configuration error for app[$name]: Missing host")
                  None
                }

                case host => {
                  toStringArray(js \ "operations").toList match {
                    case Nil => {
                      Logger.warn(s"Configuration error for app[$name] host[$host]: There are no operations defined")
                      None
                    }

                    case operations => {
                      println(s"operations: $operations")
                      Some(
                        Service(
                          name = name.trim,
                          host = host,
                          routes = Nil
                        )
                      )
                    }
                  }
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
      case v: java.lang.String => Some(v.trim)
      case _ => None
    }
  }

  private[this] def toStringArray(obj: Any): Seq[String] = {
    obj match {
      case v: java.lang.String => Seq(v)
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
