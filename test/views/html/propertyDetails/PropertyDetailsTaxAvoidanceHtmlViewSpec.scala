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

import config.ApplicationConfig
import forms.PropertyDetailsForms
import testhelpers.{AtedViewSpec, MockAuthUtil}
import models.StandardAuthRetrievals
import org.scalatest.mockito.MockitoSugar
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.twirl.api.Html

class PropertyDetailsTaxAvoidanceHtmlViewSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil{

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]

  "Property Details TaxAvoidance view" must {
    behave like pageWithTitle(messages("ated.property-details-period.isTaxAvoidance.title"))
    behave like pageWithHeader(messages("ated.property-details-period.isTaxAvoidance.header"))
    behave like pageWithPreHeading(messages("ated.property-details.pre-header"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability/create/tax-avoidance/save//period/0")
    behave like pageWithYesNoRadioButton("isTaxAvoidance-true", "isTaxAvoidance-false",
      messages("ated.property-details-period.yes"),
      messages("ated.property-details-period.no"))

    "check contents" in {
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-question")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-reveal-line-1")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-reveal-line-2")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-info-line-1")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-info-line-2")).hasText mustBe true
      doc.getElementById("moreINfoOnTaxAvoidance").html mustBe messages("ated.choose-reliefs.avoidance-more-info")
    }

    "check page errors for tax avoidance" in {
      val eform = Form(form.mapping, form.data,
        Seq(FormError("isTaxAvoidance", messages("ated.property-details-period.isTaxAvoidance.error-field-name")))
        , form.value)

      def view: Html = views.html.propertyDetails.propertyDetailsTaxAvoidance("", 0, eform, None, Some("backLink"))

      val errorDoc = doc(view)

      errorDoc must haveErrorNotification(messages("ated.property-details-period.isTaxAvoidance.error-field-name"))
      errorDoc must haveErrorSummary(messages("ated.property-details-period-error.general.isTaxAvoidance"))
    }

    "check page errors for tax avoidance yes" in {
      val eform = Form(form.mapping, Map("isTaxAvoidance" -> "true"),
        Seq(FormError("taxAvoidanceScheme", messages("ated.property-details-period.taxAvoidanceScheme.error.empty")),
          FormError("taxAvoidancePromoterReference", messages("ated.property-details-period.taxAvoidancePromoterReference.error.empty")))
        , form.value)

      def view: Html = views.html.propertyDetails.propertyDetailsTaxAvoidance("", 0, eform, None, Some("backLink"))

      val errorDoc = doc(view)

      errorDoc must haveErrorNotification(messages("ated.property-details-period.taxAvoidanceScheme.error.empty"))
      errorDoc must haveErrorSummary(messages("ated.property-details-period-error.general.taxAvoidanceScheme"))
      errorDoc must haveErrorNotification(messages("ated.property-details-period.taxAvoidancePromoterReference.error.empty"))
      errorDoc must haveErrorSummary(messages("ated.property-details-period-error.general.taxAvoidancePromoterReference"))
    }

    "check error message for empty form" in {

      doc must haveErrorNotification(messages("ated.property-details-period.isTaxAvoidance.error-field-name"))
      doc must haveErrorSummary(messages("ated.property-details-period-error.general.isFullPeriod"))
    }
  }

  private val form = PropertyDetailsForms.propertyDetailsTaxAvoidanceForm
    .withError("isTaxAvoidance", "ated.property-details-period.isTaxAvoidance.error-field-name")

  override def view: Html = views.html.propertyDetails.propertyDetailsTaxAvoidance("", 0, form, None, Some("backLink"))

}
