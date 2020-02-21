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

package views.html.helpers

import config.ApplicationConfig
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.{FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import testhelpers.MockAuthUtil

class peakGuidanceSpec extends FeatureSpec with GivenWhenThen
  with MockAuthUtil with GuiceOneAppPerTest {

  implicit val request = FakeRequest()
  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = MessagesImpl(Lang("en-GB"), messagesApi)

  feature("The user is viewing returns guidance during peak") {

    lazy val view = views.html.helpers.peakGuidance(
      duringPeak = true,
      currentYear = 2020,
      taxYearStartingYear = 2019
    )
    lazy implicit val document: Document = Jsoup.parse(view.body)

    scenario("User views the returns guidance during peak") {

      Given("the user views the return guidance")
      When("the current date is during peak period")
      Then("the peak period content is displayed")

      assert(document.select("strong").text() === "Deadline for 2020 to 2021 returns: 30 April 2020.")
      assert(document.select("p").first.text() === "This is the deadline for returns and payments for all " +
        "ATED-eligible properties that you own on 1 April 2020.")
      assert(document.select("p").last().text() === "Returns for newly acquired ATED properties must be " +
        "sent to HMRC within 30 days of the date of acquisition (90 days from start date for new builds).")

    }
  }

  feature("The user is viewing returns outside of peak") {

    lazy val view = views.html.helpers.peakGuidance(
      duringPeak = false,
      currentYear = 2020,
      taxYearStartingYear = 2020
    )

    lazy implicit val document: Document = Jsoup.parse(view.body)

    scenario("The user is viewing returns guidance outside of peak") {

      Given("the user views the return guidance")
      When("the current date is outside of peak period")
      Then("the outside of peak period guidance is displayed")

      assert(document.select("strong").text() === "Returns for newly acquired ATED properties " +
        "must be sent to HMRC within 30 days (90 days for new builds).")
      assert(document.select("p").first.text() === "Returns for 2021 to 2022 for all properties in the " +
        "scope of ATED are due by 30 April 2021.")

    }
  }
}
