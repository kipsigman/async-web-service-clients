package kipsigman.ws.google.youtube

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.typesafe.config.Config

import kipsigman.ws.google.GoogleApiClient
import kipsigman.ws.google.Page

/**
 * Google YouTube API client.
 * Documentation:
 * - https://developers.google.com/youtube/v3/getting-started
 * - http://developers.google.com/apis-explorer/#p/youtube/v3/
 */
@Singleton
class YouTubeApiClient @Inject() (config: Config)(implicit ec: ExecutionContext) extends GoogleApiClient(config) {

  private def defaultChannelId = config.getString("google.youTube.defaultChannelId")

  private val searchUrl = "https://www.googleapis.com/youtube/v3/search"
  private val videosUrl = "https://www.googleapis.com/youtube/v3/videos"
  
  def findVideos(ids: Seq[String]): Future[Page[YouTubeVideo]] = {
    val params =
      Map("part" -> "id,snippet",
        "id" -> ids.mkString(","))

    executeGetPage[YouTubeVideo](videosUrl, params)(ec, YouTubeVideo.videosReads)
  }

  /**
   * Find videos with paging functionality.
   * Page tokens are 0 based index page numbers.
   */
  def findVideos(ids: Seq[String], resultsPerPage: Int = Page.defaultResultsPerPage, pageToken: Option[String] = None): Future[Page[YouTubeVideo]] = {

    // Determine page (0 index)
    val pageIndex = pageToken match {
      case Some(pageNumStr) => pageNumStr.toInt
      case None => 0
    }

    // Validate page
    try {
      require(pageIndex >= 0, s"Invalid pageToken $pageToken")
    } catch {
      case t: Throwable => Future.apply(t)
    }

    // Choose videoIds based on paging
    val indentedVideoIds = ids.drop(Page.offset(pageIndex, resultsPerPage))
    val selectedVideoIds = indentedVideoIds.take(resultsPerPage)
    logger.debug(s"ids=$ids")
    logger.debug(s"indentedVideoIds=$indentedVideoIds")
    logger.debug(s"selectedVideoIds=$selectedVideoIds")

    // Choose page tokens
    val prevPageToken = pageIndex match {
      case 0 => None
      case _ => Option((pageIndex.toInt - 1).toString)
    }
    val nextPageToken = indentedVideoIds.size match {
      case indentedVideosSize if (indentedVideosSize > resultsPerPage) => Option((pageIndex + 1).toString)
      case _ => None
    }

    logger.debug(s"prevPageToken=$prevPageToken, nextPageToken=$nextPageToken")

    // Returns videos from ids but with no paging
    val rawPageFuture = findVideos(selectedVideoIds)

    // Add paging
    rawPageFuture.map(rawPage => rawPage.copy(prevPageToken = prevPageToken, nextPageToken = nextPageToken))
  }

  def findPublicVideosByChannel(channelId: String = defaultChannelId, q: Option[String] = None, resultsPerPage: Int = Page.defaultResultsPerPage, pageToken: Option[String] = None): Future[Page[YouTubeVideo]] = {

    val pageParams = pageToken match {
      case Some(token) => Map("pageToken" -> token)
      case None => Map[String, String]()
    }

    val searchParams = q match {
      case Some(qValue) => Map("q" -> qValue)
      case None => Map[String, String]()
    }

    val params =
      Map("part" -> "id,snippet",
        "channelId" -> channelId,
        "type" -> "video",
        "maxResults" -> resultsPerPage.toString,
        "order" -> "date") ++ searchParams ++ pageParams

    executeGetPage[YouTubeVideo](searchUrl, params)(ec, YouTubeVideo.searchReads)
  }
}