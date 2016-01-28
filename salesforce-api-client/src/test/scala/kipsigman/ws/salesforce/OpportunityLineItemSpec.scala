package kipsigman.ws.salesforce

import org.scalatest.Matchers
import org.scalatest.WordSpec

import play.api.libs.json._
import Product._

class OpportunityLineItemSpec extends WordSpec with Matchers with SampleData {

  "JSON deserialization" should {
    "read valid JSON" in {

      sampleOpportunityLineItemJson.validate[OpportunityLineItem] match {
        case s: JsSuccess[OpportunityLineItem] => {
          val oli = s.get
          oli shouldBe sampleOpportunityLineItem
        }
        case e: JsError => {
          fail(s"error=$e")
        }
      }
    }
  }

  "productDisplayText" should {
    "return Product.name and quantity if not 1" in {
      val product = Product(Option("01tC0000003W5oFIAS"), "1 Year Subscription", ProductFamily.A)
      val pricebookEntry = PricebookEntry(Option("01uC0000008zQcyIAE"), product)
      val oli = OpportunityLineItem(Option("00kM0000007CAsDIAW"), 2, pricebookEntry)
      oli.productDisplayText shouldBe "1 Year Subscription (2)"
    }
  }

}