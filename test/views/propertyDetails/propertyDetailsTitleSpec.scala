/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.PropertyDetailsForms._
import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.AtedUtils

class propertyDetailsTitleSpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext = organisationStandardRetrievals
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsTitle]

  feature("The user can adit the title") {

    info("as a client i want to be able to edit my property title")

    scenario("allow editing a title when creating a new draft") {

      Given("the client is adding a dates liable")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015, propertyDetailsTitleForm, None, Html(""), Some("backLink"))

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
      assert(document.getElementById("titleNumber-reveal").text() === "I do not know my property’s title number")


      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")


      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
    }

    scenario("allow editing a title when editing a submitted return") {

      Given("the client is adding a dates liable")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015,
        propertyDetailsTitleForm, Some(AtedUtils.EDIT_SUBMITTED), Html(""), Some("http://backLink"))

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
      assert(document.getElementById("titleNumber-reveal").text() === "I do not know my property’s title number")


      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")


      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }

}
