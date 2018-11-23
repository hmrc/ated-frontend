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
import models.{BicSwiftCode, Iban}
import play.twirl.api.Html
import utils.viewHelpers.AtedViewSpec
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

class BankDetailsSpec extends AtedViewSpec {

  "Bank Details view" must {
    behave like pageWithTitle(messages("ated.bank-details.title"))
    behave like pageWithHeader(messages("ated.bank-details.title"))
    behave like pageWithPreHeading(messages("ated.property-details.pre-header-change"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/oldFormBundleNo/change/bank-details")
    behave like pageWithYesNoRadioButton("hasUKBankAccount-true", "hasUKBankAccount-false")
  }


  /*Form(mapping(
    "hasUKBankAccount" -> optional(boolean).verifying(Messages("ated.bank-details.error-key.hasUKBankAccount.empty"), a => a.isDefined),
    "accountName" -> optional(text),
    "accountNumber" -> optional(text),
    "sortCode" -> sortCodeTuple,
    "bicSwiftCode" -> optional(of[BicSwiftCode]),
    "iban" -> optional(of[Iban])
  )*/

  private val form = BankDetailForms.bankDetailsForm
  override def view: Html = views.html.editLiability.bankDetails(form, "oldFormBundleNo", Some("backLink"))

}
