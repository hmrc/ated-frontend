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

import builders.ChangeLiabilityReturnBuilder
import config.ApplicationConfig
import models.{BankDetails, BankDetailsModel, DisposeLiability, DisposeLiabilityReturn, SortCode, StandardAuthRetrievals}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import uk.gov.hmrc.play.test.UnitSpec

class disposeLiabilitySummarySpec extends UnitSpec with GuiceOneAppPerSuite
  with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val bankDetailsYesButNoDetails: Option[BankDetailsModel] = Some(BankDetailsModel(hasBankDetails = true, bankDetails = None))
  val completedBankDetails: Option[BankDetailsModel] = Some(BankDetailsModel(hasBankDetails = true,
    bankDetails = Some(BankDetails(hasUKBankAccount = Some(true),
      accountName = Some("Account name"),
      accountNumber = Some("12312312"),
      sortCode = Some(SortCode("12","12","12")))))
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
        val html = views.html.editLiability.disposeLiabilitySummary(disposeLiabilityReturn(None), Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.getElementsByClass("button").size() === 0)
        assert(document.getElementById("supply-bank-value").text() === "INCOMPLETE")
      }

      "the user has answered yes to the bank details question but not provided bank details" in {
        val html = views.html.editLiability.disposeLiabilitySummary(disposeLiabilityReturn(bankDetailsYesButNoDetails),
          Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.getElementsByClass("button").size() === 0)
        assert(document.getElementById("type-of-account-value").text() === "INCOMPLETE")
      }
    }

    "show the submit button" when {
      "all required disposal details have been provided" in {
        val html = views.html.editLiability.disposeLiabilitySummary(disposeLiabilityReturn(completedBankDetails),
          Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.getElementsByClass("button").size() === 1)
      }
    }

    "show incomplete next to the disposal date" when {
      "the disposal date has not been provided" in {
        val html = views.html.editLiability.disposeLiabilitySummary(disposeLiabilityReturn(completedBankDetails, None),
          Html(""), Some("http://backLink"))
        val document = Jsoup.parse(html.toString())
        assert(document.getElementById("property-title-disposal-date").text() === "INCOMPLETE")
      }
    }
  }
}
