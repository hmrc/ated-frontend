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

class HasUkBankAccountSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance: hasUkBankAccount = app.injector.instanceOf[views.html.editLiability.hasUkBankAccount]
  private val form = BankDetailForms.hasUkBankAccountForm
  override def view: Html = injectedViewInstance(form, "oldFormBundleNo", Html(""), Some("backLink"))
  override def doc: Document = Jsoup.parse(view.toString())
  override def doc(view: Html): Document = Jsoup.parse(view.toString())

  "Bank Details view" must {

    behave like pageWithTitle(messages("ated.bank-details.has-uk-bank-account.title"))
    doc.getElementsByTag("h1").text.contains(messages("ated.bank-details.has-uk-bank-account.title"))
    doc.getElementsByTag("h1").text.contains(messages("ated.property-details.pre-header-change"))
    doc.getElementsByClass("govuk-back-link").text === "Back"
    doc.getElementsByClass("govuk-back-link").attr("href") === "http://backLink"

    behave like pageWithContinueButtonAndTextForm("/ated/liability/oldFormBundleNo/change/has-uk-bank-account")
    behave like pageWithYesNoRadioButton("hasUkBankAccount", "hasUkBankAccount-2", messages("ated.label.yes"), messages("ated.label.no"))

    "check page errors for has UK details" in {
      val eform = Form(form.mapping,
        Map("hasUKBankAccount" -> "true"),
        Seq(FormError("hasUkBankAccount", messages("ated.bank-details.error-key.hasUKBankAccount.empty"))), form.value)
       def view: Html = injectedViewInstance(eform, "oldFormBundleNo", Html(""), Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.select("ul.govuk-error-summary__list > li > a").get(0).text() mustBe "Select yes if your bank account is in the UK"
    }
  }

  "Has UK Bank details page" must {
    "have a title " in {
      doc.getElementsByClass("govuk-fieldset__heading").text() mustBe messages("ated.bank-details.has-uk-bank-account.title")
    }

    "have correct set of options to select" in {
      doc.getElementsByClass("govuk-label govuk-radios__label").size() mustBe 2
      doc.getElementsByClass("govuk-label govuk-radios__label").get(0).text() mustBe messages("ated.label.yes")
      doc.getElementsByClass("govuk-label govuk-radios__label").get(1).text() mustBe messages("ated.label.no")
    }

  }

}
