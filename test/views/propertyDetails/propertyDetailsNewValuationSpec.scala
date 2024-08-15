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

import config.ApplicationConfig
import forms.PropertyDetailsForms.{dateFirstOccupiedForm, propertyDetailsNewValuationForm}
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import testhelpers.MockAuthUtil
import views.html.propertyDetails.propertyDetailsNewValuation

class propertyDetailsNewValuationSpec extends PlaySpec with MockitoSugar with MockAuthUtil with GuiceOneAppPerSuite {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedView = app.injector.instanceOf[propertyDetailsNewValuation]


  "propertyDetailsNewValuationSpec" when {
    "page will " should {
      val view = injectedView(propertyDetailsNewValuationForm, Some("back"))
      val doc = Jsoup.parse(view.toString())
      "have proper tile" in {
        assert(doc.title() == "What is the new valuation of the property? - Submit and view your ATED returns - GOV.UK")
      }
      "render a backLink" in {
        assert(doc.getElementsByClass("govuk-back-link").first().text == "Back")
        assert(doc.getElementsByClass("govuk-back-link").first().attr("href") == "back")
      }

      "have the correct heading" in {
        assert(doc.getElementsByTag("h1").text.contains("What is the new valuation of the property?"))
        assert(doc.getElementsByTag("h1").size() == 1)
      }

      "have the correct section heading" in {
        assert(doc.select("h2.govuk-caption-l").first().text == "This section is Create return")
        assert(doc.select("h2.govuk-caption-l > span").hasClass("govuk-visually-hidden"))
      }
      "have a currency input box" in {
        assert(doc.select(".govuk-input__prefix").text() == "£")
        assert(doc.select(".govuk-input").attr("type") == "text")
      }
      "render a save and continue button" in {
        assert(doc.getElementsByTag("button").text() == "Save and continue")
      }

    }

    "the page has been submitted with empty value" should {
      val view = injectedView(propertyDetailsNewValuationForm.bind(Map("revaluedValue" -> "")), Some("back"))
      val doc = Jsoup.parse(view.toString)

      "append 'Error: ' to the title of the page" in {
        assert(doc.title() == "Error: What is the new valuation of the property? - Submit and view your ATED returns - GOV.UK")
      }

      "render an error summary with the correct error message" in {
        assert(doc.getElementsByClass("govuk-error-summary").size() == 1)
        assert(doc.select("h2.govuk-error-summary__title").text() == "There is a problem")
        assert(doc.select("ul.govuk-error-summary__list a").text() == "Enter a valid property value")
      }

      "apply an error class to the form group apply error styling" in {
        assert(doc.select("form > div").hasClass("govuk-form-group--error"))
      }

      "render an error message at the input field" in {
        assert(doc.getElementById("revaluedValue-error").text() == "Error: Enter a valid property value")
      }

    }

    "the page has been submitted with wrong value" should {
      val view = injectedView(propertyDetailsNewValuationForm.bind(Map("revaluedValue" -> "test data")), Some("back"))
      val doc = Jsoup.parse(view.toString)

      "append 'Error: ' to the title of the page" in {
        assert(doc.title() == "Error: What is the new valuation of the property? - Submit and view your ATED returns - GOV.UK")
      }

      "render an error summary with the correct error message" in {
        assert(doc.getElementsByClass("govuk-error-summary").size() == 1)
        assert(doc.select("h2.govuk-error-summary__title").text() == "There is a problem")
        assert(doc.select("ul.govuk-error-summary__list a").text() == "The value of the property must be an amount of money")
      }

      "apply an error class to the form group apply error styling" in {
        assert(doc.select("form > div").hasClass("govuk-form-group--error"))
      }

      "render an error message at the input field" in {
        assert(doc.getElementById("revaluedValue-error").text() == "Error: The value of the property must be an amount of money")
      }

    }
  }

}
