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

package views

import config.ApplicationConfig
import forms.AtedForms.selectPeriodForm
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import views.html.selectPeriod

class selectPeriodSpec extends AnyFeatureSpec with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedViewInstance: selectPeriod = app.injector.instanceOf[views.html.selectPeriod]

  val periodKey = 2015

  Feature("The user can view the select period page") {

    info("As a client I want to be able to select what period I am submitting a return for")

    Scenario("Show the select period radio buttons") {

      Given("The client is creating a new return and wants to tell us the year it is for")
      When("The user views the page")

      val periods = List("2015" -> "2015 to 2016", "2016" -> "2016 to 2017")
      val html = injectedViewInstance(selectPeriodForm, periods, Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Select an ATED period")
      assert(document.getElementsByTag("h1").text contains "Select an ATED chargeable period")

      Then("The subheader should be - Create relief return")
      assert(document.getElementsByClass("govuk-caption-xl").text() contains "This section is: Create return")

      Then("The the text on the screen should be correct")
      assert(document.getElementById("period-hint").text() === "The chargeable period for a year runs from the 1 April to 31 March.")
      assert(document.getElementsByClass("govuk-details__summary-text").text() === "I want to submit a return before 2015")
      assert(document.getElementsByClass("govuk-details__text").text() === "Any ATED returns before these periods" +
        " need to be submitted on a paper form. To request a paper form contact the call centre (opens in new tab).")

      assert(document.getElementsByAttributeValue("for", "period").text() == "2015 to 2016")
      assert(document.getElementsByAttributeValue("for", "period-2").text() == "2016 to 2017")

      assert(document.getElementById("submit").text() === "Continue")

      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }

}
