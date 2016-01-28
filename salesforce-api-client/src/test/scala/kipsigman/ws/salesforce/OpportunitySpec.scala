package kipsigman.ws.salesforce

import java.time._
import java.time.format.DateTimeFormatter
import java.util.Calendar

import OpportunityContactRole.Role
import Product._

import org.scalatest.Matchers
import org.scalatest.WordSpec

import play.api.libs.json._

class OpportunitySpec extends WordSpec with Matchers with SampleData {

  private val futureContract = {
    val startCal = Calendar.getInstance()
    startCal.add(Calendar.DAY_OF_YEAR, 2)
    val endCal = Calendar.getInstance()
    endCal.add(Calendar.DAY_OF_YEAR, 367)

    sampleContract.copy(startDate = startCal.getTime, endDate = endCal.getTime)
  }

  private val expiredContract = {
    val startCal = Calendar.getInstance()
    startCal.add(Calendar.DAY_OF_YEAR, -367)
    val endCal = Calendar.getInstance()
    endCal.add(Calendar.DAY_OF_YEAR, -2)

    sampleContract.copy(startDate = startCal.getTime, endDate = endCal.getTime)
  }

  private def replaceProduct(oli: OpportunityLineItem, product: Product): OpportunityLineItem = {
    val newPricebookEntry = oli.pricebookEntry.copy(product = product)
    oli.copy(pricebookEntry = newPricebookEntry)
  }

  private def replaceProduct(opp: Opportunity, product: Product): Opportunity = {
    val newOli = replaceProduct(opp.opportunityLineItems.head, product)
    opp.copy(opportunityLineItems = Seq(newOli))
  }

  "JSON deserialization" should {
    "read valid JSON" in {
      sampleOpportunityJson.validate[Opportunity] match {
        case s: JsSuccess[Opportunity] => {
          val opportunity = s.get
          opportunity shouldBe sampleOpportunity
          opportunity.account shouldBe sampleAccount
          opportunity.closeDate.format(DateTimeFormatter.ISO_DATE) shouldBe "2015-09-02"
          opportunity.name shouldBe "New PSS"
          opportunity.stage shouldBe Opportunity.Stage.ClosedWon
          opportunity.contracts.get.head shouldBe sampleContract
        }
        case e: JsError => {
          fail(s"error=$e")
        }
      }
    }
  }

  "isClosedWon" should {
    "return true for stage = ClosedWon" in {
      sampleOpportunity.isClosedWon shouldBe true
    }
    "return false for other stages" in {
      sampleOpportunity.copy(stage = Opportunity.Stage.ClosedLost).isClosedWon shouldBe false
      sampleOpportunity.copy(stage = Opportunity.Stage.NeedsAnalysis).isClosedWon shouldBe false
      sampleOpportunity.copy(stage = Opportunity.Stage.ValueProposition).isClosedWon shouldBe false
    }
  }

  "isCurrent" should {
    "return true for a contract that is current" in {
      sampleOpportunity.isCurrent shouldBe true
      sampleOpportunity.isCurrentOrFuture shouldBe true
    }
    "return false for Contract outside of now" in {
      sampleOpportunity.copy(contracts = Option(Seq(futureContract))).isCurrent shouldBe false
      sampleOpportunity.copy(contracts = Option(Seq(expiredContract))).isCurrent shouldBe false
    }
  }

  "isCurrentOrFuture" should {
    "return true for a contract that is current" in {
      sampleOpportunity.isCurrentOrFuture shouldBe true
    }
    "return true for a contract that is future" in {
      sampleOpportunity.copy(contracts = Option(Seq(futureContract))).isCurrentOrFuture shouldBe true
    }
    "return false for an expired Contract" in {
      sampleOpportunity.copy(contracts = Option(Seq(expiredContract))).isCurrentOrFuture shouldBe false
    }
  }

}