/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import forms.PropertyDetailsForms
import models.StandardAuthRetrievals
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}

class PropertyDetailsAcquisitionSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val authMock: Enrolments ~ Some[AffinityGroup] ~ Some[String] = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
  setAuthMocks(authMock)

  "Property Details Acquisition view" must {
    behave like pageWithTitle(messages("ated.property-details-value.anAcquisition.title"))
    behave like pageWithHeader(messages("ated.property-details-value.anAcquisition.header"))
    behave like pageWithPreHeading(messages("ated.property-details.pre-header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/acquisition/save//period/0")
    behave like pageWithYesNoRadioButton("anAcquisition-true", "anAcquisition-false",
      messages("ated.property-details-value.yes"),
      messages("ated.property-details-value.no"))

    "check contents" in {
      doc.getElementsContainingOwnText(messages("ated.property-details-value.anAcquisition.what")).hasText must be(true)

      doc.getElementsContainingOwnText(messages("ated.property-details-value.anAcquisition.what.text1")).hasText must be(true)
      doc.getElementsContainingOwnText(messages("ated.property-details-value.anAcquisition.what.text2")).hasText must be(true)
      doc.getElementById("anAcquisition-text-3").html mustBe messages("ated.property-details-value.anAcquisition.what.text3")
    }

    "check page errors" in {
      doc.getElementsMatchingOwnText(messages("ated.property-details-value.anAcquisition.error-field-name")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-value-error.general.anAcquisition")).hasText mustBe true
    }
  }

  private val form = PropertyDetailsForms.propertyDetailsAcquisitionForm.withError("anAcquisition",
    messages("ated.property-details-value.anAcquisition.error-field-name"))
  override def view: Html = views.html.propertyDetails.propertyDetailsAcquisition("",0,  form, None, Html(""), Some("backLink"))

}
