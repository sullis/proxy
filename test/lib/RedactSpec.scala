package lib

import org.scalatestplus.play.PlaySpec

class RedactSpec extends PlaySpec {

  "auth" in {
    def verifyUntouched(m: Map[String, String]) = {
      Redact.auth(m) must be(m)
    }
    verifyUntouched(Map.empty)
    verifyUntouched(Map("foo" -> "bar"))
    verifyUntouched(Map("foo" -> "bar", "a" -> "b"))

    Redact.auth(Map("foo" -> "bar", "X-Auth-Foo" -> "bar")) must be(
      Map("foo" -> "bar", "X-Auth-Foo" -> "[redacted]")
    )
    Redact.auth(Map("foo" -> "bar", "x-authorization-other" -> "bar")) must be(
      Map("foo" -> "bar", "x-authorization-other" -> "[redacted]")
    )
    Redact.auth(Map("foo" -> "bar", "Authorization" -> "basic 123565")) must be(
      Map("foo" -> "bar", "Authorization" -> "[redacted]")
    )
  }

}