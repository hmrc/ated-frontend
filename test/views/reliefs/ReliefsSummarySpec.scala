/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import testhelpers.MockAuthUtil
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class ReliefsSummarySpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
implicit lazy val authContext = organisationStandardRetrievals

  feature("The user can view the relief summary page") {

    info("As a client I want to be able to view my relief return summary")

    scenario("show the summary of the relief return during the draft period (month of March)") {

      Given("the client has created a new relief return and is viewing the summary of entered info")
      When("The user views the page")

      val html = views.html.reliefs.reliefsSummary(2015, None, canSubmit = false, isComplete = true, None)

      val document = Jsoup.parse(html.toString())

      Then("The text for disabling the submit button should be visible")
      assert(document.getElementById("submit-disabled-text").text() contains "You cannot submit returns until 1 April.")
    }
  }
}
