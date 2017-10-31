/*
 * Copyright 2017 HM Revenue & Customs
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

import java.util.UUID

import builders.AuthBuilder._
import forms.AddressLookupForms._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest
import utils.AtedUtils

class addressLookupSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  val userId = s"user-${UUID.randomUUID}"
  implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The user can search for an address via the post code") {

    info("as a user I want to be able to search for an address via the post code")

    scenario("user is creating a chargeable return for a new property") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val html = views.html.propertyDetails.addressLookup(None, 2015, addressLookupForm, None, None)

      val document = Jsoup.parse(html.toString())
      Then("The title and header should match - Find the property's address")
      assert(document.title() === "Find the property’s address")
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
      assert(document.getElementById("backLinkHref") === null)
    }

    scenario("user is editing a chargeable return for an existing property") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val html = views.html.propertyDetails.addressLookup(Some("123456"), 2015, addressLookupForm, Some(AtedUtils.EDIT_SUBMITTED), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The title and header should match - Find the property's address")
      assert(document.title() === "Find the property’s address")
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
