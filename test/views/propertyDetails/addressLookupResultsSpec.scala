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

package views.propertyDetails

import config.ApplicationConfig
import forms.AddressLookupForms._
import models._
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.AtedUtils
import views.html.propertyDetails.addressLookupResults

class addressLookupResultsSpec extends AnyFeatureSpecLike with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance: addressLookupResults = app.injector.instanceOf[views.html.propertyDetails.addressLookupResults]

Feature("The user can search for an address via the post code") {

    info("as a user I want to be able to search for an address via the post code")

    Scenario("user is has searched for their property but has no search results") {

      Given("A user has searched for a property while creating a new liability")
      When("The user views the page")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val results = AddressSearchResults(searchCriteria = AddressLookup("XX1 1XX", None), Nil)
      val html = injectedViewInstance(None, 2015, addressSelectedForm, results, None, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("Select the address of the property")
      assert(document.title() === "Select the address of the property - Submit and view your ATED returns - GOV.UK")

      Then("The header should match - Select the address of the property")
      assert(document.getElementsByTag("h1").text contains "Select the address of the property")

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text === "This section is: Create return")

      Then("The search criteria header should be - Postcode")
      assert(document.getElementById("search-criteria-header").text() === "Postcode")
      assert(document.getElementById("postcode").text() === "XX1 1XX")
      assert(document.getElementById("change-address-search-link").text ===  "Change postcode")

      Then("The search criteria results header should be - Property address")
      assert(document.getElementById("search-results-header").text() === "Property address")

      Then("The proprerty address returns - No addresses were found for this postcode")
      assert(document.getElementById("no-address-found").text() === "No addresses were found for this postcode")

      assert(document.getElementById("enter-address-link").text() contains "Manually enter the address")
      assert(document.getElementById("enter-address-link").attr("href") contains "/ated/liability/address-lookup/manual/2015")

      Then("The submit button should not be displayed")
      assert(document.getElementById("govuk-button") === null)

      Then("The back link is correct")
      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
    }

    Scenario("user is editing a chargeable return for an existing property") {

      Given("A user has searched for a property while editing a chargeable return")
      When("The user views the page")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val address1 = AddressLookupRecord(1, AddressSearchResult(List("1", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))
      val address2 = AddressLookupRecord(2, AddressSearchResult(List("2", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))
      val address3 = AddressLookupRecord(3, AddressSearchResult(List("3", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))
      val results = AddressSearchResults(searchCriteria = AddressLookup("XX1 1XX", None),
        results = List(address1, address2, address3))
           val html = injectedViewInstance(Some("123456"),
             2015, addressSelectedForm, results, Some(AtedUtils.EDIT_SUBMITTED), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("Select the address of the property")
      assert(document.title() === "Select the address of the property - Submit and view your ATED returns - GOV.UK")

      Then("The header should match - Select the address of the property")
      assert(document.getElementsByTag("h1").text() contains "Select the address of the property")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      Then("The search criteria header should be - Postcode")
      assert(document.getElementById("search-criteria-header").text() contains  "Postcode")
      assert(document.getElementById("postcode").text() === "XX1 1XX")
      assert(document.getElementById("change-address-search-link").text() === "Change postcode")
      assert(document.getElementById("change-address-search-link").attr("href") === "/ated/liability/address-lookup/view/2015?propertyKey=123456")

      Then("The search criteria results header should be - Property address")
      assert(document.getElementById("search-results-header").text() === "Property address")
      assert(document.getElementsByAttributeValue("for", "selected").text.contains("1, result street, UK") === true)
      assert(document.getElementsByAttributeValue("for", "selected-2").text.contains("2, result street, UK") === true)
      assert(document.getElementsByAttributeValue("for", "selected-3").text.contains("3, result street, UK") === true)

      Then("The address link should be - I cannot find my address in the list")
      assert(document.getElementById("no-address-found") === null)

      assert(document.getElementById("enter-address-link").text() === "I cannot find my address in the list")
      assert(document.getElementById("enter-address-link").attr("href") === "/ated/liability/address-lookup/manual/2015?propertyKey=123456&mode=editSubmitted")

      Then("The submit button should be - Save and continue")
      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }
}
