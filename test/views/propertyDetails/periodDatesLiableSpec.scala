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
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil

class periodDatesLiableSpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.periodDatesLiable]

feature("The user can add a period that the property is liable") {

    info("as a client i to indicate when my property is liable")

    scenario("allow setting the entire liability period") {

      Given("the client is adding a dates liable")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015, periodDatesLiableForm,
        "Enter the dates when the property was liable for an ATED charge", None, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Enter the dates when the property was liable for an ATED charge")
      assert(document.select("h1").text === "Enter the dates when the property was liable for an ATED charge")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

      Then("The date fields should have the correct titles")
      assert(document.getElementById("startDate")
        .text === "What was the start date in this chargeable period when the property became liable for a charge? For example, 1 4 2015 Day Month Year")
      assert(document.getElementById("endDate")
        .text === "What was the end date in this chargeable period when the property stopped being liable for a charge? For example, 31 3 2016 Day Month Year")

      Then("The date fields should have the correct default values")
      assert(document.getElementById("startDate-day").attr("value") === "")
      assert(document.getElementById("startDate-month").attr("value") === "")
      assert(document.getElementById("startDate-year").attr("value") === "")
      assert(document.getElementById("endDate-day").attr("value") === "")
      assert(document.getElementById("endDate-month").attr("value") === "")
      assert( document.getElementById("endDate-year").attr("value") === "")

      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
    }

    scenario("allow adding a new liability dates") {

      Given("the client is adding a dates liable")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015, periodDatesLiableForm,
        "Add the dates when the property was liable for an ATED charge", Some("add"), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Add the dates when the property was liable for an ATED charge")
      assert(document.select("h1").text === "Add the dates when the property was liable for an ATED charge")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")
      Then("The date fields should have the correct titles")
      assert(document.getElementById("startDate")
        .text === "What was the start date in this chargeable period when the property became liable for a charge? For example, 1 4 2015 Day Month Year")
      assert(document.getElementById("endDate")
        .text === "What was the end date in this chargeable period when the property stopped being liable for a charge? For example, 31 3 2016 Day Month Year")

      Then("The date fields should have the correct default values")
      assert(document.getElementById("startDate-day").attr("value") === "")
      assert(document.getElementById("startDate-month").attr("value") === "")
      assert(document.getElementById("startDate-year").attr("value") === "")
      assert(document.getElementById("endDate-day").attr("value") === "")
      assert(document.getElementById("endDate-month").attr("value") === "")
      assert( document.getElementById("endDate-year").attr("value") === "")

      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }

  }

}
