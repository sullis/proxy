package lib

import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._
//import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

class ServiceParserSpec extends PlaySpec with OneServerPerSuite { // with ScalaFutures with IntegrationPatience {

  "empty" in {
    ServiceParser.parse("   ") must be(Left(Seq("Nothing to parse")))
  }

}
