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
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class PropertyDetailsNewBuildSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance: propertyDetailsNewBuild = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsNewBuild]

  private val form = PropertyDetailsForms.propertyDetailsNewBuildForm
  override def view: Html = injectedViewInstance("",0,  form, None, Html(""), Some("backLink"))

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  "The Property Details Professionally Valued View page" must {
    "have a the correct page title" in {
      doc.title mustBe TitleBuilder.buildTitle(messages("ated.property-details-value.isNewBuild.title"))
    }
    "have the correct page header" in {
      doc.getElementsByTag("h1").text() must include (messages("ated.property-details-value.isNewBuild.header"))
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
    "have a yes/no radio button" in {
      doc.getElementsByAttributeValue("for","isNewBuild").text() mustBe messages("ated.property-details-value.yes")
      doc.getElementsByAttributeValue("for","isNewBuild-2").text() mustBe messages("ated.property-details-value.no")
    }
  }

}
