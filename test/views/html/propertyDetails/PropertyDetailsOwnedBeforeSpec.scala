/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.mockito.MockitoSugar
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import utils.viewHelpers.AtedViewSpec
import utils.{MockAuthUtil, PeriodUtils}

class PropertyDetailsOwnedBeforeSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {
  implicit lazy val authContext = organisationStandardRetrievals

  "Property Details Owned Before view" must {
    behave like pageWithTitle(messages("ated.property-details-value.isOwnedBeforeValuationYear.title", PeriodUtils.getValuationYear(2014)))
    behave like pageWithHeader(messages("ated.property-details-value.isOwnedBeforeValuationYear.title", PeriodUtils.getValuationYear(2014)))
    behave like pageWithPreHeading(messages("ated.property-details.pre-header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/owned-before/save//period/2014")
    behave like pageWithYesNoRadioButton("isOwnedBeforePolicyYear-true", "isOwnedBeforePolicyYear-false")

    "check page contents and errors" in {
      val eform = Form(form.mapping, Map("isOwnedBeforePolicyYear" -> "true"),
        Seq(FormError("ownedBeforePolicyYearValue", messages("ated.property-details-value.ownedBeforePolicyYearValue.error.empty")))
        , form.value)
      def view: Html = views.html.propertyDetails.propertyDetailsOwnedBefore("",2014,  eform, None, Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.general.ownedBeforePolicyYearValue")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.ownedBeforePolicyYearValue.error.empty")).hasText mustBe true

      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.ownedBeforevaluationYear.Value")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.ownedBeforevaluationYear.hint")).hasText mustBe true
    }

  }

  private val form = PropertyDetailsForms.propertyDetailsOwnedBeforeForm
  override def view: Html = views.html.propertyDetails.propertyDetailsOwnedBefore("",2014,  form, None, Some("backLink"))

}
