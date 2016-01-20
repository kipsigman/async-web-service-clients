package kipsigman.ws.client

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.slf4j.LoggerFactory

import com.ning.http.client.Response

import dispatch._
import play.api.http._
import play.api.libs.json._

/**
 * Base trait for an asyncronous Scala RESTful web service client.
 * Contains error handling and helpful debug logging.
 */
trait RestWebServiceClient {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected implicit val ec: ExecutionContext

  /**
   * Prepares request with content headers, method, etc
   */
  protected[ws] def prepareReq(
    baseReq: Req,
    method: String,
    mimeType: String = MimeTypes.JSON,
    postProcess: Req => Req = x => x): Req = {

    // Add Content Type headers
    val contentType = ContentTypes.withCharset(mimeType) // Adds charset
    val req1 = baseReq.addHeader(HeaderNames.ACCEPT, contentType).addHeader(HeaderNames.CONTENT_TYPE, contentType)

    // Add Method
    val req2 = method match {
      case HttpVerbs.DELETE => req1.DELETE
      case HttpVerbs.GET => req1.GET
      case HttpVerbs.PATCH => req1.PATCH
      case HttpVerbs.POST => req1.POST
      case HttpVerbs.PUT => req1.PUT
    }

    // Post process and return
    postProcess(req2)
  }

  /**
   * Makes basic call
   */
  protected def execute(req: Req): Future[Response] = {
    Http(req > as.Response(response => {
      logger.debug(s"""execute:{"request":"${req.toRequest}", "responseStatusCode":${response.getStatusCode}, "responseBody":${response.getResponseBody}}""")
      response
    }))
  }

  /**
   * Makes call and validates Response status returning:
   * - Some(Response): Resource found, call successful
   * - None: Resource not found
   * @throws WebServiceException if an unexpected status is returned
   */
  protected def execute(req: Req, method: String): Future[Option[Response]] = {
    val validPStatuses = Seq(Status.OK, Status.CREATED, Status.ACCEPTED, Status.NO_CONTENT)

    execute(req).map(response =>
      (method, response.getStatusCode) match {
        case (HttpVerbs.DELETE, Status.OK) => Option(response)
        case (HttpVerbs.DELETE, Status.NO_CONTENT) => Option(response)
        case (HttpVerbs.GET, Status.OK) => Option(response)
        case (HttpVerbs.PATCH, s) if (validPStatuses.contains(s)) => Option(response)
        case (HttpVerbs.POST, s) if (validPStatuses.contains(s)) => Option(response)
        case (HttpVerbs.PUT, s) if (validPStatuses.contains(s)) => Option(response)
        case (m, Status.NOT_FOUND) if (m != HttpVerbs.POST) => None
        case _ => throw RestException(response)
      }
    )
  }

  protected def executeJson(req: Req, method: String): Future[Option[JsValue]] = {
    val responseOptionFuture = execute(req, method).map(_.map(response => {
      if (response.getResponseBody.isEmpty()) {
        Json.obj()
      } else {
        val responseBody = response.getResponseBody
        Json.parse(responseBody)
      }
    }))
    responseOptionFuture onFailure {
      case t: Throwable => logger.error("executeJson exception", t)
    }
    responseOptionFuture
  }
}

case class RestException(response: Response) extends Throwable({
  val jsonStr = Json.stringify(Json.obj("statusCode" -> response.getStatusCode, "statusText" -> response.getStatusText, "responseBody" -> response.getResponseBody))
  s"Web Service Exception: $jsonStr"
})