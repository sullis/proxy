package filters

import javax.inject.Inject
import akka.stream.Materializer
import play.api.http.HttpFilters
import play.api.Logger
import play.api.mvc._
import play.filters.cors.CORSFilter
import scala.concurrent.{ExecutionContext, Future}

/**
  * Taken from lib-play to avoid pulling in lib-play as a dependency
  */
class CorsWithLoggingFilter @javax.inject.Inject() (corsFilter: CORSFilter, loggingFilter: LoggingFilter) extends HttpFilters {
  def filters = Seq(corsFilter, loggingFilter)
}

class LoggingFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime
      Logger.info(s"${requestHeader.method} ${requestHeader.host}${requestHeader.uri} ${result.header.status} ${requestTime}ms")

      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }
}
