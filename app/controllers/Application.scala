package controllers

def reverseProxy = Action.async(parse.raw) {
  request: Request[RawBuffer] =>
    // Create the request to the upstream server:
    val proxyRequest =
      WS.url("http://localhost:8887" + request.path)
        .withFollowRedirects(false)
        .withMethod(request.method)
        .withVirtualHost("localhost:9000")
        .withHeaders(flattenMultiMap(request.headers.toMap): _*)
        .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
        .withBody(request.body.asBytes().get)
 
    // Stream the response to the client:
    proxyRequest.stream.map {
      case (headers: WSResponseHeaders, enum) => Result(
          ResponseHeader(headers.status, headers.headers.mapValues(_.head)),
          enum)
    }
}
