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
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class ReliefsSummarySpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen {

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
  val userId = s"user-${UUID.randomUUID}"
  implicit val user = createAtedContext(createUserAuthContext(userId, "name"))

  feature("The user can view the relief summary page") {

    info("As a client I want to be able to view my relief return summary")

    scenario("show the summary of the relief return during the draft period (month of March)") {

      Given("the client has created a new relief return and is viewing the summary of entered info")
      When("The user views the page")

      val html = views.html.reliefs.reliefsSummary(2015, None, canSubmit = false, isComplete = true, None)

      val document = Jsoup.parse(html.toString())

      Then("The text for disabling the submit button should be visible")
      assert(document.getElementById("submit-disabled-text").text() contains "You can not submit returns until after the 1 April.")
    }
  }
}
