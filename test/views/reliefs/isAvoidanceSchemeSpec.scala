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

package views.reliefs

import java.util.UUID

import builders.AuthBuilder._
import forms.ReliefForms._
import models.IsTaxAvoidance
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class isAvoidanceSchemeSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen {

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
  val userId = s"user-${UUID.randomUUID}"
  implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
  val periodKey = 2015

  feature("The user can view the is avoidance scheme page") {

    info("as a client i want to be able to select whether or not I am using an avoidance scheme")

    scenario("show the is an avoidance scheme being used radio buttons") {

      Given("the client is creating a new relief and want tell us if an avoidance scheme is being used")
      When("The user views the page")

      val html = views.html.reliefs.avoidanceSchemeBeingUsed(periodKey, isTaxAvoidanceForm , new LocalDate("2015-04-01"), None)

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Is an avoidance scheme being used for any of these reliefs?")
      assert(document.select("h1").text === "Is an avoidance scheme being used for any of these reliefs?")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

      Then("The the text on the screen should be correct")
      assert(document.getElementById("isAvoidanceScheme-true").attr("checked") === "")
      assert(document.getElementById("isAvoidanceScheme-false").attr("checked") === "")

      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref") === null)
    }

    scenario("show the reliefs we have previously chosen") {

      Given("the client is creating a new relief and want to see the options")
      When("The user views the page")

      val isTaxAvoidance = IsTaxAvoidance(isAvoidanceScheme = Some(true))
      val html = views.html.reliefs.avoidanceSchemeBeingUsed(periodKey, isTaxAvoidanceForm.fill(isTaxAvoidance), new LocalDate("2015-04-01"), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Is an avoidance scheme being used for any of these reliefs?")
      assert(document.select("h1").text === "Is an avoidance scheme being used for any of these reliefs?")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

      Then("The the text on the screen should be correct")
      assert(document.getElementById("isAvoidanceScheme-true").attr("checked") === "checked")
      assert(document.getElementById("isAvoidanceScheme-false").attr("checked") === "")

      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }

}
