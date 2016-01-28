package kipsigman.ws.google.youtube

import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.slf4j.LoggerFactory

import com.typesafe.config.ConfigFactory

class YouTubeApiClientIntegrationSpec extends WordSpec with Matchers with ScalaFutures {

  private val logger = LoggerFactory.getLogger(this.getClass)

  // implicit ExecutionContext
  import scala.concurrent.ExecutionContext.Implicits.global
  
  implicit override def patienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(100, Millis))

  val config = ConfigFactory.load()
  
  val repository = new YouTubeApiClient(config)
  
  // Test data (Kip Sigman channel)
  val testChannelId = "UCHSFaued2BlLtVlTda3JyNw"
  val testVideoIds = Seq("27IaHM1u6bg", "FdJiy42ExEM")

  "findPublicVideos" should {
    "return public videos for default channel" in {
      val pageFuture = repository.findPublicVideosByChannel(channelId = testChannelId, resultsPerPage = 5)
      whenReady(pageFuture) { page =>
        page.resultsPerPage shouldBe 5
        page.totalResults should be > 5
        page.items.size shouldBe 5
        page.prevPageToken shouldBe None
        page.nextPageToken shouldBe Some("CAUQAA")
      }

      val page2Future = repository.findPublicVideosByChannel(channelId = testChannelId, pageToken = Some("CAUQAA"))
      whenReady(page2Future) { page =>
        page.items.size shouldBe 2
        page.prevPageToken shouldBe Some("CAUQAQ")
        page.nextPageToken shouldBe None
      }
    }

    "return public videos for default channel with search term" in {
      val pageFuture = repository.findPublicVideosByChannel(channelId = testChannelId, q = Some("case classes"))
      whenReady(pageFuture) { page =>
        page.items.size shouldBe 1
        page.resultsPerPage shouldBe 10
        page.totalResults should be > 0
      }
    }
  }

  "findVideos" should {
    "return videos for ids" in {
      val pageFuture = repository.findVideos(testVideoIds)
      whenReady(pageFuture) { page =>
        page.resultsPerPage shouldBe testVideoIds.size
        page.totalResults shouldBe testVideoIds.size
        page.items.size shouldBe testVideoIds.size
        page.prevPageToken shouldBe None
        page.nextPageToken shouldBe None
      }
    }
  }
}