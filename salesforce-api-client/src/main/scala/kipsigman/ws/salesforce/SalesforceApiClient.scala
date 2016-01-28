package kipsigman.ws.salesforce

import java.time._
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutionException

import javax.inject.{ Inject, Singleton }

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.slf4j.LoggerFactory

import dispatch._

import play.api.http.HttpVerbs
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import com.typesafe.config.Config
import OpportunityContactRole.Role
import kipsigman.ws.client._

case class SalesforceConfig(
  apiVersion: String,
  appId: String,
  appSecret: String,
  oauthHost: String,
  passtoken: String,
  user: String) {
  
  def this(config: Config) = this(
    config.getString("sfdc.apiVersion"),
    config.getString("sfdc.appId"),
    config.getString("sfdc.appSecret"),
    config.getString("sfdc.oauthHost"),
    config.getString("sfdc.passtoken"),
    config.getString("sfdc.user")
  )
  
  lazy val oauthTokenUrl: String = s"$oauthHost/services/oauth2/token"
  lazy val servicePath: String = s"/services/data/$apiVersion"
  def serviceUrl(instanceUrl: String): String = instanceUrl + servicePath
}

case class AccessInfo(accessToken: String, instanceUrl: String)
object AccessInfo {
  implicit val reads: Reads[AccessInfo] = (
    (JsPath \ "access_token").read[String] and
    (JsPath \ "instance_url").read[String]
  )(AccessInfo.apply _)
}

/**
 * Thrown when attempting to create a Contact with an email that belongs to an existing Contact.
 */
class ContactConflictException(msg: String) extends RuntimeException(msg)

/**
 * Client for interfacing with the Salesforce API.
 * Uses Salesforce Oauth 2 Username-Password flow:
 * https://help.salesforce.com/apex/HTViewHelpDoc?id=remoteaccess_oauth_username_password_flow.htm&language=en_US
 */
@Singleton
class SalesforceApiClient @Inject() (config: Config)(protected implicit val ec: ExecutionContext) extends RestWebServiceClient {
  
  private lazy val salesforceConfig = new SalesforceConfig(config)
  
  private def serviceUrl(implicit accessInfo: AccessInfo) = salesforceConfig.serviceUrl(accessInfo.instanceUrl)

  def refreshAccessInfo: Future[AccessInfo] = {
    val postData = Map(
      "grant_type" -> "password",
      "client_id" -> salesforceConfig.appId,
      "client_secret" -> salesforceConfig.appSecret,
      "username" -> salesforceConfig.user,
      "password" -> salesforceConfig.passtoken)

    val req = url(salesforceConfig.oauthTokenUrl).secure.POST << postData
    logger.debug(s"refreshAccessInfo: ${salesforceConfig.oauthTokenUrl} $postData")

    executeJson(req, HttpVerbs.POST).map(jsValue => jsValue.get.as[AccessInfo])
  }
  
  private[salesforce] def authorizeReq(req: Req)(implicit accessInfo: AccessInfo): Req = {
    req.addHeader("Authorization", s"Bearer ${accessInfo.accessToken}")
  }
  
  private[salesforce] def executeReq(baseReq: Req, method: String)(implicit accessInfo: AccessInfo): Future[Option[JsValue]] = {
    logger.debug(s"executeReq: accessInfo=$accessInfo")
    val req = prepareReq(baseReq, method, postProcess = authorizeReq)
    executeJson(req, method)
  }

  private[salesforce] def executeGet(baseReq: Req)(implicit accessInfo: AccessInfo): Future[Option[JsValue]] = {
    executeReq(baseReq, HttpVerbs.GET)
  }
  
  private[salesforce] def executeQuery[T](query: String)(implicit accessInfo: AccessInfo, rds: Reads[T]): Future[Seq[T]] = {
    logger.debug(s"executeQuery: $query")
    val qparams = Map("q" -> query)
    val baseReq = url(s"$serviceUrl/query") <<? qparams
    executeGet(baseReq) map {
      case Some(jsValue) => (jsValue \ "records").as[Seq[T]]
      case None => Seq()
    }
  }
  
  private[salesforce] def executeWrite(baseReq: Req, jsValue: JsValue, method: String)(implicit accessInfo: AccessInfo): Future[Option[JsValue]] = {
    val jsonString = Json.stringify(jsValue)
    val reqWithData = baseReq << jsonString
    executeReq(reqWithData, method)
  }
  
  private[salesforce] def executePatch(baseReq: Req, jsValue: JsValue)(implicit accessInfo: AccessInfo): Future[Option[JsValue]] = {
    executeWrite(baseReq, jsValue, HttpVerbs.PATCH)
  }
  
  private[salesforce] def executePost(baseReq: Req, jsValue: JsValue)(implicit accessInfo: AccessInfo): Future[Option[JsValue]] = {
    executeWrite(baseReq, jsValue, HttpVerbs.POST)
  }
  
  private[salesforce] def delete[T <: SalesforceEntity](id: String)(implicit accessInfo: AccessInfo, objectName: ObjectName[T]): Future[Boolean] = {
    val urlString = objectURL[T](Option(id))
    val baseReq = url(urlString)
    val method = HttpVerbs.DELETE
    executeReq(baseReq, method).map(jsValue => true) recover {
      case t: Throwable => false
    }
  }
  
  /**
   * Creates or updates entity with Salesforce, returns Id.
   */
  private[salesforce] def save[T <: SalesforceEntity](entity: T)(implicit accessInfo: AccessInfo, objectName: ObjectName[T], wrts: Writes[T]): Future[String] = {
    val entityUrl = objectURL[T](entity.id)
    val baseReq = url(entityUrl)
    val jsValue = Json.toJson(entity)
    logger.debug(s"save: url=$entityUrl, jsValue=$jsValue")
    entity.id match {
      case Some(id) => {
        // Perform update
        executePatch(baseReq, jsValue).map(jsValueOption => id)
      }
      case None => {
        // Perform create
        executePost(baseReq, jsValue).map(jsValueOption => (jsValueOption.get \ "id").as[String])
      }
    }
  }
  
  private[salesforce] def saveObjectFields[T <: SalesforceEntity](id: String, jsValue: JsValue)(implicit accessInfo: AccessInfo, objectName: ObjectName[T]): Future[String] = {
    val entityUrl = objectURL[T](Option(id))
    val baseReq = url(entityUrl)
    executePatch(baseReq, jsValue).map(jsValueOption => id)
  }
  
  private[salesforce] def saveObjectStringField[T <: SalesforceEntity](id: String, field: String, value: String)(implicit accessInfo: AccessInfo, objectName: ObjectName[T]): Future[String] = {
    saveObjectFields(id, Json.obj(field -> value))
  }
  
  private[salesforce] def saveObjectIntField[T <: SalesforceEntity](id: String, field: String, value: Int)(implicit accessInfo: AccessInfo, objectName: ObjectName[T]): Future[String] = {
    saveObjectFields(id, Json.obj(field -> value))
  }
  
  private[salesforce] def findById[T <: SalesforceEntity](id: String)(implicit accessInfo: AccessInfo, objectName: ObjectName[T], objectReads: Reads[T]): Future[Option[T]] = {
    val theUrl = objectURL[T](Option(id))
    val req = url(theUrl)
    executeGet(req).map(_.map(_.as[T]))
  }
  
  def findAccountById(id: String)(implicit accessInfo: AccessInfo): Future[Option[Account]] = {
    findById[Account](id)
  }

  def findAccountByEmail(email: String)(implicit accessInfo: AccessInfo): Future[Option[Account]] = {
    findContactByEmail(email).flatMap {
      case Some(contact) => {
        if (contact.account.id.isDefined)
          findAccountById(contact.account.id.get)
        else
          Future.successful(None)
      }
      case None => Future.successful(None)
    }
  }
  
  def findContactByEmail(email: String)(implicit accessInfo: AccessInfo): Future[Option[Contact]] = {
    val query = s"""${Contact.selectClause} WHERE Email = '$email' ORDER BY CreatedDate"""
    executeQuery[Contact](query).map(_.headOption)
  }

  def saveContact(contact: Contact)(implicit accessInfo: AccessInfo): Future[Contact] = {
    val writes = contact.id match {
      case Some(id) => Contact.updateWrites
      case None => Contact.createWrites
    }
    save(contact)(accessInfo, Contact.objectName, writes).map(id => contact.copy(id = Option(id)))
  }
  
  def updateContactCurrentSubscriber(contact: Contact, currentSubscriber: Boolean)(implicit accessInfo: AccessInfo): Future[Contact] = {
    val updatedContact = contact.copy(currentSubscriber = currentSubscriber)
    saveContact(contact)
  }
  
  def findContractById(id: String)(implicit accessInfo: AccessInfo): Future[Option[Contract]] = {
    // Doing a query instead of an object lookup to get the Account joined fields
    val query = s"""${Contract.selectClause} WHERE Id = '$id'"""
    executeQuery[Contract](query).map(_.headOption)
  }
  
  /**
   * Finds Contracts which expired between startDate & endDate.
   */
  private[salesforce] def findExpiredContracts(startDate: LocalDate, endDate: LocalDate)(implicit accessInfo: AccessInfo): Future[Seq[Contract]] = {
    val startDateStr = startDate.format(DateTimeFormatter.ISO_DATE)
    val endDateStr = endDate.format(DateTimeFormatter.ISO_DATE)
    logger.debug(s"findExpiredContracts($startDateStr, $endDateStr)")
    val query = s"""${Contract.selectClause}
      WHERE EndDate >= ${startDateStr} AND EndDate < ${endDateStr}
      ORDER BY EndDate
    """
    executeQuery[Contract](query)
  }
  
  private[salesforce] def findRecentlyExpiredContracts(lastNDays: Int)(implicit accessInfo: AccessInfo): Future[Seq[Contract]] = {
    val query = s"""${Contract.selectClause}
      WHERE EndDate = LAST_N_DAYS:$lastNDays
      ORDER BY EndDate
    """
    executeQuery[Contract](query)
  }

  def findOpportunityById(id: String)(implicit accessInfo: AccessInfo): Future[Option[Opportunity]] = {
    val query = s"""${Opportunity.selectClause} WHERE Id = '$id'"""
    executeQuery[Opportunity](query).map(_.headOption)
  }
  
  def findOpportunities(ids: Seq[String])(implicit accessInfo: AccessInfo): Future[Seq[Opportunity]] = {
    val opportunityIdsStr = ids.mkString("'", "','", "'")
    val query = s"""${Opportunity.selectClause}
      WHERE Id IN ($opportunityIdsStr)
      ORDER BY CloseDate
    """
    executeQuery[Opportunity](query)
  }
  
  def findOpportunityByContractId(contractId: String)(implicit accessInfo: AccessInfo): Future[Option[Opportunity]] = {
    val contractOptionFuture = findContractById(contractId)
    contractOptionFuture flatMap {
      case Some(contract) => {
        findOpportunityById(contract.opportunityId)
      }
      case None => Future.successful(None)
    }
  }
  
  private[salesforce] def findRecentlyUpdatedClosedWonOpportunities(sinceDateTime: ZonedDateTime)(implicit accessInfo: AccessInfo): Future[Seq[Opportunity]] = {
    val sinceStr = sinceDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    val query = s"""${Opportunity.selectClause}
      WHERE IsClosed = true and IsWon = true AND LastModifiedDate > $sinceStr
      ORDER BY LastModifiedDate
    """
    executeQuery[Opportunity](query)
  }
  
  def findExpiredOpportunities(startDate: LocalDate, endDate: LocalDate)(implicit accessInfo: AccessInfo): Future[Seq[Opportunity]] = {
    val opportunitiesFuture = findExpiredContracts(startDate, endDate).flatMap(contracts => {
      val opportunityIds = contracts.map(_.opportunityId)
      findOpportunities(opportunityIds)
    })

    opportunitiesFuture.map(_.filter(opp => opp.isClosedWon))
  }
  
  def findRecentlyExpiredOpportunities(lastNDays: Int)(implicit accessInfo: AccessInfo): Future[Seq[Opportunity]] = {
    val opportunitiesFuture = findRecentlyExpiredContracts(lastNDays).flatMap(contracts => {
      val opportunityIds = contracts.map(_.opportunityId)
      findOpportunities(opportunityIds)
    })

    opportunitiesFuture.map(_.filter(opp => opp.isClosedWon))
  }
  
  private[salesforce] def findOpportunitiesForAccount(accountId: String)(implicit accessInfo: AccessInfo): Future[Seq[Opportunity]] = {
    val query = s"""${Opportunity.selectClause}
      WHERE AccountId = '$accountId'
      ORDER BY CloseDate
    """
    executeQuery[Opportunity](query)
  }
  
  private[salesforce] def findClosedWonOpportunitiesByAccount(accountId: String)(implicit accessInfo: AccessInfo): Future[Seq[Opportunity]] = {
    val query = s"""${Opportunity.selectClause}
      WHERE IsClosed = true and IsWon = true AND Account.Id = '$accountId'
      ORDER BY CloseDate
    """
    executeQuery[Opportunity](query)
  }
  
  /**
   * Finds Opportunities for a given contact email that are Closed Won and have a Subscription product
   */
  private[salesforce] def findOpportunitiesByEmail(email: String)(implicit accessInfo: AccessInfo): Future[Seq[Opportunity]] = {
    val opportunitiesFuture = findOpportunityContactRolesByEmail(email).flatMap(opportunityContactRoles => {
      val opportunityIds = opportunityContactRoles.map(_.opportunityId)
      findOpportunities(opportunityIds)
    })

    opportunitiesFuture.map(_.filter(opp => opp.isClosedWon))
  }
  
  def findCurrentOpportunitiesByEmail(email: String)(implicit accessInfo: AccessInfo): Future[Seq[Opportunity]] = {
    findOpportunitiesByEmail(email).map(_.filter(_.isCurrent))
  }
  
  def findCurrentOrFutureOpportunitiesByAccount(accountId: String)(implicit accessInfo: AccessInfo): Future[Seq[Opportunity]] = {
    findClosedWonOpportunitiesByAccount(accountId).map(_.filter(_.isCurrentOrFuture))
  }

  def findCurrentOrFutureOpportunitiesByEmail(email: String)(implicit accessInfo: AccessInfo): Future[Seq[Opportunity]] = {
    findOpportunitiesByEmail(email).map(_.filter(_.isCurrentOrFuture))
  }
  
  /**
   * TODO: Use another field than Sherpa timestamp
   * Resets the Sherpa Timestamp on an Opportunity which will cause a save and
   * trigger Sherpa to process the Opportunity & Named Contacts.
   * Note: Sherpa Timestamp isn't read by anything, it's just a field to update.
   */
  private[salesforce] def touchOpportunity(opportunityId: String)(implicit accessInfo: AccessInfo): Unit = {
    val entityUrl = objectURL[Opportunity](Option(opportunityId))
    val now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    val jsValue = Json.obj("Sherpa_Timestamp__c" -> now)
    val baseReq = url(entityUrl)
    executePatch(baseReq, jsValue)
  }

  def findOpportunityContactRoleById(id: String)(implicit accessInfo: AccessInfo): Future[Option[OpportunityContactRole]] = {
    val query = s"${OpportunityContactRole.selectClause} WHERE Id = '$id'"
    executeQuery[OpportunityContactRole](query).map(_.headOption)
  }

  def findOpportunityContactRole(opportunityId: String, role: Role, email: String)(implicit accessInfo: AccessInfo): Future[Option[OpportunityContactRole]] = {
    val query = OpportunityContactRole.selectClause +
      s" WHERE OpportunityId='$opportunityId' AND Role='${role.name}' AND Contact.Email='$email'"
    executeQuery[OpportunityContactRole](query).map(_.headOption)
  }

  def findOpportunityContactRoles(opportunityId: String, role: Role)(implicit accessInfo: AccessInfo): Future[Seq[OpportunityContactRole]] = {
    val query = s"""${OpportunityContactRole.selectClause} WHERE OpportunityId = '$opportunityId' AND Role='${role.name}' ORDER BY Contact.LastName, Contact.FirstName"""
    executeQuery[OpportunityContactRole](query)
  }

  private[salesforce] def findOpportunityContactRolesByEmail(email: String)(implicit accessInfo: AccessInfo): Future[Seq[OpportunityContactRole]] = {
    val query = s"""${OpportunityContactRole.selectClause} WHERE Contact.Email = '$email'"""
    executeQuery[OpportunityContactRole](query)
  }

  private[salesforce] def saveOpportunityContactRole(ocr: OpportunityContactRole)(implicit accessInfo: AccessInfo): Future[OpportunityContactRole] = {
    ocr.id match {
      case Some(id) => {
        save(ocr)(accessInfo, OpportunityContactRole.objectName, OpportunityContactRole.updateWrites).map(id => ocr)
      }
      case None => {
        save(ocr)(accessInfo, OpportunityContactRole.objectName, OpportunityContactRole.createWrites).map(id => ocr.copy(id = Option(id)))
      }
    }
  }

  def deleteOpportunityContactRole(id: String)(implicit accessInfo: AccessInfo): Future[Boolean] = {
    findOpportunityContactRoleById(id) flatMap {
      case Some(ocr) => {
        val successFuture = delete[OpportunityContactRole](id)
        successFuture.foreach(if (_) {
          touchOpportunity(ocr.opportunityId)
          updateContactCurrentSubscriber(ocr.contact, false)
        })
        successFuture
      }
      case None => Future.successful(false)
    }
  }

  def findProductById(id: String)(implicit accessInfo: AccessInfo): Future[Option[Product]] = {
    findById[Product](id)
  }

  private[salesforce] def objectURL[T <: SalesforceEntity](id: Option[String])(implicit accessInfo: AccessInfo, objectName: ObjectName[T]) = {
    s"${serviceUrl}/sobjects/${objectName.raw}" + id.map(idVal => s"/$idVal").getOrElse("")
  }
  
}