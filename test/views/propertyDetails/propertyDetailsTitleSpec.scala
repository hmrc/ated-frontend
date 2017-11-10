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
import forms.PropertyDetailsForms._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest
import utils.AtedUtils

class propertyDetailsTitleSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
  val userId = s"user-${UUID.randomUUID}"
  implicit val user = createAtedContext(createUserAuthContext(userId, "name"))

  feature("The user can adit the title") {

    info("as a client i want to be able to edit my property title")

    scenario("allow editing a title when creating a new draft") {

      Given("the client is adding a dates liable")
      When("The user views the page")

      val html = views.html.propertyDetails.propertyDetailsTitle("1", 2015, propertyDetailsTitleForm, None, None)

      val document = Jsoup.parse(html.toString())

      Then("The header and title should match - What is the property title number?")
      assert(document.title() === "What is the property’s title number? (optional) - GOV.UK")
      assert(document.select("h1").text === "What is the property’s title number? (optional)")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")


      Then("The text fields should match")
      assert(document.getElementById("title-text").text() === "You can find the property’s title number on the title deeds for the property.")
      assert(document.getElementById("references.titleNumber").attr("value") === "")
      assert(document.getElementById("references.titleNumber_hint").text() === "For example, CS72532")
      assert(document.getElementById("titleNumber-reveal").text() === "I don’t know my property’s title number")


      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")


      Then("The back link is correct")
      assert(document.getElementById("backLinkHref") === null)
    }

    scenario("allow editing a title when editing a submitted return") {

      Given("the client is adding a dates liable")
      When("The user views the page")

      val html = views.html.propertyDetails.propertyDetailsTitle("1", 2015, propertyDetailsTitleForm, Some(AtedUtils.EDIT_SUBMITTED), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header and title should match - What is the property title number?")
      assert(document.title() === "What is the property’s title number? (optional) - GOV.UK")
      assert(document.select("h1").text === "What is the property’s title number? (optional)")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      Then("The text fields should match")
      assert(document.getElementById("title-text").text() === "You can find the property’s title number on the title deeds for the property.")
      assert(document.getElementById("references.titleNumber").attr("value") === "")
      assert(document.getElementById("references.titleNumber_hint").text() === "For example, CS72532")
      assert(document.getElementById("titleNumber-reveal").text() === "I don’t know my property’s title number")


      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")


      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }

}
