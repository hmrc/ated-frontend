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

import builders.ChangeLiabilityReturnBuilder
import config.ApplicationConfig
import models._
import java.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import views.html.editLiability.disposeLiabilitySummary

class disposeLiabilitySummarySpec extends PlaySpec with GuiceOneAppPerSuite
  with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance: disposeLiabilitySummary = app.injector.instanceOf[views.html.editLiability.disposeLiabilitySummary]

  val bankDetailsYesButNoDetails: Option[BankDetailsModel] = Some(BankDetailsModel(hasBankDetails = true, bankDetails = None))
  val bankDetailsHasNoBankAccount: Option[BankDetailsModel] = Some(BankDetailsModel(hasBankDetails = false))
  val completedBankDetails: Option[BankDetailsModel] = Some(BankDetailsModel(hasBankDetails = true,
    bankDetails = Some(BankDetails(hasUKBankAccount = Some(true),
      accountName = Some("Account name"),
      accountNumber = Some("12312312"),
      sortCode = Some(SortCode("12","12","12")))))
  )
  val completedBankDetailsOverseas: Option[BankDetailsModel] = Some(BankDetailsModel(hasBankDetails = true,
    bankDetails = Some(BankDetails(hasUKBankAccount = Some(false),
      accountName = Some("Overseas account name"),
      iban = Some(Iban("111222333444555")),
      bicSwiftCode = Some(BicSwiftCode("12345678999")))))
  )

  def disposeLiabilityReturn(bankDetails: Option[BankDetailsModel],
                             disposalDate: Option[LocalDate] = Some(LocalDate.parse("2020-01-01"))
                            ): DisposeLiabilityReturn = DisposeLiabilityReturn(
    id = "123",
    formBundleReturn = ChangeLiabilityReturnBuilder.generateFormBundleReturn,
    disposeLiability = Some(DisposeLiability(dateOfDisposal = disposalDate, periodKey = 2020)),
    calculated = None,
    bankDetails = bankDetails
  )

  "disposeLiabilitySummary" should {
    "hide the submit button and show incomplete for bank details" when {
      "the user has not answered the bank details question" in {
        val html = injectedViewInstance(disposeLiabilityReturn(None), Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.getElementsByClass("govuk-button").size() === 0)
        assert(document.select("#do-you-have-bank-account-incomplete > div > dt").text() === "Bank account to pay a refund")
        assert(document.getElementsByClass("govuk-tag--red").text() === "INCOMPLETE")
      }

      "the user has answered YES to the bank details question but not provided bank details" in {
        val html = injectedViewInstance(disposeLiabilityReturn(bankDetailsYesButNoDetails),
            Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.getElementsByClass("govuk-button").size() === 0)
        assert(document.select("#account-type-incomplete > div > dt").text() === "Bank account to pay a refund")
        assert(document.getElementsByClass("govuk-tag--red").text() === "INCOMPLETE")
      }
    }

    "show the submit button" when {
      "all required disposal details have been provided" in {
        val html = injectedViewInstance(disposeLiabilityReturn(completedBankDetails),
          Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.getElementsByClass("govuk-button").size() === 1)
      }

      "all required disposal details have been provided and the user has a UK bank account" in {
        val html = injectedViewInstance(disposeLiabilityReturn(completedBankDetails),
          Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.getElementsByClass("govuk-button").size() === 1)
        assert(document.select("#bank-details-uk > div:nth-child(1) > dt").text() === "Bank account to pay a refund")
        assert(document.select("#bank-details-uk > div:nth-child(1) > dd.govuk-summary-list__value").text() === "Yes")
        assert(document.select("#bank-details-uk > div:nth-child(2) > dt").text() === "UK bank account")
        assert(document.select("#bank-details-uk > div:nth-child(2) > dd.govuk-summary-list__value").text() === "Yes")
        assert(document.select("#bank-details-uk > div:nth-child(3) > dt").text() === "Account holder name")
        assert(document.select("#bank-details-uk > div:nth-child(3) > dd.govuk-summary-list__value").text() === "Account name")
        assert(document.select("#bank-details-uk > div:nth-child(4) > dt").text() === "Account number")
        assert(document.select("#bank-details-uk > div:nth-child(4) > dd.govuk-summary-list__value").text() === "12312312")
        assert(document.select("#bank-details-uk > div:nth-child(5) > dt").text() === "Sort code")
        assert(document.select("#bank-details-uk > div:nth-child(5) > dd.govuk-summary-list__value").text() === "12 - 12 - 12")
      }

      "all required disposal details have been provided and the user has an overseas bank account" in {
        val html = injectedViewInstance(disposeLiabilityReturn(completedBankDetailsOverseas),
          Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.getElementsByClass("govuk-button").size() === 1)
        assert(document.select("#bank-details-overseas > div:nth-child(1) > dt").text() === "Bank account to pay a refund")
        assert(document.select("#bank-details-overseas > div:nth-child(1) > dd.govuk-summary-list__value").text() === "Yes")
        assert(document.select("#bank-details-overseas > div:nth-child(2) > dt").text() === "UK bank account")
        assert(document.select("#bank-details-overseas > div:nth-child(2) > dd.govuk-summary-list__value").text() === "No")
        assert(document.select("#bank-details-overseas > div:nth-child(3) > dt").text() === "Account holder name")
        assert(document.select("#bank-details-overseas > div:nth-child(3) > dd.govuk-summary-list__value").text() === "Overseas account name")
        assert(document.select("#bank-details-overseas > div:nth-child(4) > dt").text() === "IBAN")
        assert(document.select("#bank-details-overseas > div:nth-child(4) > dd.govuk-summary-list__value").text() === "111222333444555")
        assert(document.select("#bank-details-overseas > div:nth-child(5) > dt").text() === "SWIFT Code")
        assert(document.select("#bank-details-overseas > div:nth-child(5) > dd.govuk-summary-list__value").text() === "1234 56 78 999")
      }

      "all required disposal details have been provided and the user has answered NO to the bank details question" in {
        val html = injectedViewInstance(disposeLiabilityReturn(bankDetailsHasNoBankAccount),
          Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.select("#bank-details-answered-no > div > dt").text() === "Bank account to pay a refund")
        assert(document.select("#bank-details-answered-no > div > dd.govuk-summary-list__value").text() === "No")
        assert(document.getElementsByClass("govuk-button").size() === 1)
      }
    }

    "hide the submit button and show incomplete next to the disposal date" when {
      "the disposal date has not been provided" in {
        val html = injectedViewInstance(disposeLiabilityReturn(completedBankDetails, None),
          Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.getElementsByClass("govuk-button").size() === 0)
        assert(document.getElementsByClass("govuk-tag--red").text() === "INCOMPLETE")
      }
    }
  }
}
