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
import views.html.propertyDetails.dateFirstOccupiedKnown
import forms.PropertyDetailsForms.dateFirstOccupiedKnownForm

class DateFirstOccupiedKnownSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val title: String = "Do you know when the property was first occupied?"
  val injectedViewInstance: dateFirstOccupiedKnown = app.injector.instanceOf[views.html.propertyDetails.dateFirstOccupiedKnown]

  Feature("The user views the Do you know when the property was first occupied page") {

    info("as a client I want to view the Do you know when the property was first occupied page")

    Scenario("describe the page") {

      Given("the client has not entered any information")
      When("The client views the page")
      val html = injectedViewInstance("1", dateFirstOccupiedKnownForm, None, Html(""), Some("http://backLink"))
      val document = Jsoup.parse(html.toString())
      Then(s"The title should match - $title - Submit and view your ATED returns - GOV.UK")
      assert(document.title() === s"$title - Submit and view your ATED returns - GOV.UK")

      Then("The header should match")
      assert(document.getElementsByTag("h1").text() contains title)

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return")

      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }

    Scenario("describe the page when editing a return") {

      Given("the client has not entered any information")
      When("The client views the page")
      val html = injectedViewInstance("1", dateFirstOccupiedKnownForm, Some(utils.AtedUtils.EDIT_SUBMITTED), Html(""), Some("http://backLink"))
      val document = Jsoup.parse(html.toString())
      Then("The title should match")
      assert(document.title() === s"$title - Submit and view your ATED returns - GOV.UK")

      Then("The header should match")
      assert(document.getElementsByTag("h1").text() contains title)

      Then("The subheader should match")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      assert(document.getElementsByClass("govuk-button").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }
}
