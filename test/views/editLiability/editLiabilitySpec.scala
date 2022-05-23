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

package views.editLiability

import config.ApplicationConfig
import forms.AtedForms._
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil

class editLiabilitySpec extends AnyFeatureSpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {
  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance = app.injector.instanceOf[views.html.editLiability.editLiability]

Feature("The user can view an edit liability type page") {

    info("as a user I want to view the correct page content")

    Scenario("user has visited the page for the first time") {

      Given("A user visits the page and clicks yes")
      When("The user views the page and clicks yes")

      implicit val request = FakeRequest()
      val html = injectedViewInstance(editLiabilityReturnTypeForm, "formBundleNo", 2015, editAllowed = true, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())
      Then("the page title : Have you disposed of the property?")
      assert(document.title() === "Have you disposed of the property? - GOV.UK")


      assert(document.getElementsByTag("h1").text.contains("This section is: Change return"))

      assert(document.getElementById("editLiabilityType-hint").text() === "This includes sale, de-enveloping or demolishing.")
      assert(document.select("#main-content > div > div > form > div > div.govuk-radios.govuk-radios--inline > div:nth-child(1) > label").text() === "Yes")
      assert(document.select("#main-content > div > div > form > div > div.govuk-radios.govuk-radios--inline > div:nth-child(2) > label").text() === "No")
      assert(document.getElementById("editLiabilityType").text() === "")
      assert(document.getElementById("editLiabilityType-2").text() === "")

      And("the save button is correct")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.select("#main-content > div > div > a.govuk-back-link").text === "Back")
    }

  }

  Feature("The user can edit a the setting to indicate that no value has change") {

    info("The yes option has been clicked may a user")

    Scenario("The user isnt allowed to edit this form bundle") {

      Given("A user visits the page to and they are allowed to edit the data")
      When("The user views the page without an edit option")

      implicit val request = FakeRequest()

      val html = injectedViewInstance(editLiabilityReturnTypeForm, "formBundleNo", 2015, editAllowed = false,  Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("the page title : Have you disposed of the property?")
      assert(document.title() === "Have you disposed of the property? - GOV.UK")

      assert(document.getElementsByTag("h1").text.contains("This section is: Change return"))

      assert(document.getElementById("editliability-text")
        .text() === "Your original return is too complex to edit online. To make any changes contact the ATED helpline.")

      assert(document.getElementById("editLiabilityType-dp") === null)

      assert(document.getElementById("reportDisposeLink").text() === "report the disposal of the property")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }

  }

}
