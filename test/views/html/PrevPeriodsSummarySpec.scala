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
import models.{StandardAuthRetrievals, SummaryReturnsModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import testhelpers.MockAuthUtil
import utils.{PeriodUtils, TestModels}

class PrevPeriodsSummarySpec extends PlaySpec with MockAuthUtil with GuiceOneAppPerTest with TestModels {

  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = MessagesImpl(Lang("en-GB"), messagesApi)
  when(mockAppConfig.atedPeakStartDay)
    .thenReturn("16")

  lazy val injectedViewInstance: prevPeriodsSummary = app.injector.instanceOf[views.html.prevPeriodsSummary]

  val periodKey2015: Int = 2015
  lazy val currentPeriod: Int = PeriodUtils.calculatePeakStartYear()
  val data: SummaryReturnsModel = summaryReturnsModel(periodKey = periodKey2015, withPastReturns = true)
  val currentPeriodDataOnly: SummaryReturnsModel = summaryReturnsModelCurrentOnly(periodKey = currentPeriod)
  val currentPeriodData: SummaryReturnsModel = summaryReturnsModel(periodKey = currentPeriod)


  lazy val view: HtmlFormat.Appendable = injectedViewInstance(
    data,
    Some(address),
    Some(organisationName),
    Html(""),
    Html(""),
    duringPeak = false,
    currentYear,
    currentTaxYear,
    Some("http://backLink")
  )


  lazy val document: Document = Jsoup.parse(view.body)

  "PreviousSummary" when {

    "regardless of returns data" should {
      "have the correct title" in {
        assert(document.title() === "Your previous returns - Submit and view your ATED returns - GOV.UK")
      }

      "have the correct h1" in {
        assert(document.select("h1").text() contains "Your previous returns")
      }

      "have the correct caption" in {
        assert(document.getElementsByClass("govuk-caption-xl").text === s"You have logged in as:$organisationName")
      }

      "have the correct backlink" in {
        assert(document.getElementsByClass("govuk-back-link").text === "Back")
        assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
      }
    }

    "the user has property and relief returns for the previous years" should {
      "show the Create a new return button" in {
        assert(document.getElementById("create-return").text() === s"Create a new return"
        )
        assert(document.getElementById("create-return").attr("href") === s"/ated/period/select"
        )
      }
    }

    "current period only" should {
      "show the you have no returns" in {
        lazy val view = injectedViewInstance(
          currentPeriodDataOnly,
          Some(address),
          Some(organisationName),
          Html(""),
          Html(""),
          duringPeak = false,
          currentYear,
          currentTaxYear,
          None
        )

        lazy val document: Document = Jsoup.parse(view.body)

        assert(document.getElementsByClass("govuk-caption-xl").text === s"You have logged in as:$organisationName")
        assert(document.getElementsByTag("h1").text contains s"Create an ATED return for " +
          s"${PeriodUtils.calculatePeakStartYear()-1} to ${PeriodUtils.calculatePeakStartYear()} or earlier")
      }
    }

    "with past returns" should {
      "show Returns for past periods" in {
        lazy val view = injectedViewInstance(
          data,
          Some(address),
          Some(organisationName),
          Html(""),
          Html(""),
          duringPeak = false,
          currentYear,
          currentTaxYear,
          None
        )

        lazy val document: Document = Jsoup.parse(view.body)

        assert(document.getElementById("view-change-0").text.contains("Returns for 2014 to 2015"))
        assert(document.getElementById("view-change-0").attr("href") === "/ated/period-summary/2014")

        assert(document.getElementById("charge-number-0").text.contains("2"))
        assert(document.getElementById("draft-number-0").text.contains("2"))
        assert(document.getElementById("reliefs-number-0").text.contains("1"))
      }
    }

    "with Current and past returns" should {
      "Only show current year -1 returns" in {

        lazy val view = injectedViewInstance(
          currentPeriodData,
          Some(address),
          Some(organisationName),
          Html(""),
          Html(""),
          duringPeak = false,
          currentYear,
          currentTaxYear,
          None
        )

        lazy val document: Document = Jsoup.parse(view.body)

        assert(document.getElementById("view-change-0").text.contains(s"Returns for ${PeriodUtils.calculatePeakStartYear() - 1} to ${PeriodUtils.calculatePeakStartYear()}"))
        assert(document.getElementById("view-change-0").attr("href") === s"/ated/period-summary/${PeriodUtils.calculatePeakStartYear() - 1}")

        assert(document.getElementById("charge-number-0").text.contains("1"))
        assert(document.getElementById("draft-number-0").text.contains("2"))
        assert(document.getElementById("reliefs-number-0").text.contains("1"))

      }
    }
  }
}
