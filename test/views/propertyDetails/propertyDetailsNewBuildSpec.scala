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
import forms.PropertyDetailsForms.propertyDetailsNewBuildForm
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import testhelpers.MockAuthUtil
import views.html.propertyDetails.propertyDetailsNewBuild

class propertyDetailsNewBuildSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedView = app.injector.instanceOf[propertyDetailsNewBuild]

  "propertyDetailsHasBeenRevalued" when {

    "radio(yes or no)has been selected" should {
      val view = injectedView("key", 2024,propertyDetailsNewBuildForm, backLink = Some("http://back-link"), mode = None)
      val doc = Jsoup.parse(view.toString)

      "have the correct title" in {
        assert(doc.title() == "Is the property a new build? - Submit and view your ATED returns - GOV.UK")
      }

      "correctly render a backLink" in {
        assert(doc.getElementsByClass("govuk-back-link").first().text == "Back")
        assert(doc.getElementsByClass("govuk-back-link").first().attr("href") == "http://back-link")
      }

      "have the correct heading" in {
        assert(doc.getElementsByTag("h1").text.contains("Is the property a new build?"))
        assert(doc.getElementsByTag("h1").size() == 1)
      }

      "have the correct content" in {
        assert(doc.select("div.govuk-grid-column-two-thirds > p.govuk-body:nth-child(2)").text() == "Select Yes only if:")
        assert(doc.select("div.govuk-grid-column-two-thirds > ul > li:nth-child(1)").text() == "your ATED charge starts this chargeable period")
        assert(doc.select("div.govuk-grid-column-two-thirds > ul > li:nth-child(2)").text() == "the property is newly built or converted from another type of building")
        assert(doc.select("div.govuk-grid-column-two-thirds > p.govuk-body:nth-child(4)").text() == "You may be eligible up to 90 days to file your return and pay ATED. Find out about the 90-day filing rule (opens in new tab)")
        assert(doc.select("div.govuk-grid-column-two-thirds > p.govuk-body:nth-child(4) > a").attr("href") == "https://www.gov.uk/government/publications/stld-annual-tax-on-enveloped-dwellings-ated/annual-tax-on-enveloped-dwellings-returns-guidance#sec2")

      }

      "render 2 radio buttons correctly" in {
        val radios = doc.getElementsByClass("govuk-radios__input")
        assert(radios.size() == 2)
        assert(radios.first().attr("value") == "true")
        assert(radios.get(1).attr("value") == "false")
      }

      "correctly label the radio buttons" in {
        assert(doc.getElementsByAttributeValue("for", "isNewBuild").text() == "Yes")
        assert(doc.getElementsByAttributeValue("for", "isNewBuild-2").text() == "No")
      }

      "render a save and continue button" in {
        assert(doc.getElementsByTag("button").text() == "Save and continue")
      }
    }

    "the page has been submitted but no radio button selected" should {
      val view = injectedView("key", 2024, propertyDetailsNewBuildForm.bind(Map("isNewBuild" -> "")), backLink = Some("http://back-link"), mode = None)
      val doc = Jsoup.parse(view.toString)

      "append 'Error: ' to the title of the page" in {
        assert(doc.title() == "Error: Is the property a new build? - Submit and view your ATED returns - GOV.UK")
      }

      "render an error summary with the correct error message" in {
        assert(doc.getElementsByClass("govuk-error-summary").size() == 1)
        assert(doc.select("h2.govuk-error-summary__title").text() == "There is a problem")
        assert(doc.select("ul.govuk-error-summary__list a").text() == "Select yes if the property is a new build")
      }

      "apply an error class to the form group apply error styling" in {
        assert(doc.select("form > div").hasClass("govuk-form-group--error"))
      }

      "render an error message at the input field" in {
        assert(doc.getElementById("isNewBuild-error").text() == "Error: Select yes if the property is a new build")
      }
    }
  }
}
