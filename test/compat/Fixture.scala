package compat

import java.io.File

import play.api.libs.json.{JsObject, Json}

case class Fixture(original: JsObject, expected: JsObject)

object Fixture {

  private[this] val CommentCharacter = "#"

  def load(file: File): Fixture = {
    scala.io.Source.fromFile(file).getLines.mkString("\n").
      split("\n").map(_.trim).filter { l => !l.startsWith(CommentCharacter) }.mkString("\n").
      trim.split("\n\n").toList match {
      case original :: expected :: Nil => {
        Fixture(
          original = Json.parse(original).as[JsObject],
          expected = Json.parse(expected).as[JsObject]
        )
      }
      case _ => sys.error(s"File[$file] Could not parse contents - no newline found")
    }
  }

}