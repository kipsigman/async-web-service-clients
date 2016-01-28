package kipsigman.ws.salesforce

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import com.typesafe.config._

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.Matchers
import org.scalatest.time._
import org.scalatest.WordSpec

import org.slf4j.LoggerFactory

trait SalesforceApiClientIntegrationSpec extends WordSpec with Matchers with ScalaFutures {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  implicit override def patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  import java.util.Date
  import java.text.SimpleDateFormat
  @deprecated("Replace with java.time")
  protected def formatDateTime(date: Date): String = {
    val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
    df.format(date)
  }

  // client for test
  private val config = ConfigFactory.load()
  protected val client: SalesforceApiClient = new SalesforceApiClient(config)

  // Temp AccessInfo for client requests
  logger.info("Requesting AccessInfo")
  protected implicit val accessInfo = Await.result(client.refreshAccessInfo, 5 seconds)
  logger.info(s"accessInfo=$accessInfo")

  // Sample Data
  protected val sampleAccount = Account(Option("0018A000002KQ7bQAG"), "005C00000079DQSIA2", "Kip's Karma Kiosks", Option(Account.AccountType.Customer))
  protected val sampleContactEmail = "johnny.utah@fbi.gov"
  protected val sampleContactName = "Johnny Utah"
  protected val sampleContractId = "8008A0000008znAQAQ"
  protected val sampleOpportunityId = "0068A000001sxan"

}