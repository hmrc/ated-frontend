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

package views.subscriptionData

import java.util.UUID

import builders.AuthBuilder._
import builders.PropertyDetailsBuilder
import forms.PropertyDetailsForms._
import models._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class CompanyDetailsSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  val userId = s"user-${UUID.randomUUID}"
  implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val addressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
  val contactDetails = ContactDetails(emailAddress = Some("a@b.c"))
  val correspondence = Address(Some("name1"), Some("name2"), addressDetails = addressDetails, contactDetails = Some(contactDetails))
  val businessPartnerDetails = RegisteredDetails(false, "testName",
    RegisteredAddressDetails(addressLine1 = "bpline1",
      addressLine2 = "bpline2",
      addressLine3 = Some("bpline3"),
      addressLine4 = Some("bpline4"),
      postalCode = Some("postCode"),
      countryCode = "GB"))

  val businessPartnerDetailsEditable = RegisteredDetails(true, "testName",
    RegisteredAddressDetails(addressLine1 = "bpline1",
      addressLine2 = "bpline2",
      addressLine3 = Some("bpline3"),
      addressLine4 = Some("bpline4"),
      postalCode = Some("postCode"),
      countryCode = "GB"))

  feature("The user can view their company details") {

    info("as a user I want to view my company details")

    scenario("user visits the page with email set as preference and an editable address") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val html = views.html.subcriptionData.companyDetails(Some(correspondence), Some(businessPartnerDetailsEditable), true, None, None, Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("Your ATED details")
      assert(document.title() === "Your ATED details - GOV.UK")

      And("The pre-header text is - Your ATED details")
      assert(document.getElementById("company-details-header").text() === "Your ATED details")

      assert(document.getElementById("company-name-header").text() === "Name")
      assert(document.getElementById("company-name-val").text() === "testName")

      assert(document.getElementById("ated-reference-number").text() === "ATED reference number")
      assert(document.getElementById("ated-reference-number-val").text() === "XN1200000100001")

      assert(document.getElementById("registered-address-label").text() === "Registered address")
      assert(document.getElementById("registered-edit").attr("href") === "/ated/registered-details")

      assert(document.getElementById("correspondence-address-label").text() === "Correspondence address")
      assert(document.getElementById("correspondence-edit").text() === "Edit Correspondence address")
      assert(document.getElementById("contactdetails-edit").text() === "Edit ATED contact details")

      assert(document.getElementById("correspondence-edit").attr("href") === "/ated/correspondence-address")
      assert(document.getElementById("contactdetails-edit").attr("href") === "/ated/edit-contact")

      assert(document.getElementById("contact-details-label").text() === "ATED contact details")
      assert(document.getElementById("contactdetails-edit").text() === "Edit ATED contact details")
      assert(document.getElementById("contact-pref-label").text() === "Email address")
      assert(document.getElementById("contact-pref-val").text() === "a@b.c")
      assert(document.getElementById("back").text() === "Back to your ATED online service")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }

    scenario("user visits the page with email set as preference and no editable address") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val html = views.html.subcriptionData.companyDetails(Some(correspondence), Some(businessPartnerDetails), true, None, None, Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("Your ATED details")
      assert(document.title() === "Your ATED details - GOV.UK")

      And("The pre-header text is - Your ATED details")
      assert(document.getElementById("company-details-header").text() === "Your ATED details")

      assert(document.getElementById("company-name-header").text() === "Name")
      assert(document.getElementById("company-name-val").text() === "testName")

      assert(document.getElementById("ated-reference-number").text() === "ATED reference number")
      assert(document.getElementById("ated-reference-number-val").text() === "XN1200000100001")

      assert(document.getElementById("registered-address-label").text() === "Registered address")
      assert(Option(document.getElementById("registered-edit")) === None)

      assert(document.getElementById("correspondence-address-label").text() === "Correspondence address")
      assert(document.getElementById("correspondence-edit").text() === "Edit Correspondence address")
      assert(document.getElementById("contactdetails-edit").text() === "Edit ATED contact details")

      assert(document.getElementById("correspondence-edit").attr("href") === "/ated/correspondence-address")
      assert(document.getElementById("contactdetails-edit").attr("href") === "/ated/edit-contact")

      assert(document.getElementById("contact-details-label").text() === "ATED contact details")
      assert(document.getElementById("contactdetails-edit").text() === "Edit ATED contact details")
      assert(document.getElementById("contact-pref-label").text() === "Email address")
      assert(document.getElementById("contact-pref-val").text() === "a@b.c")
      assert(document.getElementById("back").text() === "Back to your ATED online service")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }

    scenario("user visits the page without email set as preference") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val html = views.html.subcriptionData.companyDetails(Some(correspondence), Some(businessPartnerDetails), false, None, None, None)

      val document = Jsoup.parse(html.toString())
      Then("Your ATED details")
      assert(document.title() === "Your ATED details - GOV.UK")

      And("The pre-header text is - Your ATED details")
      assert(document.getElementById("company-details-header").text() === "Your ATED details")

      assert(document.getElementById("contactdetails-edit").text() === "Edit ATED contact details")
      assert(document.getElementById("contact-pref-label").text() === "Email address")
      assert(document.getElementById("contact-pref-val").text() === "Not Provided")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref") === null)
    }
  }
}
