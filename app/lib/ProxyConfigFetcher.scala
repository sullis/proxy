package lib

import javax.inject.{Inject, Singleton}
import play.api.Logger
import scala.io.Source

/**
  * Responsible for downloading the configuration from the URL
  * specified by the configuration parameter named
  * proxy.config.url. Exposes an API to refresh the configuration
  * periodically.
  * 
  * When downloading the configuration, we load it into an instance of
  * the Services class to pre-build the data needed to resolve paths.
  */
@Singleton
class ProxyConfigFetcher @Inject() (
  config: Config
) {

  private[this] lazy val Uri = config.requiredString("proxy.config.uri")

  /**
    * Loads service definitions from the specified URI
    */
  def load(uri: String): Either[Seq[String], ProxyConfig] = {
    val contents = Source.fromURL(uri).mkString
    ServiceParser.parse(contents)
  }

  private[this] def refresh(): Option[Services] = {
    load(Uri) match {
      case Left(errors) => {
        Logger.error(s"Failed to load proxy configuration from Uri[$Uri]: $errors")
        None
      }
      case Right(cfg) => {
        Option(Services(cfg))
      }
    }
  }

  private[this] var lastLoad: Services = refresh().getOrElse {
    Services(
      ProxyConfig(version = "0.0.0", services = Nil)
    )
  }

  def current(): Services = lastLoad

}
