/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views.propertyDetails

import builders.PropertyDetailsBuilder
import config.ApplicationConfig
import models.StandardAuthRetrievals
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, LocalDate}
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import testhelpers.MockAuthUtil
import utils.PeriodUtils
import utils.PeriodUtils._

class propertyDetailsPrintFriendlySpec extends AnyFeatureSpecLike with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val thisYear: Int = calculatePeakStartYear()
  val nextYear: Int = thisYear + 1

  def formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d LLLL yyyy").withZone(ZoneId.of("Europe/London")))
  Feature("The user can view their property details summary before they submit it") {

    info("as a client i want to be my property details summary")

    Scenario("return the basic summary with periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))

      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, 2015)
      assert(displayPeriods.size === 2)

      val html = views.html.propertyDetails.propertyDetailsPrintFriendly(propertyDetails,
        displayPeriods, PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Some("ACME Ltd"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Chargeable return for")
      assert(document.getElementById("property-details-summary-header").text.contains("Chargeable return for") === true)

      assert(document.getElementById("details-text").text() === s"For the ATED period from" +
        s" ${formatDate(periodStartDate(calculatePeakStartYear()))} to ${formatDate(periodEndDate(calculatePeakStartYear()))}.")
      assert(document.getElementById("property-details-header").text() === "Property details")

      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text === "Liable for charge")
      assert(document.getElementById("period-0").text === "1 April " + thisYear + " to 31 August "  + thisYear)
      assert(document.getElementById("return-type-1").text === "Rental business")
      assert(document.getElementById("period-1").text === "1 September " + thisYear + " to 31 March " + nextYear)
      assert(document.getElementById("return-type-2") === null)
      assert(document.getElementById("period-2") === null)
      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("additional-information-label").text() === "Additional information")
      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("avoidance-scheme-label").text() === "Avoidance scheme reference number")
      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("return-status-label").text() === "Status")
      assert(document.getElementById("ated-charge-text").text() === "Based on the information you have given us your ATED charge is")
      assert(document.getElementById("ated-charge-value").text() === "£1,000")
    }

    Scenario("return the basic summary with no periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))

      val html = views.html.propertyDetails.propertyDetailsPrintFriendly(propertyDetails,
        Nil, PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Some("ACME Ltd"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Chargeable return for")
      assert(document.getElementById("property-details-summary-header").text.contains("Chargeable return for") === true)

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(calculatePeakStartYear()))} to ${formatDate(periodEndDate(calculatePeakStartYear()))}.")
      assert(document.getElementById("property-details-header").text() === "Property details")

      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0") === null)
      assert(document.getElementById("period-0") === null)
      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("additional-information-label").text() === "Additional information")
      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("avoidance-scheme-label").text() === "Avoidance scheme reference number")
      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("return-status-label").text() === "Status")
      assert(document.getElementById("ated-charge-text").text() === "Based on the information you have given us your ATED charge is")
      assert(document.getElementById("ated-charge-value").text() === "£1,000")
    }
  }

}
