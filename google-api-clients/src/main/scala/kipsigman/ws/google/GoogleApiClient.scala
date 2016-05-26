package kipsigman.ws.google

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.typesafe.config.Config

import dispatch.Req
import dispatch.url

import play.api.http.HttpVerbs
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import kipsigman.ws.client.RestWebServiceClient

abstract class GoogleApiClient(config: Config)(protected implicit val ec: ExecutionContext) extends RestWebServiceClient {

  protected def apiKey = config.getString("google.apiKey")
  protected def applicationName = config.getString("google.applicationName")
  protected def serviceAccountEmail = config.getString("google.serviceAccountEmail")
  
  protected def apiKeyAuthorization(req: Req): Req = {
    req <<? Map("key" -> apiKey)
  }
  
  protected def executeGoogleApiReq(baseReq: Req, method: String, authorization: Req => Req): Future[Option[JsValue]] = {
    val req = prepareReq(baseReq, method, postProcess = authorization)
    executeJson(req, method)
  }

  protected def executeGetPage[T](baseReq: Req, authorization: Req => Req)(implicit ec: ExecutionContext, rds: Reads[T]): Future[Page[T]] = {
    executeGoogleApiReq(baseReq, HttpVerbs.GET, authorization) map {
      case Some(jsValue) => jsValue.as[Page[T]]
      case None => Page() 
    }
  }

  protected def executeGetPage[T](
    serviceUrl: String,
    params: Map[String, String],
    authorization: Req => Req = apiKeyAuthorization
    )(implicit ec: ExecutionContext, rds: Reads[T]): Future[Page[T]] = {
    
    val req = url(serviceUrl) <<? params
    executeGetPage(req, authorization)
  }
}

case class Page[T](
  items: Seq[T] = Seq[T](),
  resultsPerPage: Int = Page.defaultResultsPerPage,
  totalResults: Int = 0,
  prevPageToken: Option[String] = None,
  nextPageToken: Option[String] = None)

object Page {
  val defaultResultsPerPage = 10
  
  implicit def pageReads[T](implicit itemRds: Reads[T]): Reads[Page[T]] = (
    (JsPath \ "items").read[Seq[T]] and
    (JsPath \ "pageInfo" \ "resultsPerPage").read[Int] and
    (JsPath \ "pageInfo" \ "totalResults").read[Int] and
    (JsPath \ "prevPageToken").readNullable[String] and
    (JsPath \ "nextPageToken").readNullable[String]
  )(Page.apply[T] _)
  
  def offset(pageIndex: Int, resultsPerPage: Int): Int = pageIndex * resultsPerPage
}