/*
 * Copyright 2018 HM Revenue & Customs
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
import forms.AtedForms.editContactDetailsForm
import models._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest


class EditContactDetailsSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{


  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The client can view their email details") {

    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))

    info("as a client I want to view my email details")

    scenario("user visits the page with email settings so they can edit them") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val prePopulatedData = EditContactDetails(firstName = "Y",
        lastName = "Z",
        phoneNumber = "0191"
      )
      val html = views.html.subcriptionData.editContactDetails(editContactDetailsForm.fill(prePopulatedData), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("Edit your ATED contact details")
      assert(document.title() === "Edit your ATED contact details - GOV.UK")
      assert(document.getElementById("contact-details-header").text() === "Edit your ATED contact details")

      And("The pre-header text is - Manage your ATED service")
      assert(document.getElementById("pre-header").text() === "This section is: Manage your ATED service")

      And("The the field names are correct")
      assert(document.getElementById("phoneNumber_field").text() === "Telephone")
      assert(document.getElementById("phoneNumber").attr("type") === "number")
      assert(document.getElementById("firstName_field").text() === "First Name")
      assert(document.getElementById("lastName_field").text() === "Last Name")

      And("The the field values are blank")
      assert(document.getElementById("phoneNumber").attr("value") === "0191")
      assert(document.getElementById("firstName").attr("value") === "Y")
      assert(document.getElementById("lastName").attr("value") === "Z")

      And("The the submit button is - Save changes")
      assert(document.getElementById("submit").text() === "Save changes")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }

  }
}
