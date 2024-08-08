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

import builders.TitleBuilder
import config.ApplicationConfig
import forms.PropertyDetailsForms
import models.StandardAuthRetrievals
import java.time.LocalDate
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class PropertyDetailsValueAcquiredSpec extends AtedViewSpec with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]
  val injectedViewInstance: propertyDetailsValueAcquired = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsValueAcquired]
  private val form = PropertyDetailsForms.propertyDetailsValueAcquiredForm
  override def view: Html = injectedViewInstance("0", 0,form, None, Html(""), Some("backLink"), testDate)

  val testDate = LocalDate.parse("2020-03-11")
  val testDay = "11"
  val testMonth = "March"
  val testYear = "2020"
  val testDateString: String = testDate.toString.replace("-","")
  val testURLid = "0"
  val testURLPeriod = "0"

  "The Property Details Professionally Valued View page" must {
    "have a the correct page title" in {
      doc.title mustBe TitleBuilder.buildTitle(messages("ated.property-details-value.valueAcquired.title", testDay , testMonth, testYear))
    }
    "have the correct page header" in {
      doc.getElementsByTag("h1").text() must include (messages("ated.property-details-value.valueAcquired.header",testDay, testMonth, testYear))
    }
    "have the correct pre heading" in {
      doc.getElementsByClass("govuk-caption-xl").text() mustBe ("This section is: " + messages("ated.property-details.pre-header"))
    }
    "have a backlink" in {
      doc.getElementsByClass("govuk-back-link").text() mustBe "Back"
    }
    "have a continue button" in {
      doc.getElementsByClass("govuk-button").text() mustBe "Save and continue"
    }
    "have the correct hint" in {
      doc.getElementById("acquiredValue-hint").text() mustBe "Enter the value in GBP, for example £1,500,000"
    }
    "have the correct date input classes" in {
      doc.getElementById("acquiredValue").className mustBe "govuk-input govuk-input--width-10"
    }
    "have the correct errors when no value has been provided" in {
      val form = PropertyDetailsForms.propertyDetailsValueAcquiredForm.withError("acquiredValue",
        "ated.property-details-value-error.valueAcquired.emptyValue")

      val newDoc = doc(injectedViewInstance("0", 0, form, None, Html(""), Some("backLink"), testDate))

      newDoc.getElementById("acquiredValue-error").text() mustBe ("Error: " + messages("ated.property-details-value-error.valueAcquired.emptyValue"))

    }
    "have the correct error when the value is in an invalid format" in {
      val form = PropertyDetailsForms.propertyDetailsValueAcquiredForm.withError("acquiredValue",
        "ated.property-details-value-error.valueAcquired.invalidValue")

      val newDoc = doc(injectedViewInstance("0", 0,form,None, Html(""), Some("backLink"), testDate))
      newDoc.getElementsByClass("govuk-error-summary__list").text() mustBe messages("ated.property-details-value-error.valueAcquired.invalidValue")
    }
  }
}
