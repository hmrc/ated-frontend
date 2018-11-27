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

package views.propertyDetails

import forms.PropertyDetailsForms
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import utils.viewHelpers.AtedViewSpec

class propertyDetailsAcquisitionSpec extends AtedViewSpec {

  "Property Details Acquisition view" must {
    behave like pageWithTitle(messages("ated.property-details-value.anAcquisition.title"))
    behave like pageWithHeader(messages("ated.property-details-value.anAcquisition.title"))
    behave like pageWithPreHeading(messages("ated.property-details-value.anAcquisition.header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/acquisition/save//period/0")
    behave like pageWithYesNoRadioButton("anAcquisition-true", "anAcquisition-false")

    "check contents" in {
      doc.getElementsContainingOwnText(messages("ated.property-details-value.anAcquisition.what")).hasText must be(true)

      doc.getElementsContainingOwnText(messages("ated.property-details-value.anAcquisition.what.text1")).hasText must be(true)
      doc.getElementsContainingOwnText(messages("ated.property-details-value.anAcquisition.what.text2")).hasText must be(true)
      doc.getElementById("anAcquisition-text-3").html mustBe messages("ated.property-details-value.anAcquisition.what.text3")
    }

    "check page errors" in new AtedViewSpec {
      val eform = Form(form.mapping, form.data,
        Seq(FormError("anAcquisition", messages("ated.property-details-value.anAcquisition.error-field-name")))
        , form.value)
      override def view: Html = views.html.propertyDetails.propertyDetailsAcquisition("",0,  eform, None, Some("backLink"))

      doc.getElementsMatchingOwnText(messages("ated.property-details-value.anAcquisition.error-field-name")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-value-error.general.anAcquisition")).hasText mustBe true
    }
  }

  private val form = PropertyDetailsForms.propertyDetailsAcquisitionForm
  override def view: Html = views.html.propertyDetails.propertyDetailsAcquisition("",0,  form, None, Some("backLink"))

}
