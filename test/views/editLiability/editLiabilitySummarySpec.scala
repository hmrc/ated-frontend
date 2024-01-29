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

package views.editLiability

import builders.PropertyDetailsBuilder
import config.ApplicationConfig
import models.StandardAuthRetrievals
import java.time.format.DateTimeFormat
import java.time.{DateTimeZone, LocalDate}
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.PeriodUtils
import utils.PeriodUtils._
import views.html.editLiability.editLiabilitySummary

class editLiabilitySummarySpec extends AnyFeatureSpec with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val injectedViewInstance: editLiabilitySummary = app.injector.instanceOf[views.html.editLiability.editLiabilitySummary]

  val thisYear: Int = calculatePeakStartYear()
  val nextYear: Int = thisYear + 1

  def formatDate(date: LocalDate): String = DateTimeFormat.forPattern("d MMMM yyyy").withZone(DateTimeZone.forID("Europe/London")).print(date)
  Feature("The user can view their property details summary before they submit it") {

    info("as a client i want to be my property details summary")

    Scenario("Amended charge with periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, thisYear)
      assert(displayPeriods.size === 2)
      val html = injectedViewInstance(propertyDetails, "A", displayPeriods,
        PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Check your details are correct")
      assert(document.getElementsByTag("h1").text contains "Check your details are correct")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() equals "This section is: Change return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(calculatePeakStartYear()))} to ${formatDate(periodEndDate(calculatePeakStartYear()))}.")
      assert(document.getElementById("edit-liability-header").text() === "Property details")
      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("property-date-of-valuation-label-0").text() === "Professionally valued")
      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text() === "Liable for charge")
      assert(document.getElementById("period-0").text() === "1 April " + thisYear + " to 31 August "  + thisYear)
      assert(document.getElementById("return-type-1").text() === "Rental business")
      assert(document.getElementById("period-1").text() === "1 September " + thisYear + " to 31 March " + nextYear)
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
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
    }

    Scenario("Changed charge with periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, thisYear)
      assert(displayPeriods.size === 2)
      val html = injectedViewInstance(propertyDetails, "C", displayPeriods,
        PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Check your details are correct")
      assert(document.getElementsByTag("h1").text contains  "Check your details are correct")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(calculatePeakStartYear()))} to ${formatDate(periodEndDate(calculatePeakStartYear()))}.")
      assert(document.getElementById("edit-liability-header").text() === "Property details")

      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("property-date-of-valuation-label-0").text() === "Professionally valued")
      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text() === "Liable for charge")
      assert(document.getElementById("period-0").text() === "1 April " + thisYear + " to 31 August "  + thisYear)
      assert(document.getElementById("return-type-1").text() === "Rental business")
      assert(document.getElementById("period-1").text() === "1 September " + thisYear + " to 31 March " + nextYear)
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
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }

    Scenario("Further Charge the basic summary with no periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))

      val html = injectedViewInstance(propertyDetails, "F", Nil,
        PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Check your details are correct")
      assert(document.getElementsByTag("h1").text contains "Check your details are correct")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(calculatePeakStartYear()))} to ${formatDate(periodEndDate(calculatePeakStartYear()))}.")
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
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }

    Scenario("Changed charge with refund - UK account") {

      Given("the client is changing a liability, is due a refund and is asked bank details")
      When("The user views the page and has a UK bank account")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetailsWithRefund(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(8875.12)), true, Some("UK"))
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, thisYear)
      assert(displayPeriods.size === 1)
      val html = injectedViewInstance(propertyDetails, "C", displayPeriods,
        PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The header should match - Check your details are correct")
      assert(document.getElementsByTag("h1").text contains "Check your details are correct")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(2015))} to ${formatDate(periodEndDate(2015))}.")

      assert(document.getElementById("edit-liability-header").text() === "Property details")
      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("address-line-1").text() === "addr1")
      assert(document.getElementById("address-line-2").text() === "addr2")
      assert(document.getElementById("address-line-3").text() === "addr3")
      assert(document.getElementById("address-postcode").text() === "123456")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-title-number").text() === "titleNo")

      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("property-value-value-0").text() === "£2,000,000")
      assert(document.getElementById("property-date-of-valuation-label-0").text() === "Professionally valued")
      assert(document.getElementById("property-is-Valued-by-agent-no-0").text() === "No")

      assert(document.getElementById("bank-details-header").text() === "Bank details")
      assert(document.getElementById("has-account-uk-label").text() === "Bank account to pay a refund")
      assert(document.getElementById("has-account-uk").text() === "Yes")
      assert(document.getElementById("uk-account-label").text() === "UK bank account")
      assert(document.getElementById("uk-account").text() === "Yes")
      assert(document.getElementById("uk-account-holder-name-label").text() === "Account holder name")
      assert(document.getElementById("uk-account-holder-name-value").text() === "Account name")
      assert(document.getElementById("account-number-label").text() === "Account number")
      assert(document.getElementById("account-number-value").text() === "12312312")
      assert(document.getElementById("sort-code-label").text() === "Sort code")
      assert(document.getElementById("sort-code-value").text() === "12 - 12 - 12")

      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text() === "Liable for charge")
      assert(document.getElementById("period-0").text() === "1 April " + "2015" + " to 31 March "  + "2016")

      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("avoidance-scheme-label").text() === "Avoidance scheme reference number")
      assert(document.getElementById("avoidance-scheme-value").text() === "taxAvoidanceScheme")
      assert(document.getElementById("promoter-reference-label").text() === "Promoter reference number")
      assert(document.getElementById("promoter-scheme-value").text() === "taxAvoidancePromoterReference")

      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("additional-information-label").text() === "Additional information")
      assert(document.getElementById("additional-information-value").text() === "supportingInfo")

      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("return-status-label").text() === "Status")
      assert(document.getElementById("return-status-value").text() === "Draft")

      assert(document.getElementById("ated-charge-text-further") === null)
      assert(document.getElementById("ated-charge-text-amended") === null)
      assert(document.getElementById("ated-charge-text-changed")
        .text() === "Based on the information you have given us your revised ATED charge for this changed return is")
      assert(document.getElementById("ated-charge-value").text() === "£8,875")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }

    Scenario("Changed charge with refund - Overseas account") {

      Given("the client is changing a liability, is due a refund and is asked bank details")
      When("The user views the page and has an Overseas bank account")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetailsWithRefund(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(8875.12)), true, Some("NonUK"))
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, thisYear)
      assert(displayPeriods.size === 1)
      val html = injectedViewInstance(propertyDetails, "C", displayPeriods,
        PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The header should match - Check your details are correct")
      assert(document.getElementsByTag("h1").text contains "Check your details are correct")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(2015))} to ${formatDate(periodEndDate(2015))}.")

      assert(document.getElementById("edit-liability-header").text() === "Property details")
      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("address-line-1").text() === "addr1")
      assert(document.getElementById("address-line-2").text() === "addr2")
      assert(document.getElementById("address-line-3").text() === "addr3")
      assert(document.getElementById("address-postcode").text() === "123456")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-title-number").text() === "titleNo")

      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("property-value-value-0").text() === "£2,000,000")
      assert(document.getElementById("property-date-of-valuation-label-0").text() === "Professionally valued")
      assert(document.getElementById("property-is-Valued-by-agent-no-0").text() === "No")

      assert(document.getElementById("bank-details-header").text() === "Bank details")
      assert(document.getElementById("has-account-overseas-label").text() === "Bank account to pay a refund")
      assert(document.getElementById("has-account-overseas").text() === "Yes")
      assert(document.getElementById("overseas-account-label").text() === "UK bank account")
      assert(document.getElementById("overseas-account").text() === "No")
      assert(document.getElementById("overseas-account-holder-name-label").text() === "Account holder name")
      assert(document.getElementById("overseas-account-holder-name-value").text() === "Overseas account name")
      assert(document.getElementById("iban-label").text() === "IBAN")
      assert(document.getElementById("iban-value").text() === "111222333444555")
      assert(document.getElementById("bic-swift-code-label").text() === "SWIFT Code")
      assert(document.getElementById("bic-swift-code-value").text() === "1234 56 78 999")

      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text() === "Liable for charge")
      assert(document.getElementById("period-0").text() === "1 April " + "2015" + " to 31 March "  + "2016")

      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("avoidance-scheme-label").text() === "Avoidance scheme reference number")
      assert(document.getElementById("avoidance-scheme-value").text() === "taxAvoidanceScheme")
      assert(document.getElementById("promoter-reference-label").text() === "Promoter reference number")
      assert(document.getElementById("promoter-scheme-value").text() === "taxAvoidancePromoterReference")

      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("additional-information-label").text() === "Additional information")
      assert(document.getElementById("additional-information-value").text() === "supportingInfo")

      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("return-status-label").text() === "Status")
      assert(document.getElementById("return-status-value").text() === "Draft")

      assert(document.getElementById("ated-charge-text-further") === null)
      assert(document.getElementById("ated-charge-text-amended") === null)
      assert(document.getElementById("ated-charge-text-changed")
        .text() === "Based on the information you have given us your revised ATED charge for this changed return is")
      assert(document.getElementById("ated-charge-value").text() === "£8,875")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }

    Scenario("Changed charge with refund but user does NOT have a bank account") {

      Given("the client is changing a liability, is due a refund and is asked bank details")
      When("The user views the page but does not have bank details")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetailsWithRefund(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(8875.12)), false)
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, thisYear)
      assert(displayPeriods.size === 1)
      val html = injectedViewInstance(propertyDetails, "C", displayPeriods,
        PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The header should match - Check your details are correct")
      assert(document.getElementsByTag("h1").text contains "Check your details are correct")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(2015))} to ${formatDate(periodEndDate(2015))}.")

      assert(document.getElementById("edit-liability-header").text() === "Property details")
      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("address-line-1").text() === "addr1")
      assert(document.getElementById("address-line-2").text() === "addr2")
      assert(document.getElementById("address-line-3").text() === "addr3")
      assert(document.getElementById("address-postcode").text() === "123456")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-title-number").text() === "titleNo")

      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("property-value-value-0").text() === "£2,000,000")
      assert(document.getElementById("property-date-of-valuation-label-0").text() === "Professionally valued")
      assert(document.getElementById("property-is-Valued-by-agent-no-0").text() === "No")

      assert(document.getElementById("bank-details-header-no-bank-account").text() === "Bank details")
      assert(document.getElementById("bank-details-answered-no-label").text() === "Bank account to pay a refund")
      assert(document.getElementById("bank-details-answered-no").text() === "No")

      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text() === "Liable for charge")
      assert(document.getElementById("period-0").text() === "1 April " + "2015" + " to 31 March "  + "2016")

      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("avoidance-scheme-label").text() === "Avoidance scheme reference number")
      assert(document.getElementById("avoidance-scheme-value").text() === "taxAvoidanceScheme")
      assert(document.getElementById("promoter-reference-label").text() === "Promoter reference number")
      assert(document.getElementById("promoter-scheme-value").text() === "taxAvoidancePromoterReference")

      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("additional-information-label").text() === "Additional information")
      assert(document.getElementById("additional-information-value").text() === "supportingInfo")

      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("return-status-label").text() === "Status")
      assert(document.getElementById("return-status-value").text() === "Draft")

      assert(document.getElementById("ated-charge-text-further") === null)
      assert(document.getElementById("ated-charge-text-amended") === null)
      assert(document.getElementById("ated-charge-text-changed")
        .text() === "Based on the information you have given us your revised ATED charge for this changed return is")
      assert(document.getElementById("ated-charge-value").text() === "£8,875")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }

    Scenario("Changed charge with refund - user has bank account but leaves details incomplete") {

      Given("the client is changing a liability, is due a refund and is asked bank details")
      When("The user views the page but user leaves bank details incomplete")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetailsWithRefund(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(8875.12)), true)
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, thisYear)
      assert(displayPeriods.size === 1)
      val html = injectedViewInstance(propertyDetails, "C", displayPeriods,
        PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The header should match - Check your details are correct")
      assert(document.getElementsByTag("h1").text contains "Check your details are correct")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      assert(document.getElementById("details-text").text() === s"For the ATED period from " +
        s"${formatDate(periodStartDate(2015))} to ${formatDate(periodEndDate(2015))}.")

      assert(document.getElementById("edit-liability-header").text() === "Property details")
      assert(document.getElementById("property-address-label").text() === "Address")
      assert(document.getElementById("address-line-1").text() === "addr1")
      assert(document.getElementById("address-line-2").text() === "addr2")
      assert(document.getElementById("address-line-3").text() === "addr3")
      assert(document.getElementById("address-postcode").text() === "123456")
      assert(document.getElementById("property-title-number-label").text() === "Property’s title number")
      assert(document.getElementById("property-title-number").text() === "titleNo")

      assert(document.getElementById("property-value-header").text() === "Value of the property")
      assert(document.getElementById("property-value-label-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("property-value-value-0").text() === "£2,000,000")
      assert(document.getElementById("property-date-of-valuation-label-0").text() === "Professionally valued")
      assert(document.getElementById("property-is-Valued-by-agent-no-0").text() === "No")

      assert(document.getElementById("bank-details-header").text() === "Bank details")
      assert(document.getElementById("account-type-incomplete-label").text() === "Bank account to pay a refund")
      assert(document.getElementById("account-type-incomplete").text() === "INCOMPLETE")

      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text() === "Liable for charge")
      assert(document.getElementById("period-0").text() === "1 April " + "2015" + " to 31 March "  + "2016")

      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("avoidance-scheme-label").text() === "Avoidance scheme reference number")
      assert(document.getElementById("avoidance-scheme-value").text() === "taxAvoidanceScheme")
      assert(document.getElementById("promoter-reference-label").text() === "Promoter reference number")
      assert(document.getElementById("promoter-scheme-value").text() === "taxAvoidancePromoterReference")

      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("additional-information-label").text() === "Additional information")
      assert(document.getElementById("additional-information-value").text() === "supportingInfo")

      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("return-status-label").text() === "Status")
      assert(document.getElementById("return-status-value").text() === "Draft")

      assert(document.getElementById("ated-charge-text-further") === null)
      assert(document.getElementById("ated-charge-text-amended") === null)
      assert(document.getElementById("ated-charge-text-changed")
        .text() === "Based on the information you have given us your revised ATED charge for this changed return is")
      assert(document.getElementById("ated-charge-value").text() === "£8,875")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }

}
