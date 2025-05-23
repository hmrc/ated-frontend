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

package views.propertyDetails

import config.ApplicationConfig
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
import views.html.propertyDetails.newBuildNoStartDate

class noStartDateSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance: newBuildNoStartDate = app.injector.instanceOf[views.html.propertyDetails.newBuildNoStartDate]

  Feature("The user views to the No start date error/warning page before they return the orignal questioning") {

    info("as a client I want to view the no start date kickout page")

    Scenario("describe missing date requirements") {

      Given("the client has failed to enter the sufficient information")
      When("The user doesnt enter either start date")
      val html = injectedViewInstance("1", Html(""), None, Some("http://backLink"))
      val document = Jsoup.parse(html.toString())
      Then("The title should match - No start date was provided - Submit and view your ATED returns - GOV.UK")
      assert(document.title() === "No start date was provided - Submit and view your ATED returns - GOV.UK")

      Then("The header should match - No start date was provided")
      assert(document.getElementsByTag("h1").text() contains "No start date was provided")

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return")

      val paragraphs = document.getElementsByTag("p")
      assert(paragraphs.first.text() contains "No date was provided for when the property was first occupied or for when the local council registered the property for council tax." )
      assert(paragraphs.last.text() contains "You need to enter one or both of these dates to continue your application." )
      assert(document.getElementsByClass("govuk-button").text() === "Continue application")

      Then("The back link is correct")
      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }

    Scenario("describe missing date requirements when editing a return") {

      Given("the client has failed to enter the sufficient information")
      When("The user doesnt enter either start date")
      val html = injectedViewInstance("1", Html(""), Some(utils.AtedUtils.EDIT_SUBMITTED), Some("http://backLink"))
      val document = Jsoup.parse(html.toString())
      Then("The title should match - No start date was provided - Submit and view your ATED returns - GOV.UK")
      assert(document.title() === "No start date was provided - Submit and view your ATED returns - GOV.UK")

      Then("The header should match - No start date was provided")
      assert(document.getElementsByTag("h1").text() contains "No start date was provided")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      val paragraphs = document.getElementsByTag("p")
      assert(paragraphs.first.text() contains "No date was provided for when the property was first occupied or for when the local council registered the property for council tax." )
      assert(paragraphs.last.text() contains "You need to enter one or both of these dates to continue your application." )
      assert(document.getElementsByClass("govuk-button").text() === "Continue application")

      Then("The back link is correct")
      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }
}
