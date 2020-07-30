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
import org.joda.time.LocalDate
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class PropertyDetailsNewBuildValueSpec extends AtedViewSpec with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsNewBuildValue]

  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]

  val periodKey = 2000
  val testDate = new LocalDate("2020-02-02")
  val testDateString = testDate.toString.replace("-","")
  val testDay = "2"
  val testMonth = "February"
  val testYear = "2020"
  val id = "0"

  "Property Details New Build Value view" must {
    behave like pageWithTitle(messages("ated.property-details-value.newBuildValue.title",testDay,testMonth,testYear))
    behave like pageWithHeader(messages("ated.property-details-value.newBuildValue.header",testDay,testMonth,testYear))
    behave like pageWithElement("newBuildValue")
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm(s"/ated/liability/create/new-build-value/save/$id/period/$periodKey/date/$testDateString")
  }

  "provide an error message for when the value is empty" in {
    val form = PropertyDetailsForms.propertyDetailsNewBuildValueForm.withError("newBuildValue",
      "ated.property-details-value-error.newBuildValue.emptyValue")
    val newDoc = doc(injectedViewInstance("0", periodKey, form, None, Html(""), Some("backLink"), testDate))
    newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.newBuildValue.emptyValue")).hasText mustBe true
  }

  "provide an error message for when the value is invalid" in {
    val form = PropertyDetailsForms.propertyDetailsNewBuildValueForm.withError("newBuildValue",
      "ated.property-details-value-error.newBuildValue.invalidValue")
    val newDoc = doc(injectedViewInstance("0", periodKey, form, None, Html(""), Some("backLink"), testDate))
    newDoc.getElementsMatchingOwnText(messages("ated.property-details-value-error.newBuildValue.invalidValue")).hasText mustBe true
  }

  private val form = PropertyDetailsForms.propertyDetailsNewBuildValueForm
  override def view: Html = injectedViewInstance(id, periodKey,  form, None, Html(""), Some("backLink"), testDate)

}
