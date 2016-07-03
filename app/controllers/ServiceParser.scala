package controllers

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
        Left(Nil)
      }

      case value => {
        val yaml = new Yaml()

        Try {
          val y = Option(yaml.load(contents))

          val obj = y match {
            case None => Map[String, Object]()
            case Some(data) => data.asInstanceOf[java.util.Map[String, Object]].asScala
          }

          println(obj)

          Nil

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
