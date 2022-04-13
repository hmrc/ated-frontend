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

import builders.{PropertyDetailsBuilder, TitleBuilder}
import config.ApplicationConfig
import forms.PropertyDetailsForms._
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.AtedUtils

class PropertyDetailsAddressSpec extends AnyFeatureSpecLike with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsAddress]

Feature("The user can view an empty property details page") {

    info("as a user I want to view the correct page content")

    Scenario("user has visited the page for the first time") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val html = injectedViewInstance(
        None, 2015, propertyDetailsAddressForm, None, Html(""), Some("backLink"), fromConfirmAddressPage = false)

      val document = Jsoup.parse(html.toString())
      Then("Enter your property details")
      assert(document.title() === TitleBuilder.buildTitle("Enter the address of the property manually"))

      And("The pre-header text is - Manage your ATED service")
      assert(document.getElementsByTag("h1").text() contains  "Enter the address of the property manually")

      And("The the link to the lookup pages text is - Lookup address")
      assert(document.getElementById("lookup-address-link").text() === "Lookup address")
      assert(document.getElementById("lookup-address-link").attr("href") === "/ated/liability/create/address/lookup/2015")

      assert(document.getElementsByAttributeValue("for","line_1").text() === "Address line 1")
      assert(document.getElementsByAttributeValue("for","line_2").text() === "Address line 2")
      assert(document.getElementsByAttributeValue("for","line_3").text() === "Address line 3 (optional)")
      assert(document.getElementsByAttributeValue("for","line_4").text() === "Address line 4 (optional)")
      assert(document.getElementsByAttributeValue("for","postcode").text() === "Postcode (optional)")
      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")

    }

    Scenario("user has visited the page to edit data") {

      Given("A user visits the page to edit data")
      When("The user views the page to edit data")
      implicit val request = FakeRequest()

      val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("postCode"))
      val html = injectedViewInstance(
        Some("1"), 2015, propertyDetailsAddressForm.fill(propertyDetails), Some(AtedUtils.EDIT_SUBMITTED), Html(""), Some("http://backLink"), fromConfirmAddressPage = false)

      val document = Jsoup.parse(html.toString())
      Then("Enter your property details")
      assert(document.title() === TitleBuilder.buildTitle("Enter the address of the property manually"))

      And("The pre-header text is - Manage your ATED service")
      assert(document.getElementsByTag("h1").text() contains  "Enter the address of the property manually")

      And("The the link to the lookup pages text is - Lookup address")
      assert(document.getElementById("lookup-address-link").text() === "Lookup address")
      assert(document.getElementById("lookup-address-link").attr("href") === "/ated/liability/create/address/lookup/2015?propertyKey=1&mode=editSubmitted")

      assert(document.getElementsByAttributeValue("for","line_1").text() === "Address line 1")
      assert(document.getElementsByAttributeValue("for","line_2").text() === "Address line 2")
      assert(document.getElementsByAttributeValue("for","line_3").text() === "Address line 3 (optional)")
      assert(document.getElementsByAttributeValue("for","line_4").text() === "Address line 4 (optional)")
      assert(document.getElementsByAttributeValue("for","postcode").text() === "Postcode (optional)")
      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")

    }
  }

  Feature("The user can view a pre populated edit address page") {

    info("as a user I want to view the correct page content")

    Scenario("user has visited the page to edit address") {

      Given("A user visits the page")

      When("The user views the page")

      implicit val request = FakeRequest()
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("postCode"))
      val html = injectedViewInstance(
        Some("1"), 2015, propertyDetailsAddressForm.fill(propertyDetails), None, Html(""), Some("http://backLink"), fromConfirmAddressPage = true)

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Confirm address")
      assert(document.getElementsByTag("h1").text() contains   "Edit address")

      Then("The subheader should be - Create return")
      assert(document.getElementsByTag("h1").text() contains   "This section is: Create return")


      assert(document.getElementsByAttributeValue("for","line_1").text() === "Address line 1")
      assert(document.getElementsByAttributeValue("for","line_2").text() === "Address line 2")
      assert(document.getElementsByAttributeValue("for","line_3").text() === "Address line 3 (optional)")
      assert(document.getElementsByAttributeValue("for","line_4").text() === "Address line 4 (optional)")
      assert(document.getElementsByAttributeValue("for","postcode").text() === "Postcode (optional)")
      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")

    }
  }
}
