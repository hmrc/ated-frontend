/*
 * Copyright 2019 HM Revenue & Customs
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

package views.editLiability

import java.util.UUID

import builders.AuthBuilder._
import forms.AtedForms._
import forms.PropertyDetailsForms._
import models.{EditLiabilityReturnType, HasValueChanged}
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class editLiabilitySpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen {

  val userId = s"user-${UUID.randomUUID}"
  implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The user can view an edit liability type page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page for the first time") {

      Given("A user visits the page and clicks yes")
      When("The user views the page and clicks yes")

      implicit val request = FakeRequest()
      val html = views.html.editLiability.editLiability(editLiabilityReturnTypeForm, "formBundleNo", 2015, true, None)

      val document = Jsoup.parse(html.toString())
      Then("the page title : How do you want to change your ATED return?")
      assert(document.title() === "How do you want to change your ATED return? - GOV.UK")


      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      assert(document.getElementById("editLiabilityType_legend").text() === "How do you want to change your ATED return?")
      assert(document.getElementById("editLiabilityType-cr").text() === "")
      assert(document.getElementById("editLiabilityType-dp").text() === "")

      And("the save button is correct")
      assert(document.getElementById("submit").text() === "Continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref") === null)
    }

  }

  feature("The user can edit a the setting to indicate that no value has change") {

    info("The yes option has been clicked may a user")

    scenario("The user isnt allowed to edit this form bundle") {

      Given("A user visits the page to and they are allowed to edit the data")
      When("The user views the page without an edit option")

      implicit val request = FakeRequest()

      val html = views.html.editLiability.editLiability(editLiabilityReturnTypeForm, "formBundleNo", 2015, false, Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("the page title : How do you want to change your ATED return?")
      assert(document.title() === "How do you want to change your ATED return? - GOV.UK")

      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      assert(document.getElementById("editliability-text").text() === "Your original return is too complex to edit online. To make any changes contact Customer Support.")

      assert(document.getElementById("editLiabilityType_legend").text() === "How do you want to change your ATED return?")
      assert(document.getElementById("editLiabilityType-cr") === null)
      assert(document.getElementById("editLiabilityType-dp").text() === "")

      assert(document.getElementById("submit").text() === "Continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }

  }

}
