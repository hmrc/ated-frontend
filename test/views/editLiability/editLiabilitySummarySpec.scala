/*
 * Copyright 2020 HM Revenue & Customs
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

package views.editLiability

import builders.PropertyDetailsBuilder
import config.ApplicationConfig
import testhelpers.MockAuthUtil
import models.StandardAuthRetrievals
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTimeZone, LocalDate}
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import utils.PeriodUtils._
import utils.PeriodUtils

class editLiabilitySummarySpec extends FeatureSpec with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]

  val thisYear: Int = calculatePeriod()
  val nextYear: Int = thisYear + 1

  def formatDate(date: LocalDate): String = DateTimeFormat.forPattern("d MMMM yyyy").withZone(DateTimeZone.forID("Europe/London")).print(date)
  feature("The user can view their property details summary before they submit it") {

    info("as a client i want to be my property details summary")

    scenario("Amended charge with periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period)
      assert(displayPeriods.size === 2)
      val html = views.html.editLiability.editLiabilitySummary(propertyDetails, "A", displayPeriods,
        PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Check your details are correct")
      assert(document.getElementById("edit-liability-summary-header").text === "Check your details are correct")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(calculatePeriod()))} to ${formatDate(periodEndDate(calculatePeriod()))}.")
      assert(document.getElementById("edit-liability-header").text() === "Property details")

      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("property-date-of-valuation-label-0").text() === "Professionally valued")
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
      assert(document.getElementById("ated-charge-text-further") === null)
      assert(document.getElementById("ated-charge-text-amended")
        .text() === "Based on the information you have given us your revised ATED charge for this amended return is")
      assert(document.getElementById("ated-charge-text-changed") === null)
      assert(document.getElementById("ated-charge-value").text() === "£1,000")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
    }

    scenario("Changed charge with periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period)
      assert(displayPeriods.size === 2)
      val html = views.html.editLiability.editLiabilitySummary(propertyDetails, "C", displayPeriods,
        PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Check your details are correct")
      assert(document.getElementById("edit-liability-summary-header").text === "Check your details are correct")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(calculatePeriod()))} to ${formatDate(periodEndDate(calculatePeriod()))}.")
      assert(document.getElementById("edit-liability-header").text() === "Property details")

      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("property-date-of-valuation-label-0").text() === "Professionally valued")
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
      assert(document.getElementById("ated-charge-text-further") === null)
      assert(document.getElementById("ated-charge-text-amended") === null)
      assert(document.getElementById("ated-charge-text-changed")
        .text() === "Based on the information you have given us your revised ATED charge for this changed return is")
      assert(document.getElementById("ated-charge-value").text() === "£1,000")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }

    scenario("Futher Charge the basic summary with no periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))

      val html = views.html.editLiability.editLiabilitySummary(propertyDetails, "F", Nil,
        PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Check your details are correct")
      assert(document.getElementById("edit-liability-summary-header").text === "Check your details are correct")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(calculatePeriod()))} to ${formatDate(periodEndDate(calculatePeriod()))}.")
      assert(document.getElementById("edit-liability-header").text() === "Property details")

      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("property-date-of-valuation-label-0").text() === "Professionally valued")
      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0") === null)
      assert(document.getElementById("period-0") === null)
      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("additional-information-label").text() === "Additional information")
      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("avoidance-scheme-label").text() === "Avoidance scheme reference number")
      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("return-status-label").text() === "Status")
      assert(document.getElementById("ated-charge-text-further")
        .text() === "Based on the information you have given us your revised ATED charge for this further return is")
      assert(document.getElementById("ated-charge-text-amended") === null)
      assert(document.getElementById("ated-charge-text-changed") === null)
      assert(document.getElementById("ated-charge-value").text() === "£1,000")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }

}
