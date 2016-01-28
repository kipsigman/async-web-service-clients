package kipsigman.ws.salesforce

import java.time.LocalDate

import scala.concurrent.Future

import OpportunityContactRole.Role

/**
 * Interface for Salesforce access.
 */
trait SalesforceService {

  def findAccountById(id: String): Future[Option[Account]]

  def findAccountByEmail(email: String): Future[Option[Account]]

  def findContactByEmail(email: String): Future[Option[Contact]]

  def saveContact(contact: Contact): Future[Contact]

  def updateContactCurrentSubscriber(contact: Contact, currentSubscriber: Boolean): Future[Contact]

  def findContractById(id: String): Future[Option[Contract]]

  def findOpportunityById(id: String): Future[Option[Opportunity]]

  def findOpportunities(ids: Seq[String]): Future[Seq[Opportunity]]

  def findOpportunityByContractId(contractId: String): Future[Option[Opportunity]]

  def findExpiredOpportunities(startDate: LocalDate, endDate: LocalDate): Future[Seq[Opportunity]]

  def findRecentlyExpiredOpportunities(lastNDays: Int): Future[Seq[Opportunity]]

  def findCurrentOpportunitiesByEmail(email: String): Future[Seq[Opportunity]]

  def findCurrentOrFutureOpportunitiesByAccount(accountId: String): Future[Seq[Opportunity]]

  def findCurrentOrFutureOpportunitiesByEmail(email: String): Future[Seq[Opportunity]]

  def findOpportunityContactRoleById(id: String): Future[Option[OpportunityContactRole]]

  def findOpportunityContactRole(opportunityId: String, role: Role, email: String): Future[Option[OpportunityContactRole]]

  def findOpportunityContactRoles(opportunityId: String, role: Role): Future[Seq[OpportunityContactRole]]

  def deleteOpportunityContactRole(id: String): Future[Boolean]

  def findProductById(id: String): Future[Option[Product]]
}
