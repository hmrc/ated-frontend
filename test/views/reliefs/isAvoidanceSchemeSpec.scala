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
import forms.ReliefForms._
import models.{IsTaxAvoidance, StandardAuthRetrievals}
import org.joda.time.LocalDate
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
import views.html.reliefs.avoidanceSchemeBeingUsed

class isAvoidanceSchemeSpec extends AnyFeatureSpec with GuiceOneAppPerSuite
  with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val periodKey = 2015

  val injectedViewInstance: avoidanceSchemeBeingUsed = app.injector.instanceOf[views.html.reliefs.avoidanceSchemeBeingUsed]

  Feature("The user can view the is avoidance scheme page") {

    info("as a client i want to be able to select whether or not I am using an avoidance scheme")

    Scenario("show the is an avoidance scheme being used radio buttons") {

      Given("the client is creating a new relief and want tell us if an avoidance scheme is being used")
      When("The user views the page")

      val html = injectedViewInstance(periodKey, isTaxAvoidanceForm , new LocalDate("2015-04-01"), Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Is an avoidance scheme being used for any of these reliefs?")
      assert(document.select("h1").text contains "Is an avoidance scheme being used for any of these reliefs?")

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return")

      Then("The the text on the screen should be correct")
      assert(document.getElementsByAttributeValue("for", "isAvoidanceScheme").attr("checked") === "")
      assert(document.getElementsByAttributeValue("for", "isAvoidanceScheme-2").attr("checked") === "")

      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
    }

    Scenario("show the reliefs we have previously chosen") {

      Given("the client is creating a new relief and want to see the options")
      When("The user views the page")

      val isTaxAvoidance = IsTaxAvoidance(isAvoidanceScheme = Some(true))
      val html = injectedViewInstance(periodKey, isTaxAvoidanceForm.fill(isTaxAvoidance),
        new LocalDate("2015-04-01"), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Is an avoidance scheme being used for any of these reliefs?")
      assert(document.select("h1").text contains "Is an avoidance scheme being used for any of these reliefs?")

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return")

      Then("The the 'Yes' button is selected")
      assert(document.getElementById("isAvoidanceScheme").outerHtml() contains "checked")

      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }

}
