package kipsigman.ws.client

import scala.concurrent.ExecutionContext

import dispatch._

import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatest.WordSpec
import org.scalatest.mock.MockitoSugar
import org.slf4j.LoggerFactory

import play.api.http._
import play.api.libs.json._

class RestWebServiceClientSpec extends WordSpec with Matchers with MockitoSugar {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val client = new TestClient

  "prepareReq" should {
    "set Accept and Content-Type for JSON" in {
      val sampleUrl = "https://www.googleapis.com/language/translate/v2"
      val baseReq = url(sampleUrl)
      val preparedReq = client.prepareReq(baseReq, HttpVerbs.GET)

      val headers = preparedReq.toRequest.getHeaders
      headers.get("Accept").get(0) shouldBe "application/json; charset=utf-8"
      headers.get("Content-Type").get(0) shouldBe "application/json; charset=utf-8"

      preparedReq.toRequest.getMethod shouldBe HttpVerbs.GET
      preparedReq.toRequest.getUrl shouldBe sampleUrl
    }
  }

}

class TestClient extends RestWebServiceClient {
  protected implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}