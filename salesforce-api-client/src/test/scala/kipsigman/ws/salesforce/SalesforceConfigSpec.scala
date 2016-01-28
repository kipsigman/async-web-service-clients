package kipsigman.ws.salesforce

import org.scalatest.Matchers
import org.scalatest.WordSpec

import com.typesafe.config._

class SalesforceConfigSpec extends WordSpec with Matchers with SampleData {

  val config = ConfigFactory.load()
  val sfc = new SalesforceConfig(config)

  "properties" should {
    "match app/library config" in {
      sfc.apiVersion shouldBe config.getString("sfdc.apiVersion")
      sfc.appId shouldBe config.getString("sfdc.appId")
      sfc.appSecret shouldBe config.getString("sfdc.appSecret")
      sfc.oauthHost shouldBe config.getString("sfdc.oauthHost")
      sfc.passtoken shouldBe config.getString("sfdc.passtoken")
      sfc.user shouldBe config.getString("sfdc.user")
      sfc.servicePath shouldBe s"/services/data/${sfc.apiVersion}"
    }
  }
  "oauthTokenUrl" should {
    "return oauthHost + path" in {
      sfc.oauthTokenUrl shouldBe "https://test.salesforce.com/services/oauth2/token"
    }
  }
  "serviceUrl" should {
    "return instance + path" in {
      val instanceUrl = "https://cs45.salesforce.com"
      sfc.serviceUrl(instanceUrl) shouldBe s"${instanceUrl}${sfc.servicePath}"
    }
  }
}