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
import models.StandardAuthRetrievals
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
import views.html.propertyDetails.addressLookup

class addressLookupSpec extends AnyFeatureSpecLike with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance: addressLookup = app.injector.instanceOf[views.html.propertyDetails.addressLookup]

Feature("The user can search for an address via the post code") {

    info("as a user I want to be able to search for an address via the post code")

    Scenario("user is creating a chargeable return for a new property") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val html = injectedViewInstance(None, 2015, addressLookupForm, None, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The title and header should match - Find the property's address")
      assert(document.title() === "Find the property’s address - GOV.UK")
      assert(document.getElementsByTag("h1").text() contains "Find the property’s address")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return")

      Then("The fields should have the correct names")
      assert(document.getElementsByAttributeValue("for","houseName").text() === "House name or number (optional)")
      assert(document.getElementById("houseName").attr("value") === "")
      document.getElementsByAttributeValue("for","postcode").text() === "Postcode"
      assert(document.getElementById("postcode").attr("value") === "")

      Then("The no post code link should be - I don't have a postcode")
      assert(document.getElementById("enter-address-link").text() === "Enter address manually")
      assert(document.getElementById("enter-address-link").attr("href") === "/ated/liability/address-lookup/manual/2015")

      Then("The submit button should be - Find address")
      assert(document.getElementsByClass("govuk-button").text() === "Find address")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text() === "Back")
    }

    Scenario("user is editing a chargeable return for an existing property") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val html = injectedViewInstance(Some("123456"), 2015, addressLookupForm, Some(AtedUtils.EDIT_SUBMITTED), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The title and header should match - Find the property's address")
      assert(document.title() === "Find the property’s address - GOV.UK")
      assert(document.getElementsByTag("h1").text() contains "Find the property’s address")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      Then("The fields should have the correct names")
      assert(document.getElementsByAttributeValue("for","houseName").text() === "House name or number (optional)")
      assert(document.getElementById("houseName").attr("value") === "")
      document.getElementsByAttributeValue("for","postcode").text() === "Postcode"
      assert(document.getElementById("postcode").attr("value") === "")

      Then("The no post code link should be - I don't have a postcode")
      assert(document.getElementById("enter-address-link").text() === "Enter address manually")
      assert(document.getElementById("enter-address-link").attr("href") === "/ated/liability/address-lookup/manual/2015?propertyKey=123456&mode=editSubmitted")

      Then("The submit button should be - Find address")
      assert(document.getElementsByClass("govuk-button").text() === "Find address")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text() === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }
}
