package kipsigman.ws.google.youtube

import java.util.Date
import org.scalatest.Matchers
import org.scalatest.WordSpec

class YouTubeVideoSpec extends WordSpec with Matchers {

  val video = YouTubeVideo(
    "27IaHM1u6bg",
    new Date(),
    "Introduction to Case Classes",
    "A walkthrough on how to use Scala Case Classes to reduce boilerplate code, avoid bugs, construct/modify objects, and use pattern matching."
  )

  "url" should {
    "return URL with id" in {
      video.url shouldBe "//youtu.be/27IaHM1u6bg"
    }
  }

  "embedUrl" should {
    "return URL with id" in {
      video.embedUrl shouldBe "//www.youtube.com/embed/27IaHM1u6bg"
    }
  }

}