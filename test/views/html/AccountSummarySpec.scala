/*
 * Copyright 2025 HM Revenue & Customs
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

package views.html

import config.ApplicationConfig
import models.{ClientMandateDetails, StandardAuthRetrievals}
import org.scalatest.Assertion
import play.api.i18n.MessagesApi
import play.api.test.Injecting
import play.twirl.api.{Html, HtmlFormat}
import testhelpers.{AtedViewSpec, MockAuthUtil}
import utils.TestModels

class AccountSummarySpec extends AtedViewSpec with MockAuthUtil with TestModels with Injecting {

  implicit val mockAppConfig: ApplicationConfig = inject[ApplicationConfig]

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val injectedViewInstance: accountSummary = inject[views.html.accountSummary]

  val view: HtmlFormat.Appendable = injectedViewInstance(
    currentYearReturnsForDisplay,
    totalCurrentYearReturns = 2,
    summaryReturnsModel(periodKey = currentTaxYear),
    Some(organisationName),
    atedReference,
    Some(clientMandateDetails),
    Html(""),
    cancelAgentUrl,
    currentYear,
    currentTaxYear,
    fromAccountSummary = true
  )

  def row(rowNumber: Int) = s"#current-tax-year-returns > div:nth-child($rowNumber)"

  def checkRowItem(rowNum: Int, col1: String, col2: String, col3: String, href: String): Assertion = {
    assert(doc.select(s"${row(rowNum)} > dt").text() === col1)
    assert(doc.select(s"${row(rowNum)} > dd.govuk-summary-list__value").text() === col2)
    assert(doc.select(s"${row(rowNum)} > dd.govuk-summary-list__actions > a").attr("href") === href)
    assert(doc.select(s"${row(rowNum)} > dd.govuk-summary-list__actions > a").text().contains(col3))
  }

  "AccountSummary" when {

    "regardless of returns data" should {
      "have the correct title" in {
        assert(doc.title() === "Annual Tax on Enveloped Dwellings (ATED) summary - Submit and view your ATED returns - GOV.UK")
      }

      "have the correct h1" in {
        assert(doc.select("h1").text() contains "Annual Tax on Enveloped Dwellings (ATED) summary")
      }

      "have the correct valuation date change banner" in {
        assert(doc.select("#valuation-banner1").text() ===  "You must revalue your property every 5 years under ATED rules.")
        assert(doc.select("#valuation-banner2").text() === "For the 2023 to 2024 chargeable period, if you bought the property:")
        assert(doc.select("#valuation-banner3").text() === "on or before 1 April 2022, use 1 April 2022 as the revaluation date")
        assert(doc.select("#valuation-banner4").text() === "after 1 April 2022, use the date you acquired it as the valuation date")
      }

      "have the correct caption" in {
        assert(doc.select(".govuk-caption-xl").text() === s"You are logged in as: $organisationName")
      }

      "have correct deadline info text" in {
        val nextTaxYear = currentTaxYear+1
        assert(doc.select(".govuk-body").get(0).text() contains
          s"The deadline for $currentTaxYear to $nextTaxYear returns and payments for all ATED-eligible properties that you own on 1 April $currentTaxYear is 30 April $currentTaxYear")
        assert(doc.select(".govuk-body").get(1).text() contains
          "Returns for newly acquired ATED properties must be sent to HMRC within 30 days of the date of acquisition (90 days from start date for new builds)")
      }

      "have the correct banner link" in {
        assert(doc.select(".govuk-header__service-name").attr("href") === "/ated/home")
      }
    }

    "the user has ATED balance" should {

      "have the correct heading" in {
        assert(doc.select(".govuk-heading-l").first.text() === "Amount due")
      }

      "have the correct info text" in {
        assert(doc.select(".govuk-body").get(2).text() contains
          "You will need your payment reference number to pay. Your payment reference number is on your latest return.")
        assert(doc.select(".govuk-body").get(3).text() contains
          "There can be a 24 hour delay before you see any updates on your balance after you have paid.")
      }

      "have the pay now button" in {
        assert(doc.select("#pay-now").text() === "Pay now")
        assert(doc.select("#pay-now").attr("href")
          === "https://www.tax.service.gov.uk/pay/enter-annual-tax-enveloped-dwellings-payment-reference")
      }

      "have the other ways of pay link" in {
        assert(doc.select("#other-ways-you-can-pay").text() === "Other ways you can pay")
        assert(doc.select("#other-ways-you-can-pay").attr("href") === "https://www.gov.uk/guidance/pay-annual-tax-on-enveloped-dwellings")
      }
    }

    "the user has property and relief returns for the current year" should {

      "have the correct heading" in {
        assert(doc.select("#main-content > div > div.govuk-grid-column-two-thirds > h2:nth-of-type(2)").text() === "Current year ATED returns")
      }

      "show the returns" in {
        checkRowItem(1, "Change_Liability", "Draft", "View or change", "example/draft/route")
        checkRowItem(2, "19 Stone Row", "Submitted", "View or change", "example/non-draft/route")
      }

      "show the Create a new return for current tax year button" in {
        assert(doc.select(
          "#create-return-1").text === s"Create a new return for $currentTaxYear to ${currentTaxYear + 1}"
        )
        assert(doc.select(
          "#create-return-1").attr("href") === s"/ated/period-summary/$currentTaxYear/createReturn?fromAccountSummary=true"
        )
      }

      "show content to say how many returns are being displayed" in {
        assert(doc.select(".govuk-hint").text === "")
      }
    }

    "more than 5 returns" should {

      lazy val fiveReturns = currentYearReturnsForDisplay++currentYearReturnsForDisplay++Seq(currentYearReturnsForDisplay.head)

      val totalCurrentYearReturns = 6
      lazy val view = injectedViewInstance(
        fiveReturns,
        totalCurrentYearReturns = totalCurrentYearReturns,
        summaryReturnsModel(periodKey = currentTaxYear, withPastReturns = true),
        Some(organisationName),
        atedReference,
        Some(clientMandateDetails),
        Html(""),
        cancelAgentUrl,
        currentYear,
        currentTaxYear,
        fromAccountSummary = false
      )

      "show the view all returns link" in {
        assert(doc(view).select("#view-all-returns").text === s"View all returns for $currentTaxYear to ${currentTaxYear + 1}")
        assert(doc(view).select("#view-all-returns").attr("href") === s"/ated/period-summary/$currentTaxYear")
      }

      "show content to say how many returns are being displayed" in {
        assert(doc(view).select(".govuk-hint").text === "Showing 5 of 6 returns")
      }

    }

    "less than 6 returns" should {

      "show content to say how many returns are being displayed" in {
        lazy val view = injectedViewInstance(
          currentYearReturnsForDisplay,
          totalCurrentYearReturns = 2,
          summaryReturnsModel(periodKey = currentTaxYear, withPastReturns = true),
          Some(organisationName),
          atedReference,
          Some(clientMandateDetails),
          Html(""),
          cancelAgentUrl,
          currentYear,
          currentTaxYear,
          fromAccountSummary = false
        )
        assert(doc(view).select(".govuk-hint").text === "")
        assert(doc.select("#view-all-returns").size() === 0)
      }
    }

    "there is no current year ATED returns" should {

      "have the correct info text" in {
        lazy val view = injectedViewInstance(
          currentYearReturnsForDisplay.empty,
          totalCurrentYearReturns = 0,
          summaryReturnsModel(periodKey = currentTaxYear, withPastReturns = true),
          Some(organisationName),
          atedReference,
          Some(clientMandateDetails),
          Html(""),
          cancelAgentUrl,
          currentYear,
          currentTaxYear,
          fromAccountSummary = false
        )
        assert(doc(view).select("#empty-current-year-returns").text === "You have no current year returns.")
      }
    }

    "show the previous years ATED returns link" should {

      "have the correct heading" in {
        assert(doc.select(".govuk-heading-l").get(2).text() === "Previous years ATED returns")
      }

      "show the previous year returns link" in {
        assert(doc.select("#previous-returns").text === "View or change ATED returns for previous chargeable periods")
        assert(doc.select("#previous-returns").attr("href") === "/ated/prev-period-summary")
      }
    }

    "Your ATED Details" should {
      lazy val view = injectedViewInstance(
        currentYearReturnsForDisplay.empty,
        totalCurrentYearReturns = 3,
        summaryReturnsModel(periodKey = currentTaxYear, withPastReturns = true),
        Some(organisationName),
        atedReference,
        Some(clientMandateDetails),
        Html(""),
        cancelAgentUrl,
        currentYear,
        currentTaxYear,
        fromAccountSummary = false
      )

      "show the correct heading" in {
        assert(doc(view).select(".govuk-heading-l").get(3).text() === "Your ATED details")
      }

      "show the correct organisation" in {
        doc(view).getElementsByClass("govuk-summary-list__key govuk-!-width-one-half") must not be None
        doc(view).getElementsByClass("govuk-summary-list__key govuk-!-width-one-half").size() mustEqual 2
        doc(view).select("#company-details-label").text() must be ("Company details")

        doc(view).getElementsByClass("govuk-summary-list__value govuk-!-width-one-half") must not be None
        doc(view).getElementsByClass("govuk-summary-list__value govuk-!-width-one-half").size() mustEqual 2
        doc(view).select("#company-details").text() must be (organisationName)
      }

      "show the ATED reference number" in{
        doc(view).select("#reference-number-label").text() must be ("ATED reference number")
        doc(view).select("#reference-number").text() must be (atedReference)
      }

      "show the view your ATED details link" in {
        assert(doc.select("#view-ated-details").text === "View your ATED details")
        assert(doc.select("#view-ated-details").attr("href") === "/ated/company-details")
      }

    }

    "Your ATED Agent" should {

      "show the correct heading" in {
        val headingIndex = 4
        assert(doc.select(".govuk-heading-l").get(headingIndex).text() === "Your ATED agent")
      }

      "have Approved agent" in {
        val view = injectedViewInstance(
          currentYearReturnsForDisplay,
          totalCurrentYearReturns = 2,
          summaryReturnsModel(periodKey = currentTaxYear, withPastReturns = true),
          Some(organisationName),
          atedReference,
          Some(ClientMandateDetails(
            agentName = "name1",
            changeAgentLink = "",
            email = "aa@a.com",
            changeEmailLink = "",
            status = "Approved")),
          Html(""),
          cancelAgentUrl,
          currentYear,
          currentTaxYear,
          fromAccountSummary = false
        )

        assert(doc(view).getElementsByClass("govuk-tag govuk-tag--light-blue") !== None)
        assert(doc(view).getElementsByClass("govuk-tag govuk-tag--light-blue").text() === "Pending")
      }

      "have Cancelled agent" in {
        val view = injectedViewInstance(
          currentYearReturnsForDisplay,
          totalCurrentYearReturns = 2,
          summaryReturnsModel(periodKey = currentTaxYear, withPastReturns = true),
          Some(organisationName),
          atedReference,
          Some(ClientMandateDetails(
            agentName = "name1",
            changeAgentLink = "",
            email = "aa@a.com",
            changeEmailLink = "",
            status = "Cancelled")),
          Html(""),
          cancelAgentUrl,
          currentYear,
          currentTaxYear,
          fromAccountSummary = false
        )

        assert(doc(view).getElementsByClass("govuk-tag govuk-tag--orange") !== None)
        assert(doc(view).getElementsByClass("govuk-tag govuk-tag--orange").text() === "Revoked")
      }

      "have Rejected agent" in {
        val view = injectedViewInstance(
          currentYearReturnsForDisplay,
          totalCurrentYearReturns = 2,
          summaryReturnsModel(periodKey = currentTaxYear, withPastReturns = true),
          Some(organisationName),
          atedReference,
          Some(ClientMandateDetails(
            agentName = "name1",
            changeAgentLink = "",
            email = "aa@a.com",
            changeEmailLink = "",
            status = "Rejected")),
          Html(""),
          cancelAgentUrl,
          currentYear,
          currentTaxYear,
          fromAccountSummary = false
        )

        assert(doc(view)getElementsByClass "govuk-tag govuk-tag--red" !== None)
        assert(doc(view).getElementsByClass("govuk-tag govuk-tag--red").text() === "Rejected")
      }

      "have No agent details info" in {
        val view = injectedViewInstance(
          currentYearReturnsForDisplay,
          totalCurrentYearReturns = 2,
          summaryReturnsModel(periodKey = currentTaxYear, withPastReturns = true),
          Some(organisationName),
          atedReference,
          None,
          Html(""),
          cancelAgentUrl,
          currentYear,
          currentTaxYear,
          fromAccountSummary = false
        )

        assert(doc(view).select("#no-agent-info").text() ===
          "You can let an agent represent for ATED using an ATED1 form. However, this authority is limited to ATED and does not extend to other services.")

        assert(doc(view).select("#no-agent-appoint-agent").text() === "Appoint an agent")
        assert(doc(view).select("#no-agent-appoint-agent").attr("href") === "http://localhost:9959/mandate/client/email")

      }
    }

  }
}