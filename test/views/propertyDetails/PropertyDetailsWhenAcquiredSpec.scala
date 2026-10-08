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
import forms.PropertyDetailsForms.propertyDetailsWhenAcquiredDatesForm
import models.{PropertyDetailsWhenAcquiredDates, StandardAuthRetrievals}
import play.api.data.Form
import play.twirl.api.{Html, HtmlFormat}
import testhelpers.{AtedViewSpec, MockAuthUtil}
import utils.AtedUtils
import views.html.propertyDetails.propertyDetailsWhenAcquired

class PropertyDetailsWhenAcquiredSpec extends AtedViewSpec with MockAuthUtil {

  given appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  given authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedViewInstance: propertyDetailsWhenAcquired = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsWhenAcquired]

  val messageKey: String = messages("ated.property-details.whenAcquired.messageKey")

  override def view: Html = injectedViewInstance("anything", 2026, propertyDetailsWhenAcquiredDatesForm, None, HtmlFormat.empty, Some("localhost"))

  def viewWith(form: Form[PropertyDetailsWhenAcquiredDates]): Html =
    injectedViewInstance("anything", 2026, form, None, HtmlFormat.empty, Some("localhost"))

  "The when did you acquire the property view" when {

    "rendered with a valid form" must {
      "style none of the inputs as errors" in {
        doc.getElementById("acquiredDate.day").className() must not include "govuk-input--error"
        doc.getElementById("acquiredDate.month").className() must not include "govuk-input--error"
        doc.getElementById("acquiredDate.year").className() must not include "govuk-input--error"
      }
    }

    "submitted with only a day" must {
      val form = propertyDetailsWhenAcquiredDatesForm.withError("acquiredDate.month", "ated.error.date.monthyear.missing", messageKey)

      "style the month and year as errors and leave the day unstyled" in {
        doc(viewWith(form)).getElementById("acquiredDate.day").className() must not include "govuk-input--error"
        doc(viewWith(form)).getElementById("acquiredDate.month").className() must include("govuk-input--error")
        doc(viewWith(form)).getElementById("acquiredDate.year").className() must include("govuk-input--error")
      }
    }

    "submitted with a year outside the accepted range" must {
      val form = propertyDetailsWhenAcquiredDatesForm.withError("acquiredDate.year", "ated.error.date.notInRange", messageKey)

      "style only the year as an error" in {
        doc(viewWith(form)).getElementById("acquiredDate.day").className() must not include "govuk-input--error"
        doc(viewWith(form)).getElementById("acquiredDate.month").className() must not include "govuk-input--error"
        doc(viewWith(form)).getElementById("acquiredDate.year").className() must include("govuk-input--error")
      }
    }

    "submitted with nothing entered" must {
      val form = propertyDetailsWhenAcquiredDatesForm.withError("acquiredDate", "ated.error.date.empty", messageKey)

      "style all three inputs as errors" in {
        doc(viewWith(form)).getElementById("acquiredDate.day").className() must include("govuk-input--error")
        doc(viewWith(form)).getElementById("acquiredDate.month").className() must include("govuk-input--error")
        doc(viewWith(form)).getElementById("acquiredDate.year").className() must include("govuk-input--error")
      }
    }
    "show change return subheading and summary back link when editing" in {

      val view = injectedViewInstance(
        "anything",
        2026,
        propertyDetailsWhenAcquiredDatesForm,
        Some(AtedUtils.EDIT_SUBMITTED),
        HtmlFormat.empty,
        Some("/ated/liability/create/summary")
      )

      val document = doc(view)

      document.select("h2.govuk-caption-xl").text() mustBe "This section is: Change return"
      document.select("a.govuk-back-link").attr("href") mustBe "/ated/liability/create/summary"
    }
  }
}
