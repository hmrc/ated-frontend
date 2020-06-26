/*
 * Copyright 2020 HM Revenue & Customs
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
import models._
import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.AtedUtils

class addressLookupResultsSpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
feature("The user can search for an address via the post code") {

    info("as a user I want to be able to search for an address via the post code")

    scenario("user is has searched for their property but has no search results") {

      Given("A user has searched for a property while creating a new liability")
      When("The user views the page")
      implicit val request = FakeRequest()

      val results = AddressSearchResults(searchCriteria = AddressLookup("XX1 1XX", None), Nil)
      val html = views.html.propertyDetails.addressLookupResults(None, 2015, addressSelectedForm, results, None, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())
      Then("Select the address of the property")
      assert(document.title() === "Select the address of the property - GOV.UK")

      Then("The header should match - Select the address of the property")
      assert(document.getElementById("account-lookup-header").text === "Select the address of the property")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

      Then("The search criteria header should be - Postcode")
      assert(document.getElementById("search-criteria-header").text() === "Postcode")
      assert(document.getElementById("postcode").text() === "XX1 1XX")
      assert(document.getElementById("change-address-search-link").text() === "Change postcode")
      assert(document.getElementById("change-address-search-link").attr("href") === "/ated/liability/address-lookup/view/2015")

      Then("The search criteria results header should be - Property address")
      assert(document.getElementById("search-results-header").text() === "Property address")

      Then("The address link should be - I cannot find my address in the list")
      assert(document.getElementById("no-address-found").text() === "No addresses were found for this postcode")

      assert(document.getElementById("enter-address-link").text() === "Manually enter the address")
      assert(document.getElementById("enter-address-link").attr("href") === "/ated/liability/address-lookup/manual/2015")

      Then("The submit button should be - Save and continue")
      assert(document.getElementById("submit") === null)

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
    }

    scenario("user is editing a chargeable return for an existing property") {

      Given("A user has searched for a property while editing a chargeable return")
      When("The user views the page")
      implicit val request = FakeRequest()

      val address1 = AddressLookupRecord("1", AddressSearchResult(List("1", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))
      val address2 = AddressLookupRecord("2", AddressSearchResult(List("2", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))
      val address3 = AddressLookupRecord("3", AddressSearchResult(List("3", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))
      val results = AddressSearchResults(searchCriteria = AddressLookup("XX1 1XX", None),
        results = List(address1, address2, address3))
           val html = views.html.propertyDetails.addressLookupResults(Some("123456"),
             2015, addressSelectedForm, results, Some(AtedUtils.EDIT_SUBMITTED), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("Select the address of the property")
      assert(document.title() === "Select the address of the property - GOV.UK")

      Then("The header should match - Select the address of the property")
      assert(document.getElementById("account-lookup-header").text === "Select the address of the property")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      Then("The search criteria header should be - Postcode")
      assert(document.getElementById("search-criteria-header").text() === "Postcode")
      assert(document.getElementById("postcode").text() === "XX1 1XX")
      assert(document.getElementById("change-address-search-link").text() === "Change postcode")
      assert(document.getElementById("change-address-search-link").attr("href") === "/ated/liability/address-lookup/view/2015?propertyKey=123456")

      Then("The search criteria results header should be - Property address")
      assert(document.getElementById("search-results-header").text() === "Property address")
      assert(document.getElementById("selected-1_field").text() === "1, result street, UK")
      assert(document.getElementById("selected-2_field").text() === "2, result street, UK")
      assert(document.getElementById("selected-3_field").text() === "3, result street, UK")

      Then("The address link should be - I cannot find my address in the list")
      assert(document.getElementById("no-address-found") === null)

      assert(document.getElementById("enter-address-link").text() === "I cannot find my address in the list")
      assert(document.getElementById("enter-address-link").attr("href") === "/ated/liability/address-lookup/manual/2015?propertyKey=123456&mode=editSubmitted")

      Then("The submit button should be - Save and continue")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }
}
