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
import utils.AtedUtils
import views.html.propertyDetails.propertyDetailsDateOfRevalue

class PropertyDetailsDateOfRevalueSpec extends AtedViewSpec with MockAuthUtil {

  given appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  given authContext: StandardAuthRetrievals = organisationStandardRetrievals

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

      "style the month and year as errors when both are missing" in {
        val form = propertyDetailsDateOfRevalueForm.withError("dateOfRevalue.month", "ated.error.date.monthyear.missing", messages("ated.property-details-value.dateOfRevalue.messageKey"))
        def view: Html = injectedViewInstance("anything", 2024, form, None, HtmlFormat.empty, Some("localhost"))

        doc(view).getElementById("dateOfRevalue.day").className() must not include "govuk-input--error"
        doc(view).getElementById("dateOfRevalue.month").className() must include("govuk-input--error")
        doc(view).getElementById("dateOfRevalue.year").className() must include("govuk-input--error")
      }

      "style only the year as an error when the year is not 4 digits" in {
        val form = propertyDetailsDateOfRevalueForm.withError("dateOfRevalue.year", "ated.error.date.year.length", messages("ated.property-details-value.dateOfRevalue.messageKey"))
        def view: Html = injectedViewInstance("anything", 2024, form, None, HtmlFormat.empty, Some("localhost"))

        doc(view).getElementById("dateOfRevalue.day").className() must not include "govuk-input--error"
        doc(view).getElementById("dateOfRevalue.month").className() must not include "govuk-input--error"
        doc(view).getElementById("dateOfRevalue.year").className() must include("govuk-input--error")
      }

      "style the whole date as an error when nothing is entered" in {
        val form = propertyDetailsDateOfRevalueForm.withError("dateOfRevalue", "ated.error.date.empty", messages("ated.property-details-value.dateOfRevalue.messageKey"))
        def view: Html = injectedViewInstance("anything", 2024, form, None, HtmlFormat.empty, Some("localhost"))

        doc(view).getElementById("dateOfRevalue.day").className() must include("govuk-input--error")
        doc(view).getElementById("dateOfRevalue.month").className() must include("govuk-input--error")
        doc(view).getElementById("dateOfRevalue.year").className() must include("govuk-input--error")
      }
      "when coming from summary page" must {

        val view: Html =
          injectedViewInstance(
            "anything",
            2024,
            propertyDetailsDateOfRevalueForm,
            Some(AtedUtils.EDIT_FROM_SUMMARY),
            HtmlFormat.empty,
            Some("/ated/liability/create/summary")
          )

        val document = doc(view)

        "have the correct pre heading for change return" in {
          document.select(".govuk-caption-l").text() mustBe "This section is: Change return"
        }

        "have the summary page backlink" in {
          document.select("a.govuk-back-link").text() mustBe "Back"
          document.select("a.govuk-back-link").attr("href") mustBe "/ated/liability/create/summary"
        }
      }
    }
  }
}