package kipsigman.ws.salesforce

import org.scalatest.time._
import org.scalatest.WordSpec

import OpportunityContactRole.Role

class SalesforceApiClientWritesIntegrationSpec extends SalesforceApiClientIntegrationSpec {

  private def cleanupData = {
    client.saveObjectStringField[Account](sampleAccount.id.get, "Type", Account.AccountType.Customer.name)
  }

  "cleanupDataPre" should {
    "cleanupData" in {
      cleanupData
      1 + 1 shouldBe 2
    }
  }

  //  "saveObjectStringField" should {
  //    "update object field" in {
  //      client.saveObjectStringField[Account](sampleAccount.id.get, "Type", "IT value")
  //    }
  //  }
  //
  //  "touchOpportunity" should {
  //    "update timestamp" in {
  //      client.touchOpportunity(sampleOpportunityId)
  //      1 + 1 shouldBe 2
  //    }
  //  }

  //  "add new Contact & OpportunityContactRole" should {
  //    "create new entities" in {
  //      val contactData = ContactData("Ben", "Harp", "ben.harp@fbi.gov", Option("555-555-5555"))
  //      val ocrFuture = client.findOpportunityById(sampleOpportunityId).flatMap(opp => {
  //        client.addOpportunityNamedContact(opp.get, contactData)
  //      })
  //      val ocrOptionFuture = ocrFuture.flatMap(ocr => {
  //        client.findOpportunityContactRoleById(ocr.id.get)
  //      })
  //      whenReady(ocrOptionFuture) { ocrOption =>
  //        ocrOption shouldBe defined
  //
  //        // Delete to clean up
  //        //client.deleteOpportunityContactRole(ocrOption.get.id.get)
  //      }
  //    }
  //  }

  "cleanupDataPost" should {
    "cleanupData" in {
      cleanupData
      1 + 1 shouldBe 2
    }
  }
}