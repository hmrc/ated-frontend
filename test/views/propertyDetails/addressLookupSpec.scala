/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.AddressLookupForms._
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.AtedUtils

class addressLookupSpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.addressLookup]

feature("The user can search for an address via the post code") {

    info("as a user I want to be able to search for an address via the post code")

    scenario("user is creating a chargeable return for a new property") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val html = injectedViewInstance(None, 2015, addressLookupForm, None, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The title and header should match - Find the property's address")
      assert(document.title() === "Find the property’s address - GOV.UK")
      assert(document.getElementById("account-lookup-header").text === "Find the property’s address")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

      Then("The fields should have the correct names")
      assert(document.getElementById("house-name_field").text() === "House name or number (optional)")
      assert(document.getElementById("house-name").attr("value") === "")
      assert(document.getElementById("postcode_field").text() === "Postcode")
      assert(document.getElementById("postcode").attr("value") === "")

      Then("The no post code link should be - I don't have a postcode")
      assert(document.getElementById("enter-address-link").text() === "Enter address manually")
      assert(document.getElementById("enter-address-link").attr("href") === "/ated/liability/address-lookup/manual/2015")

      Then("The submit button should be - Find address")
      assert(document.getElementById("submit").text() === "Find address")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
    }

    scenario("user is editing a chargeable return for an existing property") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val html = injectedViewInstance(Some("123456"), 2015, addressLookupForm, Some(AtedUtils.EDIT_SUBMITTED), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The title and header should match - Find the property's address")
      assert(document.title() === "Find the property’s address - GOV.UK")
      assert(document.getElementById("account-lookup-header").text === "Find the property’s address")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      Then("The fields should have the correct names")
      assert(document.getElementById("house-name_field").text() === "House name or number (optional)")
      assert(document.getElementById("house-name").attr("value") === "")
      assert(document.getElementById("postcode_field").text() === "Postcode")
      assert(document.getElementById("postcode").attr("value") === "")

      Then("The no post code link should be - I don't have a postcode")
      assert(document.getElementById("enter-address-link").text() === "Enter address manually")
      assert(document.getElementById("enter-address-link").attr("href") === "/ated/liability/address-lookup/manual/2015?propertyKey=123456&mode=editSubmitted")

      Then("The submit button should be - Find address")
      assert(document.getElementById("submit").text() === "Find address")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }
}
