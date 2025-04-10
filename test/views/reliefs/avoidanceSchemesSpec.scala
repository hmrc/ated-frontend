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

package views.reliefs

import builders.ReliefBuilder.reliefTaxAvoidance
import config.ApplicationConfig
import forms.ReliefForms._
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import views.html.reliefs

class avoidanceSchemesSpec extends AnyFeatureSpec with GuiceOneAppPerSuite
  with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val periodKey = 2015

  val injectedViewInstance: reliefs.avoidanceSchemes = app.injector.instanceOf[views.html.reliefs.avoidanceSchemes]

  Feature("The user can view the Enter your avoidance scheme number page") {

    info("as a client i want to be able to enter the details of my avoidance scheme")

    Scenario("show the input boxes so the user can enter their avoidance scheme details ") {

      Given("the client has answered 'yes' to an avoidance scheme being used")
      When("The user views the page")

      val html = injectedViewInstance(periodKey, taxAvoidanceForm, Html(""), Some("backLink"))(Some(reliefTaxAvoidance(periodKey)))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Enter your avoidance scheme number")
      assert(document.select("h1").text contains "Enter your avoidance scheme number")

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return")

      Then("There should be a label describing the relief claimed")
      assert(document.getElementById("relief-summary-text").text() === "Reliefs claimed")

      Then("There should be a label with the text Avoidance scheme reference number")
      assert(document.getElementById("relief-summary-scheme-text").text() === "Avoidance scheme reference number")

      Then("There should be a label with text Promoter reference number")
      assert(document.getElementById("relief-summary-scheme-promoter-text").text() === "Promoter reference number")

      Then("The back link is correct")
      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
    }
  }
}
