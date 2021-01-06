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
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class PropertyDetailsValueAcquiredSpec extends AtedViewSpec with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsValueAcquired]

  val testDate = new LocalDate("2020-03-11")
  val testDay = "11"
  val testMonth = "March"
  val testYear = "2020"
  val testDateString = testDate.toString.replace("-","")
  val testURLid = "0"
  val testURLPeriod = "0"


  "Property Details Value Acquired Value view" must {
    behave like pageWithTitle(messages("ated.property-details-value.valueAcquired.title", testDay , testMonth, testYear))
    behave like pageWithHeader(messages("ated.property-details-value.valueAcquired.header",testDay, testMonth, testYear))
    behave like pageWithElement("acquiredValue")
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm(s"/ated/liability/create/value-acquired/save/$testURLid/period/$testURLPeriod/date/$testDateString")
  }

  "return an error when no value has been provided" in {
    val form = PropertyDetailsForms.propertyDetailsValueAcquiredForm.withError("acquiredValue",
      "ated.property-details-value-error.valueAcquired.emptyValue")

    val newDoc = doc(injectedViewInstance("0", 0, form, None, Html(""), Some("backLink"), testDate))

    newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.valueAcquired.emptyValue")).hasText mustBe true

  }

  "return an error when the value is in an invalid format" in {
    val form = PropertyDetailsForms.propertyDetailsValueAcquiredForm.withError("acquiredValue",
      "ated.property-details-value-error.valueAcquired.invalidValue")

    val newDoc = doc(injectedViewInstance("0", 0,form,None, Html(""), Some("backLink"), testDate))
    newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.valueAcquired.invalidValue")).hasText mustBe true
  }

  private val form = PropertyDetailsForms.propertyDetailsValueAcquiredForm
  override def view: Html = injectedViewInstance("0", 0,form, None, Html(""), Some("backLink"), testDate)

}
