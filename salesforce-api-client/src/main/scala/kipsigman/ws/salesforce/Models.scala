package kipsigman.ws.salesforce

import java.time._

import java.util.Date

import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._

trait SalesforceEntity {
  def id: Option[String]
}

class ObjectName[T <: SalesforceEntity](val raw: String) {
  override def toString = raw
}

trait ApiMetadata[T <: SalesforceEntity] {
  implicit val objectName: ObjectName[T]

  implicit val reads: Reads[T]

  val selectFields: Seq[String]

  final def selectFieldsStr: String = selectFields.mkString(", ")
  final def objectPrefixedSelectFields: Seq[String] = selectFields.map(field => s"${objectName.raw}.$field")

  /**
   * Override for complex objects with sub-selects
   */
  def selectClause: String = s"SELECT $selectFieldsStr FROM $objectName"
}

case class Account(id: Option[String], ownerId: String, name: String, accountType: Option[Account.AccountType]) extends SalesforceEntity

object Account extends ApiMetadata[Account] {
  override implicit val objectName = new ObjectName[Account]("Account")

  override implicit val reads: Reads[Account] = (
    (JsPath \ "Id").readNullable[String] and
    (JsPath \ "OwnerId").read[String] and
    (JsPath \ "Name").read[String] and
    (JsPath \ "Type").readNullable[AccountType](AccountType.reads)
  )(Account.apply _)
  
  val updateWrites: Writes[Account] = (
    (JsPath \ "Name").write[String] and
    (JsPath \ "Type").writeNullable[String]
  )((account: Account) => (account.name, account.accountType.map(_.name)))

  override val selectFields = Seq("Id", "OwnerId", "Name", "Type")
  
  sealed abstract class AccountType(val name: String) {
    override def toString: String = name
  }
  
  object AccountType {
    case object Analyst extends AccountType("Analyst")
    case object Competitor extends AccountType("Competitor")
    case object Customer extends AccountType("Customer")
    case object Integrator extends AccountType("Integrator")
    case object Investor extends AccountType("Investor")
    case object Partner extends AccountType("Partner")
    case object Press extends AccountType("Press")
    case object Prospect extends AccountType("Prospect")
    case object Reseller extends AccountType("Reseller")
    case object Other extends AccountType("Other")
    
    case class Unknown(override val name: String) extends AccountType(name)
    
    val all: Set[AccountType] = Set(
      Analyst, Competitor, Customer, Integrator,
      Investor, Partner, Press, Prospect,
      Reseller, Other
    )
    
    def apply(name: String): AccountType = {
      val stage = all.find(s => s.name == name)
      stage.getOrElse(Unknown(name))
    }
  
    implicit val reads = new Reads[AccountType] {
      def reads(json: JsValue) = json match {
        case JsString(s) => JsSuccess(AccountType(s))
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
      }
    }
  }
}

case class Contact(
    id: Option[String],
    account: Account,
    firstName: String,
    lastName: String,
    email: String,
    phone: Option[String],
    currentSubscriber: Boolean = false) extends SalesforceEntity {

  def this(account: Account, contactData: ContactData) = {
    this(None, account, contactData.firstName, contactData.lastName, contactData.email, contactData.phone)
  }

  def update(contactData: ContactData): Contact =
    this.copy(firstName = contactData.firstName, lastName = contactData.lastName, email = contactData.email, phone = contactData.phone)

  val name = s"$firstName $lastName"
}

object Contact extends ApiMetadata[Contact] {

  override implicit val objectName = new ObjectName[Contact]("Contact")

  implicit val reads: Reads[Contact] = (
    (JsPath \ "Id").readNullable[String] and
    (JsPath \ "Account").read[Account] and
    (JsPath \ "FirstName").read[String] and
    (JsPath \ "LastName").read[String] and
    (JsPath \ "Email").read[String] and
    (JsPath \ "Phone").readNullable[String] and
    (JsPath \ "Current_Subscriber__c").read[Boolean]
  )(Contact.apply _)

  val createWrites: Writes[Contact] = (
    (JsPath \ "AccountId").write[String] and
    (JsPath \ "OwnerId").write[String] and
    (JsPath \ "FirstName").write[String] and
    (JsPath \ "LastName").write[String] and
    (JsPath \ "Email").write[String] and
    (JsPath \ "Phone").writeNullable[String] and
    (JsPath \ "Current_Subscriber__c").write[Boolean]
  )((contact: Contact) => (contact.account.id.get, contact.account.ownerId, contact.firstName, contact.lastName, contact.email, contact.phone, contact.currentSubscriber))

  val updateWrites: Writes[Contact] = (
    (JsPath \ "FirstName").write[String] and
    (JsPath \ "LastName").write[String] and
    (JsPath \ "Email").write[String] and
    (JsPath \ "Phone").writeNullable[String] and
    (JsPath \ "Current_Subscriber__c").write[Boolean]
  )((contact: Contact) => (contact.firstName, contact.lastName, contact.email, contact.phone, contact.currentSubscriber))

  override val selectFields = Seq("Id") ++ Account.objectPrefixedSelectFields ++ Seq("FirstName", "LastName", "Email", "Phone", "Current_Subscriber__c")
}

case class ContactData(
  firstName: String = "",
  lastName: String = "",
  email: String = "",
  phone: Option[String] = None)

case class Contract(
    id: Option[String],
    opportunityId: String,
    startDate: Date,
    endDate: Date) extends SalesforceEntity {

  def isCurrentOrFuture = endDate.after(new Date())
  
  def isCurrent = {
    val now = new Date()
    startDate.before(now) && endDate.after(now)
  }
  
  def isExpired = !isCurrentOrFuture
}

object Contract extends ApiMetadata[Contract] {
  override implicit val objectName = new ObjectName[Contract]("Contract")

  // Note: Date strings are probably in PST from Salesforce, but system is UTC. So there may be a 7-8 hour discrepancy when using default Reads.
  // This can be fixed by creating a custom DateReads that converts the time zone.
  implicit val reads: Reads[Contract] = (
    (JsPath \ "Id").readNullable[String] and
    (JsPath \ "Opportunity__c").read[String] and
    (JsPath \ "StartDate").read[Date] and
    (JsPath \ "EndDate").read[Date]
  )(Contract.apply _)

  override val selectFields = Seq("Id", "Opportunity__c", "StartDate", "EndDate")
}

case class Opportunity(
    id: Option[String],
    account: Account,
    closeDate: LocalDate,
    name: String,
    stage: Opportunity.Stage,
    contracts: Option[Seq[Contract]],
    opportunityLineItems: Seq[OpportunityLineItem]) extends SalesforceEntity {
  
  lazy val contract: Option[Contract] = contracts.flatMap(_.headOption)
  
  lazy val isInSalesPipeline: Boolean = {
    import Opportunity.Stage._
    stage match {
      case ClosedLost => false
      case Unknown(name) => false
      case _ => true
    }
  }
  
  lazy val isClosedWon: Boolean = stage == Opportunity.Stage.ClosedWon
  
  def isCurrent: Boolean = {
    isClosedWon && (contract match {
      case Some(contract) => contract.isCurrent
      case None => false
    })
  }
  
  def isCurrentOrFuture: Boolean = {
    isClosedWon && (contract match {
      case Some(contract) => contract.isCurrentOrFuture
      case None => false
    })
  }

  def isExpired: Boolean = {
    isClosedWon && (contract match {
      case Some(contract) => contract.isExpired
      case None => true
    })
  }
  
  lazy val products: Set[Product] = opportunityLineItems.map(_.pricebookEntry.product).toSet
  
  lazy val productsDisplayText = opportunityLineItems.map(oli => oli.productDisplayText).mkString(", ")
}

object Opportunity extends ApiMetadata[Opportunity] {
  implicit val objectName = new ObjectName[Opportunity]("Opportunity")

  implicit val reads: Reads[Opportunity] = (
    (JsPath \ "Id").readNullable[String] and
    (JsPath \ "Account").read[Account] and
    (JsPath \ "CloseDate").read[LocalDate] and
    (JsPath \ "Name").read[String] and
    (JsPath \ "StageName").read[Stage](Stage.reads) and
    (JsPath \ "Contracts__r" \ "records").readNullable[Seq[Contract]] and
    (JsPath \ "OpportunityLineItems" \ "records").read[Seq[OpportunityLineItem]]
  )(Opportunity.apply _)

  // Only define primary select fields, child collections defined in SELECT statement
  override val selectFields = 
    Seq("Id") ++
    Account.objectPrefixedSelectFields ++
    Seq("CloseDate", "Name", "StageName")
    
  override def selectClause = s"SELECT ${selectFieldsStr}" +
    s", (SELECT ${Contract.selectFieldsStr} FROM Contracts__r)" +
    s", (SELECT ${OpportunityLineItem.selectFieldsStr} FROM OpportunityLineItems)" +
    s" FROM ${objectName}"
    
  sealed abstract class Stage(val name: String) {
    override def toString: String = name
  }
  
  object Stage {
    // Open
    case object Prospecting extends Stage("Prospecting")
    case object Qualification extends Stage("Qualification")
    case object NeedsAnalysis extends Stage("Needs Analysis")
    case object ValueProposition extends Stage("Value Proposition")
    case object IdDecisionMakers extends Stage("ID Decision Makers")
    case object PerceptionAnalysis extends Stage("Perception Analysis")
    case object ProposalPriceQuote extends Stage("Proposal/Price Quote")
    case object NegotiationReview extends Stage("Negotiation/Review")
    
    // Closed
    case object ClosedWon extends Stage("Closed Won")
    case object ClosedLost extends Stage("Closed Lost")
    
    final case class Unknown(override val name: String) extends Stage(name)
    
    val stages: Set[Stage] = Set(
      Prospecting, Qualification, NeedsAnalysis, ValueProposition,
      IdDecisionMakers, PerceptionAnalysis, ProposalPriceQuote, NegotiationReview,
      ClosedLost, ClosedWon
    )
    
    def apply(name: String): Stage = {
      val stage = stages.find(s => s.name == name)
      stage.getOrElse(Unknown(name))
    }
  
    implicit val reads = new Reads[Stage] {
      def reads(json: JsValue) = json match {
        case JsString(s) => JsSuccess(Stage(s))
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
      }
    }
  }
}

case class OpportunityContactRole(
  id: Option[String],
  opportunityId: String,
  role: OpportunityContactRole.Role,
  isPrimary: Boolean,
  contact: Contact) extends SalesforceEntity

object OpportunityContactRole extends ApiMetadata[OpportunityContactRole] {
  implicit val objectName = new ObjectName[OpportunityContactRole]("OpportunityContactRole")

  implicit val reads: Reads[OpportunityContactRole] = (
    (JsPath \ "Id").readNullable[String] and
    (JsPath \ "OpportunityId").read[String] and
    (JsPath \ "Role").read[Role](Role.reads) and
    (JsPath \ "IsPrimary").read[Boolean] and
    (JsPath \ "Contact").read[Contact]
  )(OpportunityContactRole.apply _)

  val createWrites: Writes[OpportunityContactRole] = (
    (JsPath \ "OpportunityId").write[String] and
    (JsPath \ "Role").write[String] and
    (JsPath \ "IsPrimary").write[Boolean] and
    (JsPath \ "ContactId").write[String]
  )((ocr: OpportunityContactRole) => (ocr.opportunityId, ocr.role.name, ocr.isPrimary, ocr.contact.id.get))

  val updateWrites: Writes[OpportunityContactRole] = (
    (JsPath \ "Role").write[String] and
    (JsPath \ "IsPrimary").write[Boolean] and
    (JsPath \ "ContactId").write[String]
  )((ocr: OpportunityContactRole) => (ocr.role.name, ocr.isPrimary, ocr.contact.id.get))

  override val selectFields = Seq("Id", "OpportunityId", "Role", "IsPrimary") ++ Contact.objectPrefixedSelectFields
  
  sealed abstract class Role(val name: String) {
    override def toString: String = name
  }
  
  object Role {
    case object BusinessUser extends Role("Business User")
    case object DecisionMaker extends Role("Decision Maker")
    case object EconomicBuyer extends Role("Economic Buyer")
    case object EconomicDecisionMaker extends Role("Economic Decision Maker")
    case object Evaluator extends Role("Evaluator")
    case object ExecutiveSponsor extends Role("Executive Sponsor")
    case object Influencer extends Role("Influencer")
    case object TechnicalBuyer extends Role("Technical Buyer")
    case object Other extends Role("Other")
    
    case class Unknown(override val name: String) extends Role(name)
  
    val all: Set[Role] = Set(
      BusinessUser,
      DecisionMaker,
      EconomicBuyer,
      EconomicDecisionMaker,
      Evaluator,
      ExecutiveSponsor,
      Influencer,
      TechnicalBuyer,
      Other)
  
    def apply(name: String): Role = {
      val role = all.find(role => role.name == name)
      role.getOrElse(Unknown(name))
    }
  
    implicit val reads = new Reads[Role] {
      def reads(json: JsValue) = json match {
        case JsString(s) => JsSuccess(Role(s))
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
      }
    }
  }
}

case class OpportunityLineItem(id: Option[String], quantity: Int, pricebookEntry: PricebookEntry) extends SalesforceEntity {
  val productDisplayText = quantity match {
    case 1 => pricebookEntry.product.name
    case q => s"${pricebookEntry.product.name} ($q)"
  }
}

object OpportunityLineItem extends ApiMetadata[OpportunityLineItem] {
  implicit val objectName = new ObjectName[OpportunityLineItem]("OpportunityLineItem")

  implicit val reads: Reads[OpportunityLineItem] = (
    (JsPath \ "Id").readNullable[String] and
    (JsPath \ "Quantity").read[Int] and
    (JsPath \ "PricebookEntry").read[PricebookEntry]
  )(OpportunityLineItem.apply _)

  override val selectFields = Seq("Id", "Quantity") ++ PricebookEntry.objectPrefixedSelectFields
}

case class PricebookEntry(id: Option[String], product: Product) extends SalesforceEntity

object PricebookEntry extends ApiMetadata[PricebookEntry] {
  implicit val objectName = new ObjectName[PricebookEntry]("PricebookEntry")

  implicit val reads: Reads[PricebookEntry] = (
    (JsPath \ "Id").readNullable[String] and
    (JsPath \ "Product2").read[Product]
  )(PricebookEntry.apply _)

  override val selectFields = Seq("Id") ++ Product.objectPrefixedSelectFields
}

case class Product(
    id: Option[String],
    name: String,
    family: Product.ProductFamily) extends SalesforceEntity

object Product extends ApiMetadata[Product] {
  implicit val objectName = new ObjectName[Product]("Product2")

  implicit val reads: Reads[Product] = (
    (JsPath \ "Id").readNullable[String] and
    (JsPath \ "Name").read[String] and
    (JsPath \ "Family").read[ProductFamily](ProductFamily.reads)
  )(Product.apply _)

  override val selectFields = Seq("Id", "Name", "Family")
  
  sealed abstract class ProductFamily(val name: String) {
    override def toString: String = name
  }
  
  object ProductFamily {
    case object A extends ProductFamily("")
    case object B extends ProductFamily("B")
    final case class Other(override val name: String) extends ProductFamily(name)
  
    val all: Set[ProductFamily] = Set(A, B)
  
    def apply(name: String): ProductFamily = {
      val productFamilyOption = all.find(productFamily => productFamily.name == name)
      productFamilyOption.getOrElse(Other(name))
    }
  
    implicit val reads = new Reads[ProductFamily] {
      def reads(json: JsValue) = json match {
        case JsString(s) => JsSuccess(ProductFamily(s))
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
      }
    }
  }
   
}