/*
 * Copyright 2021 HM Revenue & Customs
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

class PropertyDetailsInReliefSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsInRelief]

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
"Property Details in-relief view" must {
    behave like pageWithTitle(messages("ated.property-details-period.isInRelief.title"))
    behave like pageWithHeader(messages("ated.property-details-period.isInRelief.header"))
    behave like pageWithPreHeading(messages("ated.property-details.pre-header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/in-relief/save//period/2014")
    behave like pageWithYesNoRadioButton("isInRelief-true", "isInRelief-false",
    messages("ated.property-details-period.yes"),
    messages("ated.property-details-period.no"))

    "check page errors" in {
      doc.getElementsMatchingOwnText(messages("ated.property-details-period.isInRelief.error-field-name")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-period-error.general.isInRelief")).hasText mustBe true
    }
  }

  private val form = PropertyDetailsForms.periodsInAndOutReliefForm.withError("isInRelief",
    messages("ated.property-details-period.isInRelief.error-field-name"))
  override def view: Html = injectedViewInstance("",2014,  form, None, Html(""), Some("backLink"))

}
