package clients

import play.api.Configuration

@javax.inject.Singleton
class DefaultTokenClient @javax.inject.Inject() () extends io.flow.token.v0.Client(baseUrl="https://token.api.flow.io")
