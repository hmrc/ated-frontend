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

import config.ApplicationConfig
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
import views.html.reliefs.reliefDeclaration

class ReliefDeclarationSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val injectedViewInstance: reliefDeclaration = app.injector.instanceOf[views.html.reliefs.reliefDeclaration]
  Feature("The user can view the relief declaration page") {

    implicit val authContext: StandardAuthRetrievals = organisationStandardRetrievals.copy(delegationModel = None)

    info("as a user I want to view the correct page content")

    Scenario("user has created a relief return") {
      Given("A user visits the page")
      When("The user views the page")


      val html = injectedViewInstance(2015, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("Returns declaration")
      assert(document.title() === "Returns declaration - Submit and view your ATED returns - GOV.UK")
      And("The pre-header text is - Create relief return")
      assert(document.getElementsByClass("govuk-heading-xl").text() contains "Returns declaration")
      assert(document.getElementById("relief-declaration-before-declaration-text")
        .text() === "! Warning Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
      assert(document.getElementById("relief-declaration-mid-declaration-text").text() === "Each type of relief claimed is an individual ATED return.")
      assert(document.getElementById("declare-or-confirm").text() === "I declare that:")
      assert(document.getElementById("declaration-confirmation-text-1")
        .text() === "the information I have given on this return (or each of these returns) is correct")
      assert(document.getElementById("declaration-confirmation-text-2")
        .text() === "I am eligible for the reliefs claimed")
      assert(document.getElementsByClass("govuk-button").text() === "Agree and submit returns")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
    }
  }

  Feature("The agent can view the relief declaration page as a client") {

    implicit lazy val authContext: StandardAuthRetrievals = agentStandardRetrievals

    info("as an agent I want to view the correct page content")

    Scenario("agent has created a relief return") {
      Given("An agent visits the page")
      When("The agent views the page")

      val html = injectedViewInstance(2015, Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("Returns declaration")
      assert(document.title() === "Returns declaration - Submit and view your ATED returns - GOV.UK")

      And("The pre-header text is - Create relief return")
      assert(document.getElementsByClass("govuk-heading-xl").text() contains "Returns declaration")
      assert(document.getElementById("relief-declaration-before-declaration-text")
        .text() === "! Warning Before your client’s return or returns can be submitted to HMRC, you must read and agree to the following statement. Your client’s approval may be in electronic or non-electronic form. If your client gives false information, they may have to pay financial penalties and face prosecution.")
      assert(document.getElementById("relief-declaration-mid-declaration-text")
        .text() === "Each type of relief claimed is an individual ATED return.")
      assert(document.getElementById("declare-or-confirm").text() === "I confirm that my client has:")
      assert(document.getElementById("declaration-confirmation-text-1")
        .text() === "approved the information contained in this return (or each of these returns) as being correct")
      assert(document.getElementById("declaration-confirmation-text-2")
        .text() === "confirmed it is complete to the best of their knowledge and belief")
      assert(document.getElementById("declaration-confirmation-text-3")
        .text() === "confirmed they are eligible for the reliefs claimed")
      assert(document.getElementsByClass("govuk-button").text() === "Agree and submit returns")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }
}
