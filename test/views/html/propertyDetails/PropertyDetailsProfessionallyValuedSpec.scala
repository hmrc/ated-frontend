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
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import utils.viewHelpers.AtedViewSpec

class PropertyDetailsProfessionallyValuedSpec extends AtedViewSpec {

  "Property Details Professionally Valued view" must {
    behave like pageWithTitle(messages("ated.property-details-value.isValuedByAgent.title"))
    behave like pageWithHeader(messages("ated.property-details-value.isValuedByAgent.title"))
    behave like pageWithPreHeading(messages("ated.property-details-value.isValuedByAgent.header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/valued/save//period/0")
    behave like pageWithYesNoRadioButton("isValuedByAgent-true", "isValuedByAgent-false")

    "check page errors" in {
      doc.getElementsMatchingOwnText(messages("ated.property-details-value.isValuedByAgent.error.non-selected")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-value-error.general.isValuedByAgent")).hasText mustBe true
    }
  }

  private val form = PropertyDetailsForms.propertyDetailsProfessionallyValuedForm.withError("isValuedByAgent",
    messages("ated.property-details-value.isValuedByAgent.error.non-selected"))
  override def view: Html = views.html.propertyDetails.propertyDetailsProfessionallyValued("",0,  form, None, Some("backLink"))

}
