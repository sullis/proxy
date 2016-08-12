package lib

import javax.inject.{Inject, Singleton}
import play.api.Logger
import scala.io.Source

/**
  * Responsible for downloading the configuration from the URL
  * specified by the configuration parameter named
  * proxy.config.uris. Exposes an API to refresh the configuration
  * periodically.
  * 
  * When downloading the configuration, we load it into an instance of
  * the Index class to pre-build the data needed to resolve paths.
  */
@Singleton
class ProxyConfigFetcher @Inject() (
  config: Config
) {

  private[this] lazy val Uris: Seq[String] = config.requiredString("proxy.config.uris").split(",").map(_.trim)

  /**
    * Loads service definitions from the specified URI
    */
  def load(uris: Seq[String]): Either[Seq[String], ProxyConfig] = {
    Logger.info(s"ProxyConfigFetcher: fetching configuration from uris[$uris]")
    val uri = uris.headOption.getOrElse {
      sys.error("No uris")
    }

    // TODO: Handle second configuration file

    val contents = Source.fromURL(uri).mkString
    ServiceParser.parse(contents)
  }

  private[this] def refresh(): Option[Index] = {
    load(Uris) match {
      case Left(errors) => {
        Logger.error(s"Failed to load proxy configuration from Uris[$Uris]: $errors")
        None
      }
      case Right(cfg) => {
        Option(Index(cfg))
      }
    }
  }

  private[this] var lastLoad: Index = refresh().getOrElse {
    Index(
      ProxyConfig(version = "0.0.0", services = Nil)
    )
  }

  def current(): Index = lastLoad

}
