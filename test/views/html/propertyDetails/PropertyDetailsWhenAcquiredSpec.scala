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
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class PropertyDetailsWhenAcquiredSpec extends AtedViewSpec with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]

  "Property Details New Build Value view" must {
    behave like pageWithTitle(messages("ated.property-details-value.whenAcquired.title"))
    behave like pageWithHeader(messages("ated.property-details-value.whenAcquired.header"))
    behave like pageWithElement("whenAcquired")
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm(s"/ated/liability/create/when-acquired/save/0/period/0")

    "return an error when the date is in the future" in {
      val form = PropertyDetailsForms.propertyDetailsWhenAcquiredDatesForm.withError("acquiredDate", "ated.property-details-value-error.whenAcquired.futureDateError")
      val newDoc = doc(views.html.propertyDetails.propertyDetailsWhenAcquired("0", 0, form, None, Some("backLink")))
      newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.whenAcquired.futureDateError")).hasText mustBe true
    }

    "return an error when the date is invalid" in {
      val form = PropertyDetailsForms.propertyDetailsWhenAcquiredDatesForm.withError("acquiredDate", "ated.property-details-value-error.whenAcquired.invalidDateError")
      val newDoc = doc(views.html.propertyDetails.propertyDetailsWhenAcquired("0", 0, form, None, Some("backLink")))
      newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.whenAcquired.invalidDateError")).hasText mustBe true
    }
  }

  private val form = PropertyDetailsForms.propertyDetailsWhenAcquiredDatesForm

  override def view: Html = views.html.propertyDetails.propertyDetailsWhenAcquired("0", 0, form, None, Some("backLink"))

}
