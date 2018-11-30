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

package views.html.propertyDetails

import forms.PropertyDetailsForms
import org.joda.time.LocalDate
import play.api.data.FormError
import play.twirl.api.Html
import utils.viewHelpers.AtedViewSpec

class IsFullTaxPeriodSpec extends AtedViewSpec {

  "isFullTaxPeriod view" must {
    behave like pageWithTitle(messages("ated.property-details-period.isFullPeriod.title"))
    behave like pageWithHeader(messages("ated.property-details-period.isFullPeriod.title"))
    behave like pageWithPreHeading(messages("ated.property-details-period.isFullPeriod.header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/full-tax-period/save//period/0")
    behave like pageWithYesNoRadioButton("isFullPeriod-true", "isFullPeriod-false")

    "check page errors" in {
      doc.getElementsMatchingOwnText(messages("ated.property-details-period.isFullPeriod.error-field-name")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-period-error.general.isFullPeriod")).hasText mustBe true
    }
  }

  private val form = PropertyDetailsForms.isFullTaxPeriodForm.withError("isFullPeriod",
    messages("ated.property-details-period.isFullPeriod.error-field-name"))
  override def view: Html = views.html.propertyDetails.isFullTaxPeriod("",0,  form, new LocalDate, new LocalDate, Some("backLink"))

}
