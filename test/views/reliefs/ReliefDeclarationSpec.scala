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

package views.reliefs

import java.util.UUID

import builders.AuthBuilder._
import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest


class ReliefDeclarationSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  val userId = s"user-${UUID.randomUUID}"

  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The user can view the relief declaration page") {

    info("as a user I want to view the correct page content")

    scenario("user has created a relief return") {
      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
      val html = views.html.reliefs.reliefDeclaration(2015, None)

      val document = Jsoup.parse(html.toString())
      Then("Returns declaration")
      assert(document.title() === "Returns declaration")

      And("The pre-header text is - Create relief return")
      assert(document.getElementById("relief-declaration-confirmation-header").text() === "Returns declaration")
      assert(document.getElementById("relief-declaration-before-declaration-text").text() === "Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
      assert(document.getElementById("relief-declaration-mid-declaration-text").text() === "Each type of relief claimed is an individual ATED return.")
      assert(document.getElementById("declare-or-confirm").text() === "I declare that:")
      assert(document.getElementById("declaration-confirmation-text").text() === "the information I have given on this return (or each of these returns) is correct and complete to the best of my knowledge and belief and confirm I am eligible for the reliefs claimed")
      assert(document.getElementById("submit").text() === "Agree and submit returns")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref") === null)
    }
  }

  feature("The agent can view the relief declaration page as a client") {

    info("as an agent I want to view the correct page content")

    scenario("agent has created a relief return") {
      Given("An agent visits the page")
      When("The agent views the page")
      implicit val request = FakeRequest()

      implicit val user = createAtedContext(createDelegatedAuthContext(userId, "company name|display name"))
      val html = views.html.reliefs.reliefDeclaration(2015, Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("Returns declaration")
      assert(document.title() === "Returns declaration")

      And("The pre-header text is - Create relief return")
      assert(document.getElementById("relief-declaration-confirmation-header").text() === "Returns declaration")
      assert(document.getElementById("relief-declaration-before-declaration-text").text() === "Before your client's return or returns can be submitted to HMRC, you must read and agree to the following statement. Your client's approval may be in electronic or non-electronic form. If your client gives false information, they may have to pay financial penalties and face prosecution.")
      assert(document.getElementById("relief-declaration-mid-declaration-text").text() === "Each type of relief claimed is an individual ATED return.")
      assert(document.getElementById("declare-or-confirm").text() === "I confirm that:")
      assert(document.getElementById("declaration-confirmation-text").text() === "my client has approved the information contained in this return (or each of these returns) as being correct and complete to the best of their knowledge and belief and confirms they are eligible for the reliefs claimed")
      assert(document.getElementById("submit").text() === "Agree and submit returns")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }
}
