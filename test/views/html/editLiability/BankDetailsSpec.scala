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

package views.html.editLiability

import config.ApplicationConfig
import forms.BankDetailForms
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class BankDetailsSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance = app.injector.instanceOf[views.html.editLiability.bankDetails]
  private val form = BankDetailForms.bankDetailsForm
  override def view: Html = injectedViewInstance(form, "oldFormBundleNo", Html(""), Some("backLink"))
  override def doc: Document = Jsoup.parse(view.toString())
  override def doc(view: Html): Document = Jsoup.parse(view.toString())

"Bank Details view" must {

    behave like pageWithTitle(messages("ated.bank-details.title"))
  doc.getElementsByTag("h1").text.contains(messages("ated.bank-details.title"))
  doc.getElementsByTag("h1").text.contains(messages("ated.property-details.pre-header-change"))
  doc.getElementsByClass("govuk-back-link").text === "Back"
  doc.getElementsByClass("govuk-back-link").attr("href") === "http://backLink"
//    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/oldFormBundleNo/change/bank-details")
    behave like pageWithYesNoRadioButton("hasUKBankAccount", "hasUKBankAccount-2",
      messages("ated.label.yes"),
      messages("ated.label.no"))

    "check page errors for uk account" in {
      val eform = Form(form.mapping, Map("hasUKBankAccount" -> "true"),
        Seq(FormError("accountName", messages("ated.bank-details.error-key.accountName.empty")),
        FormError("accountNumber", messages("ated.bank-details.error-key.accountNumber.empty")),
        FormError("sortCode", messages("ated.bank-details.error-key.sortCode.empty")))
        , form.value)
       def view: Html = injectedViewInstance(eform, "oldFormBundleNo", Html(""), Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.select("ul.govuk-error-summary__list > li > a").get(0).text() mustBe "You must enter the name of the bank account holder"
      errorDoc.select("ul.govuk-error-summary__list > li > a").get(1).text() mustBe "You must enter an account number"
      errorDoc.select("ul.govuk-error-summary__list > li > a").get(2).text() mustBe "You must enter a sort code"
      errorDoc.getElementById("accountNumber-error").text() mustBe "Error: You must enter an account number"
      errorDoc.getElementById("sortCode-error").text() mustBe "Error: You must enter a sort code"
      errorDoc.getElementById("accountNameUK-error").text() mustBe "Error: You must enter the name of the bank account holder"
    }

    "check page errors for non uk account" in {
      val eform = Form(form.mapping, Map("hasUKBankAccount" -> "false"),
        Seq(FormError("accountName", messages("ated.bank-details.error-key.accountName.empty")),
        FormError("iban", messages("ated.bank-details.error-key.iban.empty")),
        FormError("bicSwiftCode", messages("ated.bank-details.error-key.bicSwiftCode.empty")))
        , form.value)
       def view: Html = injectedViewInstance(eform, "oldFormBundleNo", Html(""), Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.select("ul.govuk-error-summary__list > li > a").get(0).text() mustBe "You must enter the name of the bank account holder"
      errorDoc.select("ul.govuk-error-summary__list > li > a").get(1).text() mustBe "You must enter the IBAN"
      errorDoc.select("ul.govuk-error-summary__list > li > a").get(2).text() mustBe "You must enter the SWIFT code"
      errorDoc.getElementById("bicSwiftCode-error").text() mustBe "Error: You must enter the SWIFT code"
      errorDoc.getElementById("iban-error").text() mustBe "Error: You must enter the IBAN"
      errorDoc.getElementById("accountNameUK-error").text() mustBe "Error: You must enter the name of the bank account holder"
    }
  }

  "Bank details" must {
    "have account name " in {
      doc.select("#name-of-person > div > label").text() mustBe messages("ated.bank-details.uk-bank-account.name.label")
    }

    "have account number" in {
      doc must haveInputLabelWithText("accountNumber", messages("ated.bank-details.uk-bank-account.number.label"))
    }

    "have sort code" in {
      doc must haveInputLabelWithText("sortCode", messages("Sort code"))
    }

    "have iban code" in {
      doc must haveInputLabelWithText("iban", messages("ated.bank-details.non-uk-bank-account.iban.label"))
    }

    "have bic swift code" in {
      doc must haveInputLabelWithText("bicSwiftCode", messages("ated.bank-details.non-uk-bank-account.bic-swift-code.label"))
    }

  }

}
