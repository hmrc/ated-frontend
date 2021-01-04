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
import models.{Address, StandardAuthRetrievals}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import testhelpers.MockAuthUtil
import utils.TestModels

class AccountSummarySidebarSpec extends PlaySpec with MockAuthUtil
  with GuiceOneAppPerTest with TestModels with MockitoSugar {

  implicit lazy val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = MessagesImpl(Lang("en-GB"), messagesApi)

  val defaultBalance: Int = -1000

  def createView(balance: Int = defaultBalance, correspondenceAddress: Option[Address] = None): HtmlFormat.Appendable =
    views.html._accountSummary_sideBar(
      Some(BigDecimal(balance)),
      correspondenceAddress,
      None,
      Html("")
    )


  lazy val document: Document = Jsoup.parse(createView().body)

  "AccountSummarySidebar" should {

    when(mockAppConfig.atedPeakStartDay)
      .thenReturn("16")

    "show a link to your ated details if a correspondence address is present" in {

      val document = Jsoup.parse(createView(correspondenceAddress = Some(address)).body)

      document.getElementById("change-details-link").text() must be("View your ATED details")
      document.getElementById("change-details-link").attr("href") must be("/ated/company-details")
    }

    "show the user's balance in credit with relevant content and links" in {
      document.getElementById("sidebar.balance-header").text() must be("Your balance")
      document.getElementById("sidebar.balance-content").text() must be("£1,000 credit")
      document.getElementById("sidebar.balance-info").text() must be("There can be a 24-hour delay before you see any updates to your balance.")
      document.getElementById("sidebar.link-text").text() must be("Ways to be paid")
      document.getElementById("sidebar.link-text").attr("href") must be("https://www.gov.uk/guidance/pay-annual-tax-on-enveloped-dwellings")
    }

    "show the user's balance in debit with relevant content and links" in {

      val document = Jsoup.parse(createView(1000).body)

      document.getElementById("sidebar.balance-header").text() must be("Your balance")
      document.getElementById("sidebar.balance-content").text() must be("£1,000 debit")
      document.getElementById("sidebar.link-text").text() must be("Deadlines and ways to pay")
      document.getElementById("sidebar.balance-info").text() must be("There can be a 24-hour delay before you see any updates to your balance.")
      document.getElementById("sidebar.link-text").attr("href") must be("https://www.gov.uk/guidance/pay-annual-tax-on-enveloped-dwellings")
    }

    "show the user's balance when it's zero" in {

      val document = Jsoup.parse(createView(0).body)

      document.getElementById("sidebar.balance-header").text() must be("Your balance")
      document.getElementById("sidebar.balance-content").text() must be("£0")
    }

    "have a link for creating returns for other years" in {
      assert(document.select("#create-return-other").text() === "Create, view or change returns for other years")
      assert(document.select("#create-return-other").attr("href") === "/ated/prev-period-summary")
    }
  }
}
