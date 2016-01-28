package kipsigman.ws.salesforce

import org.scalatest.Matchers
import org.scalatest.WordSpec

import play.api.libs.json._

class ContactSpec extends WordSpec with Matchers with SampleData {

  "selectFields" should {
    "return the correct representations" in {
      Contact.selectFields shouldBe Seq("Id", "Account.Id", "Account.OwnerId", "Account.Name", "Account.Type", "FirstName", "LastName", "Email", "Phone", "Current_Subscriber__c")
      Contact.selectFieldsStr shouldBe "Id, Account.Id, Account.OwnerId, Account.Name, Account.Type, FirstName, LastName, Email, Phone, Current_Subscriber__c"
      Contact.objectPrefixedSelectFields shouldBe Seq("Contact.Id", "Contact.Account.Id", "Contact.Account.OwnerId", "Contact.Account.Name", "Contact.Account.Type", "Contact.FirstName", "Contact.LastName", "Contact.Email", "Contact.Phone", "Contact.Current_Subscriber__c")
    }
  }

  "JSON deserialization" should {
    "read valid JSON" in {
      sampleContactJson.validate[Contact] match {
        case s: JsSuccess[Contact] => {
          val contact = s.get
          contact shouldBe sampleContact
        }
        case e: JsError => {
          fail(s"error=$e")
        }
      }
    }
  }

  "name" should {
    "concatenate firstName and lastName" in {
      sampleContact.name shouldBe "Johnny Utah"
    }
  }

  "update" should {
    "replace non-reference fields" in {
      val contactData = ContactData("Johnny", "Utah", "johnny.utah@fbi.gov", Option("(805) 867-5309"))
      val updatedContact = sampleContact.update(contactData)
      updatedContact.id shouldBe sampleContact.id
      updatedContact.account shouldBe sampleContact.account
      updatedContact.firstName shouldBe contactData.firstName
      updatedContact.lastName shouldBe contactData.lastName
      updatedContact.email shouldBe contactData.email
      updatedContact.phone shouldBe contactData.phone
    }
  }
}