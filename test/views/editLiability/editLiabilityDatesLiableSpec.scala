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

package views.editLiability

import config.ApplicationConfig
import forms.PropertyDetailsForms.periodDatesLiableForm
import models.{PropertyDetailsDatesLiable, StandardAuthRetrievals}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import play.api.data.{Form, FormError, Mapping}
import play.api.data.Forms._
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil

class editLiabilityDatesLiableSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil{

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedViewInstance = app.injector.instanceOf[views.html.editLiability.editLiabilityDatesLiable]

  Feature("The user can edit the period that the property is liable") {

    info("as a client i want to indicate when my property is liable")

    Scenario("allow editing the entire liability period") {

      Given("the client is adding a dates liable")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015, periodDatesLiableForm,  Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Enter the dates this change applies to")
      assert(document.title() === "Enter the dates this change applies to - GOV.UK")
      assert(document.select("h1").text.contains("Enter the dates this change applies to"))

      Then("The subheader should be - Change return")
      assert(document.getElementsByTag("h1").text.contains("This section is: Change return"))



      Then("The date fields should have the correct titles")
      assert(document.getElementById("startDate").text === "Day Month Year")
      assert(document.select("legend.govuk-fieldset__legend").first.text.contains("What was the first date, in this chargeable period, that this change applies to?"))
      assert(document.select("legend.govuk-fieldset__legend").get(1).text.contains("What was the last date, in this chargeable period, that this change applies to?"))
      assert(document.getElementById("startDate-hint").text === "For example, 1 4 2015")
      assert(document.getElementsByAttributeValue("for", "startDate.day").text === "Day")
      assert(document.getElementsByAttributeValue("for", "startDate.month").text === "Month")
      assert(document.getElementsByAttributeValue("for", "startDate.year").text === "Year")
      assert(document.getElementById("endDate").text === "Day Month Year")
      assert(document.getElementsByAttributeValue("for", "endDate.day").text === "Day")
      assert(document.getElementsByAttributeValue("for", "endDate.month").text === "Month")
      assert(document.getElementsByAttributeValue("for", "endDate.year").text === "Year")
      assert(document.getElementById("endDate-hint").text === "For example, 31 3 2016")

      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }

}
