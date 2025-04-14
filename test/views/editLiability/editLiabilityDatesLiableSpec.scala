/*
 * Copyright 2023 HM Revenue & Customs
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
import views.html.editLiability.editLiabilityDatesLiable

class editLiabilityDatesLiableSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil{

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedViewInstance: editLiabilityDatesLiable = app.injector.instanceOf[views.html.editLiability.editLiabilityDatesLiable]

  Feature("The user can edit the period that the property is liable") {

    info("as a client i want to indicate when my property is liable")

    Scenario("allow editing the entire liability period") {

      Given("the client is adding a dates liable")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015, periodDatesLiableForm,  Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Enter the dates this change applies to")
      assert(document.title() === "Enter the dates this change applies to - Submit and view your ATED returns - GOV.UK")
      assert(document.select("h1").text.contains("Enter the dates this change applies to"))

      Then("The subheader should be - Change return")
      assert(document.getElementsByTag("h2").text.contains("This section is: Change return"))



      Then("The date fields should have the correct titles")
      assert(document.getElementById("startDate").text === "Day The first day the change applied to in this chargeable period Month The first month the change applied to in this chargeable period Year The first year the change applied to in this chargeable period")
      assert(document.select("legend.govuk-fieldset__legend").first.text.contains("What was the first date the change applied to in this chargeable period?"))
      assert(document.select("legend.govuk-fieldset__legend").get(1).text.contains("What was the last date the change applied to in this chargeable period?"))
      assert(document.getElementById("startDate-hint").text === "For example, 1 4 2015")
      assert(document.getElementsByAttributeValue("for", "startDate.day").text === "Day The first day the change applied to in this chargeable period")
      assert(document.getElementsByAttributeValue("for", "startDate.month").text === "Month The first month the change applied to in this chargeable period")
      assert(document.getElementsByAttributeValue("for", "startDate.year").text === "Year The first year the change applied to in this chargeable period")
      assert(document.getElementById("endDate").text === "Day The last day the change applied to in this chargeable period Month The last month the change applied to in this chargeable period Year The last year the change applied to in this chargeable period")
      assert(document.getElementsByAttributeValue("for", "endDate.day").text === "Day The last day the change applied to in this chargeable period")
      assert(document.getElementsByAttributeValue("for", "endDate.month").text === "Month The last month the change applied to in this chargeable period")
      assert(document.getElementsByAttributeValue("for", "endDate.year").text === "Year The last year the change applied to in this chargeable period")
      assert(document.getElementById("endDate-hint").text === "For example, 1 4 2015")

      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }

}
