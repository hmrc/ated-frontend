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

package views.propertyDetails

import config.ApplicationConfig
import config.featureswitch.FeatureSwitch
import forms.PropertyDetailsForms._
import models.{PeriodChooseRelief, StandardAuthRetrievals}
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.ReliefsUtils

class periodChooseReliefSpec extends AnyFeatureSpecLike with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.periodChooseRelief]

Feature("The user can add a period that the property is in relief") {

    info("as a client i want to indicate when my property is in relief")

    Scenario("allow selecting a relief") {

      Given("the client is adding a relief")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015, periodChooseReliefForm, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Select the type of relief")
      assert(document.select("h1").text contains "Select the type of relief")

      Then("The subheader should be - Create return")
//      assert(document.getElementsByTag("h1").text() contains  "This section is: Create return")

      assert(document.getElementById("reliefDescription").attributes().get("value") === "Property rental businesses")
      assert(document.getElementById("reliefDescription-2").attributes().get("value") === "Dwellings opened to the public")
      assert(document.getElementById("reliefDescription-3").attributes().get("value") === "Property developers")
      assert(document.getElementById("reliefDescription-4").attributes().get("value") === "Property traders carrying on a property trading business")
      assert(document.getElementById("reliefDescription-5").attributes().get("value") === "Financial institutions acquiring dwellings in the course of lending")
      assert(document.getElementById("reliefDescription-6").attributes().get("value") === "Dwellings used for trade purposes")
      assert(document.getElementById("reliefDescription-7").attributes().get("value") === "Farmhouses")
      assert(document.getElementById("reliefDescription-8").attributes().get("value") === "Registered providers of Social Housing")
      assert(document.getElementById("reliefDescription-9").attributes().get("value") === "Equity Release Scheme")

      Then("The submit button should have the correct name")
      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
    }

  Scenario("allow selecting a relief in 2020 with the social housing feature switch") {

    Given("the client is adding a relief")
    When("The user views the page")

    mockAppConfig.enable(FeatureSwitch.CooperativeHousing)

    val html = injectedViewInstance("1", 2020, periodChooseReliefForm, Html(""), Some("backLink"))

    val document = Jsoup.parse(html.toString())
    Then("The header should match - Select the type of relief")
    assert(document.select("h1").text contains "Select the type of relief")

    Then("The subheader should be - Create return")
//    assert(document.getElementsByTag("h1").text() contains  "This section is: Create return")

    Then("The correct radio buttons for relief types should be present")

    assert(document.getElementById("reliefDescription").attributes().get("value") === "Property rental businesses")
    assert(document.getElementById("reliefDescription-2").attributes().get("value") === "Dwellings opened to the public")
    assert(document.getElementById("reliefDescription-3").attributes().get("value") === "Property developers")
    assert(document.getElementById("reliefDescription-4").attributes().get("value") === "Property traders carrying on a property trading business")
    assert(document.getElementById("reliefDescription-5").attributes().get("value") === "Financial institutions acquiring dwellings in the course of lending")
    assert(document.getElementById("reliefDescription-6").attributes().get("value") === "Dwellings used for trade purposes")
    assert(document.getElementById("reliefDescription-7").attributes().get("value") === "Farmhouses")
    assert(document.getElementById("reliefDescription-8").attributes().get("value") === "Registered providers of Social Housing")
    assert(document.getElementById("reliefDescription-9").attributes().get("value") === "Equity Release Scheme")

    Then("The submit button should have the correct name")
    assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

    Then("The back link is correct")
    assert(document.getElementsByClass("govuk-back-link").text === "Back")
  }

    Scenario("display a selected a relief") {

      Given("the client is adding a relief")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015, periodChooseReliefForm.fill(PeriodChooseRelief(ReliefsUtils.RentalBusinessDesc)), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Select the type of relief")
      assert(document.select("h1").text contains "Select the type of relief")

      Then("The subheader should be - Create return")
//      assert(document.getElementsByTag("h1").text() contains  "This section is: Create return")

      assert(document.getElementById("reliefDescription").attributes().get("value") === "Property rental businesses")
      assert(document.getElementById("reliefDescription-2").attributes().get("value") === "Dwellings opened to the public")
      assert(document.getElementById("reliefDescription-3").attributes().get("value") === "Property developers")
      assert(document.getElementById("reliefDescription-4").attributes().get("value") === "Property traders carrying on a property trading business")
      assert(document.getElementById("reliefDescription-5").attributes().get("value") === "Financial institutions acquiring dwellings in the course of lending")
      assert(document.getElementById("reliefDescription-6").attributes().get("value") === "Dwellings used for trade purposes")
      assert(document.getElementById("reliefDescription-7").attributes().get("value") === "Farmhouses")
      assert(document.getElementById("reliefDescription-8").attributes().get("value") === "Registered providers of Social Housing")
      assert(document.getElementById("reliefDescription-9").attributes().get("value") === "Equity Release Scheme")

      Then("The submit button should have the correct name")
      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }

}
