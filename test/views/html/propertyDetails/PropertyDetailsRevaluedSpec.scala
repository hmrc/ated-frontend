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
import models.StandardAuthRetrievals
import org.scalatest.mockito.MockitoSugar
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import utils.MockAuthUtil
import utils.viewHelpers.AtedViewSpec

class PropertyDetailsRevaluedSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit lazy val authContext = organisationStandardRetrievals

  "Property Details Revalued view" must {
    behave like pageWithTitle(messages("ated.property-details-value.isPropertyRevalued.title"))
    behave like pageWithHeader(messages("ated.property-details-value.isPropertyRevalued.header"))
    behave like pageWithPreHeading(messages("ated.property-details.pre-header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/revalued/save//period/0")
    behave like pageWithYesNoRadioButton("isPropertyRevalued-true", "isPropertyRevalued-false")

    "check page errors" in {
      doc.getElementsMatchingOwnText(messages("ated.property-details-value.isPropertyRevalued.error.non-selected")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-value-error.general.isPropertyRevalued")).hasText mustBe true
    }
  }

  private val form = PropertyDetailsForms.propertyDetailsRevaluedForm.withError("isPropertyRevalued",
    messages("ated.property-details-value.isPropertyRevalued.error.non-selected"))

  override def view: Html = views.html.propertyDetails.propertyDetailsRevalued("",0,  form, None, Some("backLink"))

}
