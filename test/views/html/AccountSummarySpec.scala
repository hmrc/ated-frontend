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

package views.html

import config.ApplicationConfig
import models.StandardAuthRetrievals
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
    hasPastReturns = false,
    summaryReturnsModel(periodKey = currentTaxYear),
    Some(address),
    Some(organisationName),
    Html(""),
    Html(""),
    duringPeak = false,
    currentYear,
    currentTaxYear,
    true
  )

  def row(rowNumber: Int) = s"#main-content > div > div.govuk-grid-column-two-thirds > dl > div:nth-child($rowNumber)"

  def checkRowItem(rowNum: Int, col1: String, col2: String, col3: String, href: String): Assertion = {
    assert(doc.select(s"${row(rowNum)} > dt").text() === col1)
    assert(doc.select(s"${row(rowNum)} > dd.govuk-summary-list__value").text() === col2)
    assert(doc.select(s"${row(rowNum)} > dd.govuk-summary-list__actions > a").attr("href") === href)
    assert(doc.select(s"${row(rowNum)} > dd.govuk-summary-list__actions > a").text().contains(col3))
  }

  "AccountSummary" when {

    "regardless of returns data" should {
      "have the correct title" in {
        assert(doc.title() === "Your ATED summary - Submit and view your ATED returns - GOV.UK")
      }

      "have the correct h1" in {
        assert(doc.select("h1").text() contains "Your ATED summary")
      }

      "have the correct valuation date change banner" in {
        assert(doc.select("#valuation-banner1").text() contains "Properties must be revalued every 5 years in line with ATED legislation.")
        assert(doc.select("#valuation-banner2").text() contains "The 2023 to 2024 chargeable period is a revaluation year.")
        assert(doc.select("#valuation-banner3").text() contains "For properties acquired on or before 1 April 2022, use this date as the revaluation date.")
        assert(doc.select("#valuation-banner4").text() contains "For properties acquired after 1 April 2022, use the acquisition date as the valuation date.")
      }

      "have the correct caption" in {
        assert(doc.select(".govuk-caption-l").text() === s"You are logged in as:$organisationName")
      }

      "have the correct banner link" in {
        assert(doc.select(".govuk-header__service-name").attr("href") === "/ated/home")
      }
    }

    "the user has property and relief returns for the current year" should {

      "have the correct heading" in {
        assert(doc.select("#main-content > div > div.govuk-grid-column-two-thirds > h2").text() === "Current year returns")
      }

      "show the returns" in {
        checkRowItem(1, "Change_Liability", "Draft", "View or change", "example/draft/route")
        checkRowItem(2, "19 Stone Row", "Submitted", "View or change", "example/non-draft/route")
      }

      "not show the View all returns link if there are less than 6 returns and no past returns" in {
        assert(doc.select("#view-all-returns").size() === 0)
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
        assert(doc.select(".govuk-hint").text === "Showing 2 of 2 returns")
      }
    }

    "more than 5 returns and no past returns" should {

      lazy val fiveReturns = currentYearReturnsForDisplay++currentYearReturnsForDisplay++Seq(currentYearReturnsForDisplay.head)

      lazy val view = injectedViewInstance(
        fiveReturns,
        totalCurrentYearReturns = 6,
        hasPastReturns = false,
        summaryReturnsModel(periodKey = currentTaxYear, withPastReturns = true),
        Some(address),
        Some(organisationName),
        Html(""),
        Html(""),
        duringPeak = false,
        currentYear,
        currentTaxYear,
        false
      )

      "show the view all returns link" in {
        assert(doc(view).select("#view-all-returns").text === s"View all returns for $currentTaxYear to ${currentTaxYear + 1}")
        assert(doc(view).select("#view-all-returns").attr("href") === s"/ated/period-summary/$currentTaxYear")
      }

      "show content to say how many returns are being displayed" in {
        assert(doc(view).select(".govuk-hint").text === "Showing 5 of 6 returns")
      }

    }

    "less than 6 returns and there is at least 1 past return" should {
      "show the view all returns link" in {
        val view = injectedViewInstance(
          currentYearReturnsForDisplay,
          totalCurrentYearReturns = 2,
          hasPastReturns = true,
          summaryReturnsModel(periodKey = currentTaxYear, withPastReturns = true),
          Some(address),
          Some(organisationName),
          Html(""),
          Html(""),
          duringPeak = false,
          currentYear,
          currentTaxYear,
          false
        )

        assert(doc(view).select("#view-all-returns").text === s"View all returns for $currentTaxYear to ${currentTaxYear + 1}")
        assert(doc(view).select("#view-all-returns").attr("href") === s"/ated/period-summary/$currentTaxYear")
      }

      "show content to say how many returns are being displayed" in {
        assert(doc(view).select(".govuk-hint").text === "Showing 2 of 2 returns")
      }
    }
  }
}
