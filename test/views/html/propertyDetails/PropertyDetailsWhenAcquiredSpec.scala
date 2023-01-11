/*
 * Copyright 2023 HM Revenue & Customs
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
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class PropertyDetailsWhenAcquiredSpec extends AtedViewSpec with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]
  val injectedViewInstance: propertyDetailsWhenAcquired = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsWhenAcquired]

  private val form = PropertyDetailsForms.propertyDetailsWhenAcquiredDatesForm

  override def view: Html = injectedViewInstance("0", 0, form, None, Html(""), Some("backLink"))

  "The property details acquisition view for a valid form" must {
    "have the correct page title" in {
      doc.title mustBe (messages("ated.property-details-value.whenAcquired.title") + " - GOV.UK")
    }

    "have the correct header" in {
      doc.getElementsByTag("h1").text() must include (messages("ated.property-details-value.whenAcquired.header"))
    }

    "have the correct pre heading" in {
      doc.getElementsByClass("govuk-caption-xl").text() mustBe ("This section is " + messages("ated.property-details.pre-header"))
    }

    "have a backlink" in {
      doc.getElementsByClass("govuk-back-link").text() mustBe "Back"
    }

    "have a continue button" in {
      doc.getElementsByClass("govuk-button").text() mustBe "Save and continue"
    }

    "have a date input" in {
      doc.getElementById("acquiredDate").className mustBe "govuk-date-input"
    }

    "have the correct errors when the date is in the future" in {
      val form = PropertyDetailsForms.propertyDetailsWhenAcquiredDatesForm.withError("acquiredDate", "ated.property-details-value-error.whenAcquired.futureDateError")
      val newDoc = doc(injectedViewInstance("0", 0, form, None, Html(""), Some("backLink")))
      newDoc.getElementById("acquiredDate-error").text() mustBe ("Error: " + messages("ated.property-details-value-error.whenAcquired.futureDateError"))
    }
    "have the correct errors when the date is invalid" in {
      val form = PropertyDetailsForms.propertyDetailsWhenAcquiredDatesForm.withError("acquiredDate", "ated.property-details-value-error.whenAcquired.invalidDateError")
      val newDoc = doc(injectedViewInstance("0", 0, form, None, Html(""), Some("backLink")))
      newDoc.getElementsByClass("govuk-error-summary__list").text() mustBe messages("ated.property-details-value-error.whenAcquired.invalidDateError")
    }
  }
}
