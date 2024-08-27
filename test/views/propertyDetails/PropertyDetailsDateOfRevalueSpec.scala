/*
 * Copyright 2024 HM Revenue & Customs
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

import config.ApplicationConfig
import forms.PropertyDetailsForms.propertyDetailsDateOfRevalueForm
import models.StandardAuthRetrievals
import play.twirl.api.{Html, HtmlFormat}
import testhelpers.{AtedViewSpec, MockAuthUtil}
import views.html.propertyDetails.propertyDetailsDateOfRevalue

class PropertyDetailsDateOfRevalueSpec extends AtedViewSpec with MockAuthUtil {

  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedViewInstance: propertyDetailsDateOfRevalue = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsDateOfRevalue]

  override def view: Html = injectedViewInstance("anything", 2024, propertyDetailsDateOfRevalueForm, None, HtmlFormat.empty, Some("localhost"))

  "The property details revalue date view" when {
    "rendered with a valid form" must {
      "have the correct page title" in {
        doc.title mustBe "What date did you revalue the property? - Submit and view your ATED returns - GOV.UK"
      }

      "have the correct header" in {
        doc.getElementsByTag("h1").text() mustBe "What date did you revalue the property?"
      }

      "have the correct pre heading" in {
        doc.select("h2.govuk-caption-l").text() mustBe "This section is: Create return"
      }

      "have a backlink" in {
        doc.select("a.govuk-back-link").text() mustBe "Back"
        doc.select("a.govuk-back-link").attr("href") mustBe "localhost"
      }

      "have a continue button" in {
        doc.select(".govuk-button").text() mustBe "Save and continue"
      }

      "have a date input" in {
        doc.getElementById("dateOfRevalue").className() mustBe "govuk-date-input"
        doc.getElementById("dateOfRevalue-hint").text() mustBe "For example, 1 4 2024"
      }

    }

    "submitted with an invalid form" must {
      "have the correct errors when no date is provided" in {
        val form = propertyDetailsDateOfRevalueForm.withError("dateOfRevalue", "ated.error.date.empty", messages("ated.property-details-value.dateOfRevalue.messageKey"))
        def view: Html = injectedViewInstance("anything", 2024, form, None, HtmlFormat.empty, Some("localhost"))

        doc(view).getElementsByClass("govuk-error-summary") must not have size(0)
        doc(view).select("ul.govuk-error-summary__list a").attr("href") mustBe "#dateOfRevalue.day"
        doc(view).select("ul.govuk-error-summary__list a").text() mustBe "The date when you revalued the property cannot be empty"
        doc(view).getElementById("dateOfRevalue-error").className() mustBe("govuk-error-message")
        doc(view).getElementById("dateOfRevalue-error").text() mustBe "Error: The date when you revalued the property cannot be empty"
      }

      "have the correct errors when the date is in the future" in {
        val form = propertyDetailsDateOfRevalueForm.withError("dateOfRevalue", "ated.error.date.future", messages("ated.property-details-value.dateOfRevalue.messageKey"))
        def view: Html = injectedViewInstance("anything", 2024, form, None, HtmlFormat.empty, Some("localhost"))

        doc(view).getElementsByClass("govuk-error-summary") must not have size(0)
        doc(view).select("ul.govuk-error-summary__list a").attr("href") mustBe "#dateOfRevalue.day"
        doc(view).select("ul.govuk-error-summary__list a").text() mustBe "The date when you revalued the property cannot be in the future"
        doc(view).getElementById("dateOfRevalue-error").className() mustBe("govuk-error-message")
        doc(view).getElementById("dateOfRevalue-error").text() mustBe "Error: The date when you revalued the property cannot be in the future"
      }

      "have the correct errors when the date is invalid" in {
        val form = propertyDetailsDateOfRevalueForm.withError("dateOfRevalue", "ated.error.date.invalid", messages("ated.property-details-value.dateOfRevalue.messageKey"))
        def view: Html = injectedViewInstance("anything", 2024, form, None, HtmlFormat.empty, Some("localhost"))
        doc(view).getElementsByClass("govuk-error-summary") must not have size(0)
        doc(view).select("ul.govuk-error-summary__list a").attr("href") mustBe "#dateOfRevalue.day"
        doc(view).select("ul.govuk-error-summary__list a").text() mustBe "The date when you revalued the property must be a valid date"
        doc(view).getElementById("dateOfRevalue-error").className() mustBe("govuk-error-message")
        doc(view).getElementById("dateOfRevalue-error").text() mustBe "Error: The date when you revalued the property must be a valid date"
      }
    }
  }
}