package lib

object Redact {

  def auth(value: Map[String, String]): Map[String, String] = {
    value.map { case (k, v) =>
      if (isAuthorizationHeader(k)) {
        k -> "[redacted]"
      } else {
        k -> v
      }
    }
  }

  private[this] def isAuthorizationHeader(name: String): Boolean = {
    name.toLowerCase().indexOf("auth") >= 0
  }

}