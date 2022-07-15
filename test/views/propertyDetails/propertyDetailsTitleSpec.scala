/*
 * Copyright 2022 HM Revenue & Customs
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
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.AtedUtils
import views.html.propertyDetails.propertyDetailsTitle

class propertyDetailsTitleSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance: propertyDetailsTitle = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsTitle]

  Feature("The user can adit the title") {

    info("as a client i want to be able to edit my property title")

    Scenario("allow editing a title when creating a new draft") {

      Given("the client is adding a dates liable")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015, propertyDetailsTitleForm, None, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header and title should match - What is the property title number?")
      assert(document.title() === "What is the property’s title number? (optional) - GOV.UK")
      assert(document.getElementsByTag("h1").text() contains "What is the property’s title number? (optional)")
      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return" )

      Then("The text fields should match")
      assert(document.getElementById("title-text").text() === "You can find the property’s title number on the title deeds for the property.")
      assert(document.getElementById("titleNumber").attr("value") === "")
      assert(document.getElementById("titleNumber-hint").text() === "For example, CS72532")

      Then("The submit button should have the correct name")
      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text() === "Back")
    }

    Scenario("allow editing a title when editing a submitted return") {

      Given("the client is adding a dates liable")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015,
        propertyDetailsTitleForm, Some(AtedUtils.EDIT_SUBMITTED), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header and title should match - What is the property title number?")
      assert(document.title() === "What is the property’s title number? (optional) - GOV.UK")
      assert(document.getElementsByTag("h1").text() contains "What is the property’s title number? (optional)")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      Then("The text fields should match")
      assert(document.getElementById("title-text").text() === "You can find the property’s title number on the title deeds for the property.")
      assert(document.getElementById("titleNumber").attr("value") === "")
      assert(document.getElementById("titleNumber-hint").text() === "For example, CS72532")
      assert(document.getElementsByClass("govuk-details__summary-text").text() === "I do not know my property’s title number")

      Then("The submit button should have the correct name")
      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text() === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }

}
