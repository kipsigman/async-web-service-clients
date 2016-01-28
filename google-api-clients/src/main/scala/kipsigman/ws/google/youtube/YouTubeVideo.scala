package kipsigman.ws.google.youtube

import java.util.Date
import java.util.TimeZone
import java.text.SimpleDateFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._

case class YouTubeVideo(
    id: String,
    publishedAt: Date,
    title: String,
    description: String) {

  val url: String = s"//youtu.be/$id"
  val embedUrl: String = s"//www.youtube.com/embed/$id"
}

object YouTubeVideo {

  private val datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  private val timeZone = TimeZone.getTimeZone("UTC")

  def bindDate(dateStr: String): Date = {
    val df = new SimpleDateFormat(datePattern);
    df.setTimeZone(timeZone)
    df.parse(dateStr)
  }

  private val dateReads = Reads.dateReads(datePattern)

  val searchReads: Reads[YouTubeVideo] = (
    (JsPath \ "id" \ "videoId").read[String] and
    (JsPath \ "snippet" \ "publishedAt").read[Date](dateReads) and
    (JsPath \ "snippet" \ "title").read[String] and
    (JsPath \ "snippet" \ "description").read[String]
  )(YouTubeVideo.apply _)

  val videosReads: Reads[YouTubeVideo] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "snippet" \ "publishedAt").read[Date](dateReads) and
    (JsPath \ "snippet" \ "title").read[String] and
    (JsPath \ "snippet" \ "description").read[String]
  )(YouTubeVideo.apply _)

}