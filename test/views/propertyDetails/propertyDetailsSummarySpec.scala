/*
 * Copyright 2022 HM Revenue & Customs
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
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTimeZone, LocalDate}
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.PeriodUtils
import utils.PeriodUtils._

class propertyDetailsSummarySpec extends AnyFeatureSpecLike with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val thisYear: Int = calculatePeakStartYear()
  val nextYear: Int = thisYear + 1

  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsSummary]

  def formatDate(date: LocalDate): String = DateTimeFormat.forPattern("d MMMM yyyy").withZone(DateTimeZone.forID("Europe/London")).print(date)
  Feature("The user can view their property details summary before they submit it") {

    info("as a client i want to be my property details summary")

    Scenario("return the basic summary with periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, 2015)
      assert(displayPeriods.size === 2)

      val html = injectedViewInstance(propertyDetails, displayPeriods,
        canSubmit = true, PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The header should match - Check your details are correct")
      assert(document.getElementsByClass("govuk-heading-l").text contains "Check your details are correct")

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-heading-l").text contains  "This section is: Create return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(calculatePeakStartYear()))} to ${formatDate(periodEndDate(calculatePeakStartYear()))}.")
      assert(document.getElementById("property-details-header").text() === "Property details")

      assert(document.getElementsByClass("govuk-summary-list__key").text() contains  "Address")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains  "Property’s title number")
      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains "Value for the purposes of ATED")
      assert(document.getElementById("property-is-Valued-by-agent-label-0") === null)
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains  "Professionally valued")
      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains "Liable for charge")
      assert(document.getElementsByClass("govuk-summary-list__value").text contains "1 April " + thisYear + " to 31 August "  + thisYear)
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains "Rental business")
      assert(document.getElementsByClass("govuk-summary-list__value").text contains "1 September " + thisYear + " to 31 March " + nextYear)
      assert(document.getElementById("return-type-2") === null)
      assert(document.getElementById("period-2") === null)
      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains "Additional information")
      assert(document.getElementById("avoidance-scheme-header-incomplete").text() === "Avoidance scheme")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains "Avoidance scheme reference number")
      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains "Status")
      assert(document.getElementsByClass("govuk-body-m").text() === "Based on the information you have given us your ATED charge is")
      assert(document.getElementById("ated-charge-value").text() === "£1,000")
    }

    Scenario("return the basic summary with no periods") {
      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))

      val html = injectedViewInstance(propertyDetails, Nil,
          canSubmit = true, PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Html(""), None)

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Check your details are correct")
      assert(document.getElementsByClass("govuk-heading-l").text contains "Check your details are correct")

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-heading-l").text contains "This section is: Create return")

      assert(document.getElementById("details-text")
        .text() === s"For the ATED period from ${formatDate(periodStartDate(calculatePeakStartYear()))} to ${formatDate(periodEndDate(calculatePeakStartYear()))}.")
      assert(document.getElementById("property-details-header").text() === "Property details")

      assert(document.getElementsByClass("govuk-summary-list__key").text() contains  "Address")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains  "Property’s title number")
      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains "Value for the purposes of ATED")
      assert(document.getElementById("property-is-Valued-by-agent-label-0") === null)
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains  "Professionally valued")
      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0") === null)
      assert(document.getElementById("period-0") === null)
      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains "Additional information")
      assert(document.getElementById("avoidance-scheme-header-incomplete").text() === "Avoidance scheme")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains "Avoidance scheme reference number")
      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementsByClass("govuk-summary-list__key").text() contains "Status")
      assert(document.getElementsByClass("govuk-body-m").text() === "Based on the information you have given us your ATED charge is")
      assert(document.getElementById("ated-charge-value").text() === "£1,000")
      assert(document.getElementById("submit-disabled-text").text() == "You cannot submit returns until 1 April.")
    }

    Scenario("user comes in during march to create a draft return so submit should be disabled") {

      Given("the client is creating a new liability during march")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))

      val html = injectedViewInstance(propertyDetails, Nil,
        canSubmit = false, PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Html(""), None)

      val document = Jsoup.parse(html.toString())

      Then("The text for submit being disabled should appear")
      assert(document.getElementById("submit-disabled-text").text() === "You cannot submit returns until 1 April.")
    }
  }

}
