package kipsigman.ws.salesforce

import java.time._

import org.scalatest.time._
import org.scalatest.WordSpec

import OpportunityContactRole.Role
import Product._

class SalesforceApiClientReadsIntegrationSpec extends SalesforceApiClientIntegrationSpec {

  //  "bad accessInfo" should {
  //    "return Exception" in {
  //      val badAccessInfo = accessInfo.copy(accessToken = "invalid")
  //      val accountFuture = client.findAccountById(sampleAccount.id.get)(badAccessInfo)
  //      whenReady(accountFuture) { accountOption =>
  //        fail
  //      }
  //    }
  //  }

  "findAccountByEmail" should {
    "return Account for valid email" in {
      val accountFuture = client.findAccountByEmail(sampleContactEmail)
      whenReady(accountFuture) { accountOption =>
        accountOption shouldBe defined
        accountOption.get shouldBe sampleAccount
      }
    }

    "return None invalid email" in {
      val email = "kxuwerjvkce@xutckid.com"
      val accountFuture = client.findAccountByEmail(email)
      whenReady(accountFuture) { accountOption =>
        accountOption shouldBe None
      }
    }
  }

  "findAccountById" should {
    "return Account for valid id" in {
      val accountFuture = client.findAccountById(sampleAccount.id.get)
      whenReady(accountFuture) { accountOption =>
        accountOption shouldBe defined
        accountOption.get shouldBe sampleAccount
      }
    }

    "return None for invalid id" in {
      val accountFuture = client.findAccountById("invalid")
      whenReady(accountFuture) { accountOption =>
        accountOption shouldBe None
      }
    }
  }

  "findContactByEmail" should {
    "return Contact for valid email" in {
      val contactFuture = client.findContactByEmail(sampleContactEmail)
      whenReady(contactFuture) { contactOption =>
        contactOption shouldBe defined
        val contact = contactOption.get
        contact.name shouldBe sampleContactName
        contact.email shouldBe sampleContactEmail
      }
    }

    "return None invalid email" in {
      val email = "kxuwerjvkce@xutckid.com"
      val contactFuture = client.findContactByEmail(email)
      whenReady(contactFuture) { contactOption =>
        contactOption shouldBe None
      }
    }
  }

  "findContractById" should {
    "return results for valid id" in {
      val contractOptionFuture = client.findContractById(sampleContractId)
      whenReady(contractOptionFuture) { contractOption =>
        val contract = contractOption.get
        contract.id.get should startWith(sampleContractId)
        contract.opportunityId shouldBe "0068A000001sxanQAA"
        formatDateTime(contract.startDate) shouldBe "2015-09-02 00:00:00 UTC"
        formatDateTime(contract.endDate) shouldBe "2016-09-01 00:00:00 UTC"
        contract.isCurrent shouldBe true
        contract.isCurrentOrFuture shouldBe true
        contract.isExpired shouldBe false
      }
    }
  }

  "findExpiredContracts" should {
    "get some Contracts" in {
      val startDate = LocalDate.of(2015, 11, 5)
      val endDate = LocalDate.of(2015, 11, 8)
      val x = client.findExpiredContracts(startDate, endDate)
      whenReady(x) { contracts =>
        contracts.foreach(contract => logger.debug(s"Expired Contract: ${contract.id.get}: ${contract.startDate} - ${contract.endDate}"))
        contracts.size should be > 0
      }
    }
  }

  "findOpportunityById" should {
    "return results for valid id" in {
      val opportunityOptionFuture = client.findOpportunityById(sampleOpportunityId)
      whenReady(opportunityOptionFuture) { opportunityOption =>
        val opportunity = opportunityOption.get
        opportunity.id.get should startWith(sampleOpportunityId)
        opportunity.account.name shouldBe sampleAccount.name
        opportunity.name shouldBe "New PSS"
      }
    }
  }

  "findOpportunityByContractId" should {
    "return results for valid id" in {
      val opportunityOptionFuture = client.findOpportunityByContractId(sampleContractId)
      whenReady(opportunityOptionFuture) { opportunityOption =>
        val opportunity = opportunityOption.get
        opportunity.id.get should startWith(sampleOpportunityId)
        opportunity.account.name shouldBe sampleAccount.name
        opportunity.name shouldBe "New PSS"
      }
    }
  }

  //  "findRecentlyUpdatedClosedWonOpportunities" should {
  //    "return results if updated Opps" in {
  //      val sinceDateTime = java.time.ZonedDateTime.now().minusDays(7)
  //      val opportunitiesFuture = client.findRecentlyUpdatedClosedWonOpportunities(sinceDateTime)
  //      whenReady(opportunitiesFuture) { opportunities =>
  //        logger.debug(s"opportunities: $opportunities")
  //        opportunities.isEmpty shouldBe true
  //      }
  //    }
  //  }

  "findRecentlyExpiredOpportunities" should {
    "get some Opps" in {
      val x = client.findRecentlyExpiredOpportunities(90)
      whenReady(x) { opps =>
        opps.foreach(opp => logger.info(s"Expired subscription Opp: ${opp.account.name} - ${opp.name}"))
        opps.size should be > 0
      }
    }
  }

  "findCurrentOrFutureOpportunitiesByEmail" should {
    "return results for valid email attached to role" in {
      val opportunitiesFuture = client.findCurrentOrFutureOpportunitiesByEmail(sampleContactEmail)
      whenReady(opportunitiesFuture) { opportunities =>
        logger.debug(s"opportunities: $opportunities")
        val opportunity = opportunities.find(_.name == "New PSS").get
        opportunity.id.get should startWith(sampleOpportunityId)
        opportunity.contracts.size shouldBe 1
        opportunity.products.size shouldBe 2 // PSS + ConductR
        opportunity.products.head.family shouldBe ProductFamily.A
      }
    }
  }

  "findOpportunityContactRoles" should {
    "return results for valid Opportunity" in {
      val ocrsFuture = client.findOpportunityContactRoles(sampleOpportunityId, Role.BusinessUser)
      whenReady(ocrsFuture) { ocrs =>
        logger.debug(s"ocrs: $ocrs")
        ocrs.size should be >= 1
      }
    }
  }

}