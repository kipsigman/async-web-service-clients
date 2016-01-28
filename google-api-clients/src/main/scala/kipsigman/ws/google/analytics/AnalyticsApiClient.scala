package kipsigman.ws.google.analytics

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.analytics.Analytics
import com.google.api.services.analytics.AnalyticsScopes
import com.google.api.services.analytics.model.AccountSummaries;
import com.google.api.services.analytics.model.AccountSummary;
import com.google.api.services.analytics.model.Accounts;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.ProfileSummary;
import com.google.api.services.analytics.model.Profiles;
import com.google.api.services.analytics.model.WebPropertySummary;
import com.google.api.services.analytics.model.Webproperties;

import com.typesafe.config.Config

import kipsigman.ws.google.GoogleApiClient

/**
 * Google Analytics API client.
 * This is a work in progress, only minimal functionality available. 
 */
@Singleton
class AnalyticsApiClient @Inject() (config: Config, p12File: File)(implicit ec: ExecutionContext) extends GoogleApiClient(config) {

  private val analytics: Analytics = initializeAnalytics
  
  /////////////////
  // Builder methods
  /////////////////
  private[analytics] def initializeAnalytics: Analytics = {
    // Initializes an authorized analytics service object.
    val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance();

    val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport();
    val credential: GoogleCredential = new GoogleCredential.Builder()
      .setTransport(httpTransport)
      .setJsonFactory(jsonFactory)
      .setServiceAccountId(serviceAccountEmail)
      .setServiceAccountPrivateKeyFromP12File(p12File)
      .setServiceAccountScopes(AnalyticsScopes.all())
      .build();

    // Construct the Analytics service object.
    new Analytics.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName).build();
  }
  
  private[analytics] def filter(dimension: Dimension, operator: FilterOperator, expression: String): Filters = {
    new Filters(s"${dimension.toString}${operator.underlying}$expression")
  }

  private[analytics] def filterMetric(metric: Metric, operator: FilterOperator, expression: String): Filters = {
    new Filters(s"${metric.toString}${operator.underlying}$expression")
  }
  
  private[analytics] def eventFilters(eventCategory: String, eventAction: String): Filters = {
    val filtersStr = filter(Dimension.EventCategory, FilterOperator.equals, eventCategory) +
      FilterOperator.and.underlying +
      filter(Dimension.EventAction, FilterOperator.equals, eventAction)
    new Filters(filtersStr)
  }
  
  private[analytics] def eventFilters(eventCategory: String, eventAction: String, eventLabels: Seq[String]): Filters = {
    val filtersStr = eventLabels.map(label => filter(Dimension.EventLabel, FilterOperator.equals, label).underlying).mkString(FilterOperator.or.underlying) +
        FilterOperator.and.underlying +
        eventFilters(eventCategory, eventAction).underlying
        
    new Filters(filtersStr)
  }
  
  private[analytics] def getEvents(profileId: String, startDate: Date, endDate: Date, filters: Filters): Future[Seq[DataRow]] = Future {
    val data: GaData = analytics.data().ga()
      .get("ga:" + profileId, startDate.underlying, endDate.underlying, Metric.TotalEvents.toString)
      .setDimensions(Dimension.EventLabel.toString)
      .setFilters(filters.underlying)
      .execute()

    if (data.getTotalResults > 0) {
      data.getRows.map(row => DataRow(row.get(0), row.get(1)))
    } else {
      Seq.empty[DataRow]
    }
  }

  /////////////////
  // Public methods
  /////////////////
  def getEvents(
    profileId: String,
    startDate: Date,
    endDate: Date,
    eventCategory: String,
    eventAction: String,
    eventLabels: Seq[String]): Future[Seq[DataRow]] = {
    
    val filters = eventFilters(eventCategory, eventAction, eventLabels)
    getEvents(profileId, startDate, endDate, filters)
  }
  
  def getPageViewsByPath(profileId: String, startDate: Date, endDate: Date, path: String): Future[Seq[DataRow]] = Future {
    val filters = filter(Dimension.PagePath, FilterOperator.equals, path)
    
    val data: GaData = analytics.data().ga()
      .get("ga:" + profileId, startDate.underlying, endDate.underlying, Metric.Pageviews.toString)
      .setDimensions(Dimension.PagePath.toString)
      .setFilters(filters.underlying)
      .execute();

    data.getRows.map(row => DataRow(row.get(0), row.get(1)))
  }

  /////////////////
  // Debug methods
  /////////////////
  /**
   * Used to print out profile ids for gathering config info.
   */
  private[analytics] def accountSummaries = {

    val accountSummaries: AccountSummaries = analytics.management().accountSummaries().list().execute()
    accountSummaries.getItems.foreach(account => {
      logger.info(account.getName() + " (" + account.getId() + ")")
      printPropertySummaries(account)
    })

    def printPropertySummaries(accountSummary: AccountSummary) {
      accountSummary.getWebProperties.foreach(property => {
        logger.info("  " + property.getName() + " (" + property.getId() + ")")
        logger.info("  [" + property.getWebsiteUrl() + " | " + property.getLevel() + "]")
        printProfileSummary(property)
      })
    }

    def printProfileSummary(webPropertySummary: WebPropertySummary) {
      webPropertySummary.getProfiles.foreach(profile => {
        logger.info("    " + profile.getName() + " (" + profile.getId() + ") | " + profile.getType())
      })
    }
  }  
}

case class DataRow(dimension: String, metric: String)

class Date(val underlying: String) extends AnyVal {
  override def toString: String = underlying
}

object Date {
  val today = new Date("today")
  val yesterday = new Date("yesterday")
  def daysAgo(num: Int) = new Date(s"${num}daysAgo")
  def fromLocalDate(localDate: LocalDate): Date = {
    new Date(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
  }
}

sealed abstract class Dimension(val name: String) {
  override def toString: String = name
}

object Dimension {
  case object EventAction extends Dimension("ga:eventAction")
  case object EventCategory extends Dimension("ga:eventCategory")
  case object EventLabel extends Dimension("ga:eventLabel")
  case object PagePath extends Dimension("ga:pagePath")
}

class Filters(val underlying: String) extends AnyVal {
  override def toString: String = underlying
}


sealed abstract class Metric(val name: String) {
  override def toString: String = name
}
  
object Metric {
  case object Pageviews extends Metric("ga:pageviews")
  case object TotalEvents extends Metric("ga:totalEvents")
}

sealed abstract class FilterOperator(val underlying: String) {
  override def toString: String = underlying
}

object FilterOperator {
  case object equals extends FilterOperator("==")
  case object notEqual extends FilterOperator("!=")
  case object greaterThan extends FilterOperator(">")
  case object lessThan extends FilterOperator("<")
  case object greaterThanEqual extends FilterOperator(">=")
  case object lessThanEqual extends FilterOperator("<=")
  case object containsSubstring extends FilterOperator("=@")
  case object notContainsSubstring extends FilterOperator("!@")
  case object matchRegex extends FilterOperator("=~")
  case object notMatchRegex extends FilterOperator("!~")
  case object and extends FilterOperator(";")
  case object or extends FilterOperator(",")
}
