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
    * Loads service definitions from the specified URIs
    */
  private[this] def load(uris: Seq[String]): Either[Seq[String], ProxyConfig] = {
    uris.toList match {
      case Nil => {
        sys.error("Must have at least one configuration uri")
      }

      case uri :: rest => {
        load(uri) match {
          case Left(errors) => {
            Left(errors)
          }
          case Right(config) => {
            combine(rest, config)
          }
        }
      }
    }
  }

  @scala.annotation.tailrec
  private[this] def combine(uris: Seq[String], config: ProxyConfig): Either[Seq[String], ProxyConfig] = {
    uris.toList match {
      case Nil => {
        Right(config)
      }

      case uri :: rest => {
        load(uri) match {
          case Left(errors) => {
            Left(errors)
          }
          case Right(newConfig) => {
            combine(rest, config.merge(newConfig))
          }
        }
      }
    }
  }
  
  /**
    * Loads service definitions from the specified URI
    */
  private[this] def load(uri: String): Either[Seq[String], ProxyConfig] = {
    Logger.info(s"ProxyConfigFetcher: fetching configuration from uri[$uri]")
    val contents = Source.fromURL(uri).mkString
    ServiceParser.parse(uri, contents)
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
      ProxyConfig(
        sources = Nil,
        services = Nil
      )
    )
  }

  def current(): Index = lastLoad

}
