package clients

import play.api.Configuration

@javax.inject.Singleton
class DefaultTokenClient @javax.inject.Inject() (
  configuration: Configuration
) extends io.flow.token.v0.Client(
  configuration.getString("service.token.uri").getOrElse {
    sys.error("Missing config parameter[service.token.uri]")
  }
)
