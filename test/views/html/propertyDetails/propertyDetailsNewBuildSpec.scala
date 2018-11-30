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

class propertyDetailsNewBuildSpec extends AtedViewSpec {

  "Property details New Build view" must {
    behave like pageWithTitle(messages("ated.property-details-value.isNewBuild.title"))
    behave like pageWithHeader(messages("ated.property-details-value.isNewBuild.header"))
    behave like pageWithPreHeading(messages("ated.property-details.pre-header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/new-build/save//period/0")
    behave like pageWithYesNoRadioButton("isNewBuild-true", "isNewBuild-false")

    "check page errors for uk account" in {

      val eform = Form(form.mapping, Map("isNewBuild" -> "true"),
        Seq(FormError("newBuildDate", messages("ated.property-details-value.newBuildDate.error.empty")),
        FormError("localAuthRegDate", messages("ated.property-details-value.localAuthRegDate.error.empty")),
        FormError("newBuildValue", messages("ated.property-details-value.newBuildValue.error.empty")))
        , form.value)
       def view: Html = views.html.propertyDetails.propertyDetailsNewBuild("",0,  eform, None, Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.newBuildDate.error.empty")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.general.newBuildDate")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.localAuthRegDate.error.empty")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.general.localAuthRegDate")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.newBuildValue.error.empty")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.general.newBuildValue")).hasText mustBe true

      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.newBuildDate")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.localAuthRegDate")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.newBuildValue")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.newBuildValue.hint")).hasText mustBe true

    }

    "check page errors for non uk account" in {

      val eform = Form(form.mapping, Map("isNewBuild" -> "false"),
        Seq(FormError("notNewBuildDate", messages("ated.property-details-value.notNewBuildDate.error.empty")),
        FormError("notNewBuildValue", messages("ated.property-details-value.notNewBuildValue.error.empty")))
        , form.value)
       def view: Html = views.html.propertyDetails.propertyDetailsNewBuild("",0,  eform, None, Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.general.notNewBuildDate")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.notNewBuildDate.error.empty")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.general.notNewBuildValue")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.notNewBuildValue.error.empty")).hasText mustBe true

      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.notNewBuildDate")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.notNewBuildDate.hint")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.notNewBuildValue")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.notNewBuildValue.hint")).hasText mustBe true
    }
  }

  private val form = PropertyDetailsForms.propertyDetailsNewBuildForm
  override def view: Html = views.html.propertyDetails.propertyDetailsNewBuild("",0,  form, None, Some("backLink"))

}
