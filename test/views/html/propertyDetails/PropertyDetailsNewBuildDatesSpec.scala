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

class PropertyDetailsNewBuildDatesSpec extends AtedViewSpec with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]

  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsNewBuildDates]

  val periodKey = 2019

  "Property Details New Build Dates view" must {
    behave like pageWithTitle(messages("ated.property-details-value.newBuildDates.title"))
    behave like pageWithHeader(messages("ated.property-details-value.newBuildDates.header"))
    behave like pageWithElementAndText("paragraphFirst", messages("ated.property-details-value.newBuildDates.paragraphOne"))
    behave like pageWithElementAndText("paragraphTwo", messages("ated.property-details-value.newBuildDates.paragraphTwo"))
    behave like pageWithElement("dateOfFirstOccupation")
    behave like pageWithElement("dateOfLocalCouncilReg")
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm(s"/ated/liability/create/new-build-start/save/0/period/2019")

    "provide an error message for when the dates are empty" in {

    val form = PropertyDetailsForms.propertyDetailsNewBuildDatesForm.withError("newBuildOccupyDate",
      "ated.property-details-value-error.newBuildDates.errorEmpty")
    val newDoc = doc(injectedViewInstance("0", periodKey, form, None, Html(""), Some("backLink")))
    newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.newBuildDates.errorEmpty")).hasText mustBe true

    }

    "provide an error message for when there is an error with the occupied dates" in {
      val form = PropertyDetailsForms.propertyDetailsNewBuildDatesForm.withError("newBuildOccupyDate",
        "ated.property-details-value-error.newBuildDates.occupiedDateError")
      val newDoc = doc(injectedViewInstance("0", periodKey, form, None, Html(""), Some("backLink")))
      newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.newBuildDates.occupiedDateError")).hasText mustBe true
    }

    "provided an error message for when there is an error with the council registration date " in {
      val form = PropertyDetailsForms.propertyDetailsNewBuildDatesForm.withError("newBuildOccupyDate",
        "ated.property-details-value-error.newBuildDates.councilRegError")
      val newDoc = doc(injectedViewInstance("0", periodKey, form, None, Html(""), Some("backLink")))
      newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.newBuildDates.councilRegError")).hasText mustBe true
    }

    "provide an error message for when the dates are too far into the future" in {
      val form = PropertyDetailsForms.propertyDetailsNewBuildDatesForm.withError("newBuildOccupyDate",
        "ated.property-details-value-error.newBuildDates.futureOccupiedError"
      )
      val newDoc = doc(injectedViewInstance("0", periodKey, form, None, Html(""), Some("backLink")))
      newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.newBuildDates.futureOccupiedError")).hasText mustBe true
    }

    "provide an error message for when the registration error is too far into the future" in {
      val form = PropertyDetailsForms.propertyDetailsNewBuildDatesForm.withError("newBuildRegisterDate",
        "ated.property-details-value-error.newBuildDates.futureRegError"
      )
      val newDoc = doc(injectedViewInstance("0", periodKey, form, None, Html(""), Some("backLink")))
      newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.newBuildDates.futureRegError")).hasText mustBe true
    }

    "provide an error message for when the occupied date is invalid" in {
      val form = PropertyDetailsForms.propertyDetailsNewBuildDatesForm.withError("newBuildRegisterDate",
        "ated.property-details-value-error.newBuildDates.invalidOccupiedDateError"
      )
      val newDoc = doc(injectedViewInstance("0", periodKey, form, None, Html(""), Some("backLink")))
      newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.newBuildDates.invalidOccupiedDateError")).hasText mustBe true
    }

    "provide an error message when the registration date is invalid" in {
      val form = PropertyDetailsForms.propertyDetailsNewBuildDatesForm.withError("newBuildRegisterDate",
        "ated.property-details-value-error.newBuildDates.invalidRegDateError"
      )
      val newDoc = doc(injectedViewInstance("0", periodKey, form, None, Html(""), Some("backLink")))
      newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.newBuildDates.invalidRegDateError")).hasText mustBe true
    }
  }

  val form = PropertyDetailsForms.propertyDetailsNewBuildDatesForm

  override def view: Html = injectedViewInstance("0", 2019, form, None, Html(""), Some("backLink"))

}
