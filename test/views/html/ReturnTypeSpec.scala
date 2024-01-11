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

package views.html

import config.ApplicationConfig
import forms.AtedForms.returnTypeForm
import models.StandardAuthRetrievals
import play.api.test.Injecting
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class ReturnTypeSpec extends AtedViewSpec with MockAuthUtil with Injecting {

  implicit val mockAppConfig: ApplicationConfig = inject[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedView = inject[views.html.returnType]
  def view: Html = injectedView(2021, returnTypeForm, Html(""), Some("http://backLink"))


  "The ReturnType view" when {
    "No data has been entered" must {

      "have the correct page title" in {
        doc.title() mustBe "Select a type of return - Submit and view your ATED returns - GOV.UK"
      }

      "have correct heading" in {
        doc.getElementsByTag("h1").text must include("Select a type of return")
      }

      "have correct caption" in {
        doc.getElementsByClass("govuk-caption-xl").text mustBe "This section is Create return"
      }

      "have a backLink" in {
        val backLink = new CssSelector("a.govuk-back-link")
        doc must backLink
      }

      behave like pageWithButtonForm("/ated/return-type?periodKey=2021", messages("ated.return-type.button"))
      behave like pageWithYesNoRadioButton("returnType", "returnType-2",
        messages("ated.return-type.chargeable"),
        messages("ated.return-type.relief-return"))

    }
    "Form is submitted with no data" must {
      def view: Html = injectedView(2021, returnTypeForm.bind(Map("returnType" -> "")), Html(""), Some("http://backLink"))

      "append 'Error: ' to the page title" in {
        doc(view).title mustBe "Error: Select a type of return - Submit and view your ATED returns - GOV.UK"
      }

      "display an error summary" in {
        val errorSummary = new CssSelector("div.govuk-error-summary")
        doc(view) must errorSummary
      }

      "have 'There is a problem' h2" in {
        doc(view).getElementsByTag("h2").first.text mustBe("There is a problem")
      }

      "have error message in the summary, linking to the field" in {
        val errorLink = doc(view).select("ul.govuk-error-summary__list > li > a")

        errorLink.text mustBe "Select an option for type of return"
        errorLink.attr("href") mustBe "#returnType"
      }

      "have an error message displayed at the field" in {
        doc(view).getElementById("returnType-error").text must include("Select an option for type of return")
      }
    }
  }

}
