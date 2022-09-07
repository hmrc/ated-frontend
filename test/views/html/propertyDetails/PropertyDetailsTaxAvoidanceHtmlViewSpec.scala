/*
 * Copyright 2022 HM Revenue & Customs
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

import builders.TitleBuilder
import config.ApplicationConfig
import forms.PropertyDetailsForms
import models.StandardAuthRetrievals
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class PropertyDetailsTaxAvoidanceHtmlViewSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedViewInstance: propertyDetailsTaxAvoidance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsTaxAvoidance]

  private val form = PropertyDetailsForms.propertyDetailsTaxAvoidanceForm
    .withError("isTaxAvoidance", "ated.property-details-period.isTaxAvoidance.error-field-name")

  override def view: Html = injectedViewInstance("", 0, form, None, Html(""), Some("backLink"))

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  "The Property Details Professionally Valued View page" must {

    "have a the correct page title" in {
      doc.title mustBe TitleBuilder.buildErrorTitle(messages("ated.property-details-period.isTaxAvoidance.title"))
    }
    "have the correct page header" in {
      doc.getElementsByTag("h1").text() must include(messages("ated.property-details-period.isTaxAvoidance.header"))
    }
    "have the correct pre heading" in {
      doc.getElementsByClass("govuk-caption-xl").text() === "This section is Change return"
    }
    "have a backlink" in {
      doc.getElementsByClass("govuk-back-link").text mustBe "Back"
    }
    "have the correct text paragraphs" in {
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-question")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-reveal-line-1")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-reveal-line-2")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-info-line-1")).hasText mustBe true
      doc.getElementsContainingOwnText(messages("ated.choose-reliefs.avoidance-info-line-2")).hasText mustBe true
      doc.getElementsByClass("govuk-details__text").html contains messages("ated.choose-reliefs.avoidance-more-info")
    }
    "have a continue button" in {
      doc.getElementsByClass("govuk-button").text mustBe "Save and continue"
    }
    "have a yes/no radio button" in {
      doc.getElementsByAttributeValue("for", "isTaxAvoidance").text() mustBe messages("ated.property-details-value.yes")
      doc.getElementsByAttributeValue("for", "isTaxAvoidance-2").text() mustBe messages("ated.property-details-value.no")
    }

    "check page errors when no radio buttons has been selected" in {
      val eform = Form(form.mapping, form.data,
        Seq(FormError("isTaxAvoidance", messages("ated.property-details-period.isTaxAvoidance.error-field-name")))
        , form.value)

      def view: Html = injectedViewInstance("", 0, eform, None, Html(""), Some("backLink"))

      val errorDoc = doc(view)

      errorDoc.getElementById("isTaxAvoidance-error").text() mustBe ("Error: " + messages("ated.property-details-period.isTaxAvoidance.error-field-name"))
      errorDoc.getElementsByClass("govuk-error-summary__list").text() mustBe (messages("ated.property-details-period.isTaxAvoidance.error-field-name"))
    }

    "check page errors for tax avoidance yes" in {
      val eform = Form(form.mapping, Map("isTaxAvoidance" -> "true"),
        Seq(FormError("taxAvoidanceScheme", messages("ated.property-details-period.taxAvoidanceScheme.error.empty")),
          FormError("taxAvoidancePromoterReference", messages("ated.property-details-period.taxAvoidancePromoterReference.error.empty")))
        , form.value)

      def view: Html = injectedViewInstance("", 0, eform, None, Html(""), Some("backLink"))

      val errorDoc = doc(view)

      errorDoc.getElementById("taxAvoidanceScheme-error").text() mustBe ("Error: " + messages("ated.property-details-period.taxAvoidanceScheme.error.empty"))
      errorDoc.select("#main-content > div > div > div > div > ul > li:nth-child(1) > a").text() mustBe messages("ated.property-details-period.taxAvoidanceScheme.error.empty")
      errorDoc.getElementById("taxAvoidancePromoterReference-error").text() mustBe ("Error: " + messages("ated.property-details-period.taxAvoidancePromoterReference.error.empty"))
      errorDoc.select("#main-content > div > div > div > div > ul > li:nth-child(2) > a").text() mustBe messages("ated.property-details-period.taxAvoidancePromoterReference.error.empty")
    }

  }
}
