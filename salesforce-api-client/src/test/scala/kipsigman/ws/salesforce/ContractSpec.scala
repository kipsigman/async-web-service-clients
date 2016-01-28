package kipsigman.ws.salesforce

import org.scalatest.Matchers
import org.scalatest.WordSpec

import play.api.libs.json._

class ContractSpec extends WordSpec with Matchers with SampleData {

  "selectFields" should {
    "return the correct representations" in {
      Contract.selectFields shouldBe Seq("Id", "Opportunity__c", "StartDate", "EndDate")
      Contract.selectFieldsStr shouldBe "Id, Opportunity__c, StartDate, EndDate"
      Contract.objectPrefixedSelectFields shouldBe Seq("Contract.Id", "Contract.Opportunity__c", "Contract.StartDate", "Contract.EndDate")
    }
  }

  "JSON deserialization" should {
    "read valid JSON" in {
      sampleContractJson.validate[Contract] match {
        case s: JsSuccess[Contract] => {
          val contract = s.get
          contract shouldBe sampleContract
        }
        case e: JsError => {
          fail(s"error=$e")
        }
      }
    }
  }

}