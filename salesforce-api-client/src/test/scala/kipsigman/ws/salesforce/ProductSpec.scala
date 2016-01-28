package kipsigman.ws.salesforce

import org.scalatest.Matchers
import org.scalatest.WordSpec

import play.api.libs.json._
import OpportunityContactRole.Role
import Product._

class ProductSpec extends WordSpec with Matchers with SampleData {

  "JSON deserialization" should {
    "read valid JSON" in {
      sampleProductJson.validate[Product] match {
        case s: JsSuccess[Product] => {
          val product2 = s.get
          product2 shouldBe sampleProduct
        }
        case e: JsError => {
          fail(s"error=$e")
        }
      }
    }
  }

}