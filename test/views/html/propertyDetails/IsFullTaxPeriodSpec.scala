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
import org.joda.time.LocalDate
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class IsFullTaxPeriodSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.isFullTaxPeriod]

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
"isFullTaxPeriod view" must {
    behave like pageWithTitle(messages("ated.property-details-period.isFullPeriod.title"))
    behave like pageWithHeader(messages("ated.property-details-period.isFullPeriod.header"))
    behave like pageWithPreHeading(messages("ated.property-details.pre-header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/full-tax-period/save//period/0")
    behave like pageWithYesNoRadioButton("isFullPeriod-true", "isFullPeriod-false",
      messages("ated.property-details-period.yes"),
      messages("ated.property-details-period.no"))

    "check page errors" in {
      doc.getElementsMatchingOwnText(messages("ated.property-details-period.isFullPeriod.error-field-name")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.property-details-period-error.general.isFullPeriod")).hasText mustBe true
    }
  }

  private val form = PropertyDetailsForms.isFullTaxPeriodForm.withError("isFullPeriod",
    messages("ated.property-details-period.isFullPeriod.error-field-name"))
  override def view: Html = injectedViewInstance("",0,  form, new LocalDate, new LocalDate, Html(""), Some("backLink"))

}
