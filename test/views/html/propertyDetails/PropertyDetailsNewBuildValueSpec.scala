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
import org.joda.time.LocalDate
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class PropertyDetailsNewBuildValueSpec extends AtedViewSpec with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsNewBuildValue]

  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]

  private val form = PropertyDetailsForms.propertyDetailsNewBuildValueForm
  override def view: Html = injectedViewInstance(id, periodKey,  form, None, Html(""), Some("backLink"), testDate)

  val periodKey = 2000
  val testDate = new LocalDate("2020-02-02")
  val testDateString = testDate.toString.replace("-","")
  val testDay = "2"
  val testMonth = "February"
  val testYear = "2020"
  val id = "0"

  "The Property Details New Build Dates page" must {
    "have a the correct page title" in {
      doc.title mustBe TitleBuilder.buildTitle(messages("ated.property-details-value.newBuildValue.title",testDay,testMonth,testYear))
    }
    "have the correct page header" in {
      doc.title mustBe TitleBuilder.buildTitle(messages("ated.property-details-value.newBuildValue.header",testDay,testMonth,testYear))
    }
    "have the correct pre heading" in {
      doc.title(messages("ated.property-details.pre-header"))
    }
    "have a backlink" in {
      doc.getElementsByClass("govuk-back-link").text mustBe "Back"
    }
    "have a continue button" in {
      doc.getElementsByClass("govuk-button").text mustBe "Save and continue"
//      behave like pageWithContinueButtonForm(s"/ated/liability/create/new-build-value/save/$id/period/$periodKey/date/$testDateString")
    }
    "have an input field for the new build value" in {
      doc.getElementsByTag("input") === true
      doc.getElementsByTag("label").text mustBe "What was the value of the property in GBP at this date?"
    }
  }

  "Property Details New Build Value view" must {
    behave like pageWithElement("newBuildValue")
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

}
