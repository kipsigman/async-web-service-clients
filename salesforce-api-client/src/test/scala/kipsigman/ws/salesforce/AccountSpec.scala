package kipsigman.ws.salesforce

import org.scalatest.Matchers
import org.scalatest.WordSpec

import play.api.libs.json._

class AccountSpec extends WordSpec with Matchers with SampleData {

  "JSON deserialization" should {
    "read valid JSON" in {
      sampleAccountJson.validate[Account] match {
        case s: JsSuccess[Account] => {
          val account = s.get
          account shouldBe sampleAccount
        }
        case e: JsError => {
          fail(s"error=$e")
        }
      }
    }
  }

}