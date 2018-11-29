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

class propertyDetailsTaxAvoidanceSpec extends AtedViewSpec {

  "Property Details TaxAvoidance view" must {
    behave like pageWithTitle(messages("ated.property-details-period.isTaxAvoidance.title"))
    behave like pageWithHeader(messages("ated.property-details-period.isTaxAvoidance.title"))
    behave like pageWithPreHeading(messages("ated.property-details-period.isTaxAvoidance.header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/tax-avoidance/save//period/0")
    behave like pageWithYesNoRadioButton("isTaxAvoidance-true", "isTaxAvoidance-false")

    "check contents" in {
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-question")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-reveal-line-1")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-reveal-line-2")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-info-line-1")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-info-line-2")).hasText mustBe true
      doc.getElementById("moreINfoOnTaxAvoidance").html mustBe messages("ated.choose-reliefs.avoidance-more-info")
    }

    "check page errors for tax avoidance" in new AtedViewSpec {
      val eform = Form(form.mapping, form.data,
        Seq(FormError("isTaxAvoidance", messages("ated.property-details-period.isTaxAvoidance.error-field-name")))
        , form.value)
      override def view: Html = views.html.propertyDetails.propertyDetailsTaxAvoidance("",0,  eform, None, Some("backLink"))

      doc.getElementsMatchingOwnText(messages("ated.property-details-period.isTaxAvoidance.error-field-name")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-period-error.general.isTaxAvoidance")).hasText mustBe true
    }

    "check page errors for tax avoidance yes" in new AtedViewSpec {
      val eform = Form(form.mapping, Map("isTaxAvoidance" -> "true"),
        Seq(FormError("taxAvoidanceScheme", messages("ated.property-details-period.taxAvoidanceScheme.error.empty")),
          FormError("taxAvoidancePromoterReference", messages("ated.property-details-period.taxAvoidancePromoterReference.error.empty")))
        , form.value)
      override def view: Html = views.html.propertyDetails.propertyDetailsTaxAvoidance("",0,  eform, None, Some("backLink"))

      doc.getElementsMatchingOwnText(messages("ated.property-details-period.taxAvoidanceScheme.error.empty")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-period-error.general.taxAvoidanceScheme")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-period.taxAvoidancePromoterReference.error.empty")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-period-error.general.taxAvoidancePromoterReference")).hasText mustBe true
    }
  }

  private val form = PropertyDetailsForms.propertyDetailsTaxAvoidanceForm
  override def view: Html = views.html.propertyDetails.propertyDetailsTaxAvoidance("",0,  form, None, Some("backLink"))

}
