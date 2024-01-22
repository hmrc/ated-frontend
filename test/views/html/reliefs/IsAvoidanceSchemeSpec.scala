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

package views.html.reliefs

import config.ApplicationConfig
import forms.ReliefForms.isTaxAvoidanceForm
import models.StandardAuthRetrievals
import org.joda.time.LocalDate
import play.api.test.Injecting
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class IsAvoidanceSchemeSpec extends AtedViewSpec with MockAuthUtil with Injecting {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val mockAppConfig: ApplicationConfig = inject[ApplicationConfig]
  val periodKey = 2017
  val periodStartDate = new LocalDate()
  val injectedView: avoidanceSchemeBeingUsed = inject[views.html.reliefs.avoidanceSchemeBeingUsed]

  def view: Html = injectedView(periodKey, isTaxAvoidanceForm, periodStartDate, Html(""), Some("http://backLink"))

  "The avoidanceSchemeBeingUsed view" when {
    "No data has been entered" must {

      "have the correct page title" in {
        doc.title() mustBe "Is an avoidance scheme being used for any of these reliefs? - Submit and view your ATED returns - GOV.UK"
      }

      "have correct heading" in {
        doc.getElementsByTag("h1").text must include("Is an avoidance scheme being used for any of these reliefs?")
      }

      "have correct caption" in {
        doc.getElementsByClass("govuk-caption-xl").text mustBe "This section is: Create return"
      }

      "have a backLink" in {
        val backLink = new CssSelector("a.govuk-back-link")
        doc must backLink
      }

      behave like pageWithYesNoRadioButton(
        "isAvoidanceScheme",
        "isAvoidanceScheme-2",
        messages("ated.claim-relief.avoidance-scheme.yes"),
        messages("ated.claim-relief.avoidance-scheme.no"))
      behave like pageWithContinueButtonForm(s"/ated/reliefs/$periodKey/avoidance-schemes-used/send")
    }


    "Form is submitted with no data" must {
      def view: Html = injectedView(periodKey, isTaxAvoidanceForm.bind(Map("isAvoidanceScheme" -> "")), periodStartDate, Html(""), Some("http://backLink"))

      "append 'Error: ' to the page title" in {
        doc(view).title mustBe "Error: Is an avoidance scheme being used for any of these reliefs? - Submit and view your ATED returns - GOV.UK"
      }

      "display an error summary" in {
        val errorSummary = new CssSelector("div.govuk-error-summary")
        doc(view) must errorSummary
      }

      "have 'There is a problem' h2" in {
        doc(view).getElementsByTag("h2").first.text mustBe ("There is a problem")
      }

      "have error message in the summary, linking to the field" in {
        val errorLink = doc(view).select("ul.govuk-error-summary__list > li > a")

        errorLink.text mustBe "Select yes if an avoidance scheme is being used for any of these reliefs"
        errorLink.attr("href") mustBe "#isAvoidanceScheme"
      }

      "have an error message displayed at the field" in {
        doc(view).getElementById("isAvoidanceScheme-error").text must include("Select yes if an avoidance scheme is being used for any of these reliefs")
      }
    }
  }
}
