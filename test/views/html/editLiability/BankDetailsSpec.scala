/*
 * Copyright 2018 HM Revenue & Customs
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

import forms.BankDetailForms
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import utils.viewHelpers.AtedViewSpec

class BankDetailsSpec extends AtedViewSpec {

  "Bank Details view" must {
    behave like pageWithTitle(messages("ated.bank-details.title"))
    behave like pageWithHeader(messages("ated.bank-details.title"))
    behave like pageWithPreHeading(messages("ated.property-details.pre-header-change"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/oldFormBundleNo/change/bank-details")
    behave like pageWithYesNoRadioButton("hasUKBankAccount-true", "hasUKBankAccount-false")

    "check contents" in {
      doc.getElementsMatchingOwnText(messages("ated.bank-details.uk-bank-account.name.label")).hasText must be(true)
      doc.getElementsMatchingOwnText(messages("ated.bank-details.uk-bank-account.number.label")).hasText must be(true)
      doc.getElementsMatchingOwnText(messages("ated.bank-details.uk-bank-account.sort-code.label")).hasText must be(true)
    }

    "check page errors" in {
      doc.getElementsMatchingOwnText(messages("ated.bank-details.error-key.accountName.empty")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.bank-details.error-key.accountName")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.bank-details.error-key.accountNumber.empty")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.bank-details.error-key.accountNumber")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.bank-details.error-key.sortCode.empty")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.bank-details.error-key.sortCode")).hasText mustBe true
    }
  }


  private val form = BankDetailForms.bankDetailsForm
  val eform = Form(form.mapping, form.data, Seq(FormError("accountName", messages("ated.bank-details.error-key.accountName.empty")),
    FormError("accountNumber", messages("ated.bank-details.error-key.accountNumber.empty")),
    FormError("sortCode", messages("ated.bank-details.error-key.sortCode.empty")))
    , form.value)
  override def view: Html = views.html.editLiability.bankDetails(eform, "oldFormBundleNo", Some("backLink"))

}
