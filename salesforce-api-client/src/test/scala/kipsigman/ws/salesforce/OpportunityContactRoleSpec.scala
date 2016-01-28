package kipsigman.ws.salesforce

import OpportunityContactRole.Role;

import org.scalatest.Matchers
import org.scalatest.WordSpec

import play.api.libs.json._

class OpportunityContactRoleSpec extends WordSpec with Matchers with SampleData {

  "JSON deserialization" should {
    "read valid JSON" in {

      sampleOpportunityContactRoleJson.validate[OpportunityContactRole] match {
        case s: JsSuccess[OpportunityContactRole] => {
          val ocr = s.get
          ocr shouldBe sampleOpportunityContactRole
        }
        case e: JsError => {
          fail(s"error=$e")
        }
      }
    }
  }

  "Role.name" should {
    "return proper name for Role objects" in {
      Role.BusinessUser.name shouldBe "Business User"
      Role.TechnicalBuyer.name shouldBe "Technical Buyer"
    }
  }

  "Role.apply" should {
    "select existing Role object" in {
      Role.apply(Role.BusinessUser.name) shouldBe Role.BusinessUser
      Role.apply(Role.TechnicalBuyer.name) shouldBe Role.TechnicalBuyer
    }
    "create new Role when existing object not found" in {
      val roleName = "Bogus"
      val role = Role.apply(roleName)
      role shouldBe Role.Unknown(roleName)
      role.name shouldBe roleName
    }
  }

}