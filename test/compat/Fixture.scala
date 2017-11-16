package compat

import java.io.File

import play.api.libs.json.{JsObject, Json}

case class FixtureTestCase(
  locale: String,
  expected: JsObject
)

case class Fixture(
  original: JsObject,
  testCases: Seq[FixtureTestCase]
)

object Fixture {

  private[this] val CommentCharacter = "#"

  def load(file: File): Fixture = {
    scala.io.Source.fromFile(file).getLines.mkString("\n").
      split("\n").map(_.trim).filter { l => !l.startsWith(CommentCharacter) }.mkString("\n").
      trim.split("\n\n").toList match {
      case original :: rest => {
        Fixture(
          original = Json.parse(original).as[JsObject],
          testCases = rest.map(parseTestCase)
        )
      }
      case _ => sys.error(s"File[$file] Could not parse contents - no newline found")
    }
  }

  private[this] def parseTestCase(value: String): FixtureTestCase = {
    value.split("\n").toList match {
      case localeLine :: json => {
        if (!localeLine.startsWith("locale:")) {
          sys.error(s"Missing locale for test case: $value")
        }
        val locale: String = localeLine.split(":").last.trim
        FixtureTestCase(
          locale = locale,
          expected = Json.parse(json.mkString("\n")).as[JsObject]
        )
      }
      case _ => {
        sys.error(s"Missing locale for test case: $value")
      }
    }
  }

}