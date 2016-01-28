package kipsigman.ws.salesforce

import scala.io.Source

import play.api.libs.json._

trait SampleData {

  protected val sampleAccountJson = Json.parse(Source.fromURL(getClass.getResource("/account.json")).getLines().mkString)
  protected val sampleAccount: Account = {
    sampleAccountJson.as[Account]
  }

  protected val sampleContactJson = Json.parse(Source.fromURL(getClass.getResource("/contact.json")).getLines().mkString)
  protected val sampleContact: Contact = {
    sampleContactJson.as[Contact]
  }

  protected val sampleContractJson = Json.parse(Source.fromURL(getClass.getResource("/contract.json")).getLines().mkString)
  protected val sampleContract: Contract = {
    sampleContractJson.as[Contract]
  }

  protected val sampleProductJson = Json.parse(Source.fromURL(getClass.getResource("/product2.json")).getLines().mkString)
  protected val sampleProduct: Product = {
    sampleProductJson.as[Product]
  }

  protected val sampleOpportunityJson = Json.parse(Source.fromURL(getClass.getResource("/opportunity.json")).getLines().mkString)
  protected val sampleOpportunity: Opportunity = {
    sampleOpportunityJson.as[Opportunity]
  }

  protected val sampleOpportunityContactRoleJson = Json.parse(Source.fromURL(getClass.getResource("/opportunityContactRole.json")).getLines().mkString)
  protected val sampleOpportunityContactRole: OpportunityContactRole = {
    sampleOpportunityContactRoleJson.as[OpportunityContactRole]
  }

  protected val sampleOpportunityLineItemJson = Json.parse(Source.fromURL(getClass.getResource("/opportunityLineItem.json")).getLines().mkString)
  protected val sampleOpportunityLineItem: OpportunityLineItem = {
    sampleOpportunityLineItemJson.as[OpportunityLineItem]
  }

  private[salesforce] def parseDate(input: String): java.util.Date = {
    val df = new java.text.SimpleDateFormat("yyyy-MM-dd")
    df.setLenient(false)
    df.parse(input)
  }

}