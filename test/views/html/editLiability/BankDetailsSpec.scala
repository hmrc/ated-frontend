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
  }

  "Bank details" must {
    "have account name " in {
      doc must haveInputLabelWithText("accountName", messages("ated.bank-details.uk-bank-account.name.label"))
    }

    "have account number" in {
      doc must haveInputLabelWithText("accountNumber", messages("ated.bank-details.uk-bank-account.number.label"))
    }

    "have sort code" in {
      doc must haveInputLabelWithText("sortCode_firstElement", messages("First two numbers"))
      doc must haveInputLabelWithText("sortCode_secondElement", messages("Second two numbers"))
      doc must haveInputLabelWithText("sortCode_thirdElement", messages("Third two numbers"))
    }

    "have iban code" in {
      doc must haveInputLabelWithText("iban", messages("ated.bank-details.non-uk-bank-account.iban.label"))
    }

    "have bic swift code" in {
      doc must haveInputLabelWithText("bicSwiftCode", messages("ated.bank-details.non-uk-bank-account.bic-swift-code.label"))
    }

  }


  private val form = BankDetailForms.bankDetailsForm

  override def view: Html = views.html.editLiability.bankDetails(form, "oldFormBundleNo", Some("backLink"))


}
