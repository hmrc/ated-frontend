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

package views.reliefs

import config.ApplicationConfig
import config.featureswitch.FeatureSwitch
import models.{Reliefs, ReliefsTaxAvoidance, TaxAvoidance}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil

class ReliefsPrintFriendlySpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance = app.injector.instanceOf[views.html.reliefs.reliefsPrintFriendly]
  implicit lazy val authContext = organisationStandardRetrievals

  feature("The user can view the relief print summary page") {

    info("As a client I want to be able to view my relief return summary")

    scenario("show the summary of the relief return during the draft period (month of March)") {

      Given("the client has created a new relief return and is viewing the summary of entered info")
      When("The user views the page")

      val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefsTaxAvoidance("123456", 2015, Reliefs(
        2015, socialHousing = true, socialHousingDate = Some(LocalDate.parse("2015-04-01"))
      ), TaxAvoidance(), LocalDate.now(), LocalDate.now())

      val html = injectedViewInstance(2015, Some(reliefsTaxAvoidance), isComplete = true, None)

      val document = Jsoup.parse(html.toString())

      assert(document.getElementById("socialHousing").text() contains "Social housing")
    }

    scenario("show the summary of the relief return during the draft period (month of March) in 2020 with the feature switch enabled") {

      Given("the client has created a new relief return and is viewing the summary of entered info")
      When("The user views the page")

      mockAppConfig.enable(FeatureSwitch.CooperativeHousing)

      val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefsTaxAvoidance("123456", 2020, Reliefs(
        2020, socialHousing = true, socialHousingDate = Some(LocalDate.parse("2020-04-01"))
      ), TaxAvoidance(), LocalDate.now(), LocalDate.now())

      val html = injectedViewInstance(2020, Some(reliefsTaxAvoidance), isComplete = true,  None)

      val document = Jsoup.parse(html.toString())

      assert(document.getElementById("socialHousing").text() contains "Provider of social housing or housing co-operative")
    }
  }
}
