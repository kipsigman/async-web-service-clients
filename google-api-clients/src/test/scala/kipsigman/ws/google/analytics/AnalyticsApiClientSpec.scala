package kipsigman.ws.google.analytics

import java.time.LocalDate

import org.scalatest.Matchers
import org.scalatest.WordSpec

class AnalyticsApiClientSpec extends WordSpec with Matchers {

  "Date.fromLocalDate" should {
    "format a LocalDate using ISO" in {
      val localDate = LocalDate.of(2016, 1, 28)
      Date.fromLocalDate(localDate).toString shouldBe "2016-01-28"
    }
  }

}