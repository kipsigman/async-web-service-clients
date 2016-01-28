package kipsigman.ws.salesforce.actor

import java.time.LocalDate
import javax.inject.{ Inject, Singleton }

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout

import com.typesafe.config.Config
import kipsigman.ws.salesforce._
import OpportunityContactRole.Role
import SalesforceActor.ApiRequest

/**
 * Service for interfacing indirectly with the Salesforce API via an Actor.
 */
@Singleton
class SalesforceActorService @Inject() (actorSystem: ActorSystem, config: Config) extends SalesforceService {

  import actorSystem.dispatcher
  implicit val timeout = Timeout(15 seconds)

  val actorRef = SalesforceActor(actorSystem, config)

  override def findAccountById(id: String): Future[Option[Account]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findAccountById(id)(accessInfo))
    (actorRef ? req).mapTo[Option[Account]]
  }

  override def findAccountByEmail(email: String): Future[Option[Account]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findAccountByEmail(email)(accessInfo))
    (actorRef ? req).mapTo[Option[Account]]
  }

  override def findContactByEmail(email: String): Future[Option[Contact]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findContactByEmail(email)(accessInfo))
    (actorRef ? req).mapTo[Option[Contact]]
  }

  override def saveContact(contact: Contact): Future[Contact] = {
    val req = ApiRequest((repository, accessInfo) => repository.saveContact(contact)(accessInfo))
    (actorRef ? req).mapTo[Contact]
  }

  override def updateContactCurrentSubscriber(contact: Contact, currentSubscriber: Boolean): Future[Contact] = {
    val req = ApiRequest((repository, accessInfo) => repository.updateContactCurrentSubscriber(contact, currentSubscriber)(accessInfo))
    (actorRef ? req).mapTo[Contact]
  }

  override def findContractById(id: String): Future[Option[Contract]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findContractById(id)(accessInfo))
    (actorRef ? req).mapTo[Option[Contract]]
  }

  override def findOpportunityById(id: String): Future[Option[Opportunity]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findOpportunityById(id)(accessInfo))
    (actorRef ? req).mapTo[Option[Opportunity]]
  }

  override def findOpportunities(ids: Seq[String]): Future[Seq[Opportunity]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findOpportunities(ids)(accessInfo))
    (actorRef ? req).mapTo[Seq[Opportunity]]
  }

  override def findOpportunityByContractId(contractId: String): Future[Option[Opportunity]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findOpportunityByContractId(contractId)(accessInfo))
    (actorRef ? req).mapTo[Option[Opportunity]]
  }

  override def findExpiredOpportunities(startDate: LocalDate, endDate: LocalDate): Future[Seq[Opportunity]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findExpiredOpportunities(startDate, endDate)(accessInfo))
    (actorRef ? req).mapTo[Seq[Opportunity]]
  }

  override def findRecentlyExpiredOpportunities(lastNDays: Int): Future[Seq[Opportunity]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findRecentlyExpiredOpportunities(lastNDays)(accessInfo))
    (actorRef ? req).mapTo[Seq[Opportunity]]
  }

  override def findCurrentOpportunitiesByEmail(email: String): Future[Seq[Opportunity]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findCurrentOpportunitiesByEmail(email)(accessInfo))
    (actorRef ? req).mapTo[Seq[Opportunity]]
  }

  override def findCurrentOrFutureOpportunitiesByAccount(accountId: String): Future[Seq[Opportunity]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findCurrentOrFutureOpportunitiesByAccount(accountId)(accessInfo))
    (actorRef ? req).mapTo[Seq[Opportunity]]
  }

  override def findCurrentOrFutureOpportunitiesByEmail(email: String): Future[Seq[Opportunity]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findCurrentOrFutureOpportunitiesByEmail(email)(accessInfo))
    (actorRef ? req).mapTo[Seq[Opportunity]]
  }

  override def findOpportunityContactRoleById(id: String): Future[Option[OpportunityContactRole]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findOpportunityContactRoleById(id)(accessInfo))
    (actorRef ? req).mapTo[Option[OpportunityContactRole]]
  }

  override def findOpportunityContactRole(opportunityId: String, role: Role, email: String): Future[Option[OpportunityContactRole]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findOpportunityContactRole(opportunityId, role, email)(accessInfo))
    (actorRef ? req).mapTo[Option[OpportunityContactRole]]
  }

  override def findOpportunityContactRoles(opportunityId: String, role: Role): Future[Seq[OpportunityContactRole]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findOpportunityContactRoles(opportunityId, role)(accessInfo))
    (actorRef ? req).mapTo[Seq[OpportunityContactRole]]
  }

  override def deleteOpportunityContactRole(id: String): Future[Boolean] = {
    val req = ApiRequest((repository, accessInfo) => repository.deleteOpportunityContactRole(id)(accessInfo))
    (actorRef ? req).mapTo[Boolean]
  }

  override def findProductById(id: String): Future[Option[Product]] = {
    val req = ApiRequest((repository, accessInfo) => repository.findProductById(id)(accessInfo))
    (actorRef ? req).mapTo[Option[Product]]
  }

}