/*
 * Copyright 2021 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.TestModels

class AccountSummarySpec extends PlaySpec with MockAuthUtil with GuiceOneAppPerTest with TestModels {

  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = MessagesImpl(Lang("en-GB"), messagesApi)

  lazy val injectedViewInstance = app.injector.instanceOf[views.html.accountSummary]

  when(mockAppConfig.atedPeakStartDay)
    .thenReturn("16")

  lazy val view = injectedViewInstance(
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

  def row(rowNumber: Int) = s"#content > article > dl > div:nth-child($rowNumber)"

  def checkRowItem(rowNum: Int, col1: String, col2: String, col3: String, href: String) = {
    assert(document.select(s"${row(rowNum)} > dt").text() === col1)
    assert(document.select(s"${row(rowNum)} > dd.govuk-summary-list__value").text() === col2)
    assert(document.select(s"${row(rowNum)} > dd.govuk-summary-list__actions > a").attr("href") === href)
    assert(document.select(s"${row(rowNum)} > dd.govuk-summary-list__actions > a").text().contains(col3))
  }

  lazy val document: Document = Jsoup.parse(view.body)

  "AccountSummary" when {

    "regardless of returns data" should {
      "have the correct title" in {
        assert(document.title() === "Your ATED summary - GOV.UK")
      }

      "have the correct h1" in {
        assert(document.select("h1").text() === "Your ATED summary")
      }

      "have the correct banner link" in {
        assert(document.select(".header__menu__proposition-name").attr("href") === "/ated/home")
      }
    }

    "the user has property and relief returns for the current year" should {

      "have the correct heading" in {
        assert(document.select("#content > article > h2").text() === "Current year returns")
      }

      "show the returns" in {
        checkRowItem(1, "Change_Liability", "Draft", "View or change", "example/draft/route")
        checkRowItem(2, "19 Stone Row", "Submitted", "View or change", "example/non-draft/route")
      }

      "not show the View all returns link if there are less than 6 returns and no past returns" in {
        assert(document.select("#view-all-returns").size() === 0)
      }

      "show the Create a new return for current tax year button" in {
        assert(document.select(
          "#create-return-1").text === s"Create a new return for $currentTaxYear to ${currentTaxYear + 1}"
        )
        assert(document.select(
          "#create-return-1").attr("href") === s"/ated/period-summary/$currentTaxYear/createReturn?fromAccountSummary=true"
        )
      }

      "show content to say how many returns are being displayed" in {
        assert(document.select("#content > article > div.govuk-hint").text === "Showing 2 of 2 returns")
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

      lazy val document: Document = Jsoup.parse(view.body)

      "show the view all returns link" in {
        assert(document.select("#view-all-returns").text === s"View all returns for $currentTaxYear to ${currentTaxYear + 1}")
        assert(document.select("#view-all-returns").attr("href") === s"/ated/period-summary/$currentTaxYear")
      }

      "show content to say how many returns are being displayed" in {
        assert(document.select("#content > article > div.govuk-hint").text === "Showing 5 of 6 returns")
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

        lazy val document: Document = Jsoup.parse(view.body)

        assert(document.select("#view-all-returns").text === s"View all returns for $currentTaxYear to ${currentTaxYear + 1}")
        assert(document.select("#view-all-returns").attr("href") === s"/ated/period-summary/$currentTaxYear")

      }

      "show content to say how many returns are being displayed" in {
        assert(document.select("#content > article > div.govuk-hint").text === "Showing 2 of 2 returns")
      }
    }
  }
}
