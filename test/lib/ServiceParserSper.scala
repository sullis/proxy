package lib

import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._

class ServiceParserSpec extends PlaySpec with OneAppPerSuite {

  "empty" in {
    ServiceParser.parse("   ") must be(Left(Seq("Nothing to parse")))
  }

}
