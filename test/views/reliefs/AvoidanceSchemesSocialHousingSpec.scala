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

package views.reliefs

import config.ApplicationConfig
import forms.ReliefForms.taxAvoidanceForm
import models.{Reliefs, ReliefsTaxAvoidance, StandardAuthRetrievals, TaxAvoidance}
import java.time.LocalDate
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
import views.html.reliefs

class AvoidanceSchemesSocialHousingSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance: reliefs.avoidanceSchemes = app.injector.instanceOf[views.html.reliefs.avoidanceSchemes]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  Feature("The user can view the relief avoidance scheme page") {

    info("As a client I want to be able to edit my avoidance schemes for my reliefs")

    Scenario("show the avoidance scheme page with social housing") {

      val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefsTaxAvoidance("123456", 2015, Reliefs(
        2015, socialHousing = true, socialHousingDate = Some(LocalDate.parse("2015-04-01"))
      ), TaxAvoidance(), LocalDate.now(), LocalDate.now())

      val html: Html = injectedViewInstance(2015, taxAvoidanceForm, backLink = None)(Some(reliefsTaxAvoidance))


      val document = Jsoup.parse(html.toString())

      assert(document.getElementsByAttributeValue("for", "socialHousingScheme").text() contains "Social housing Avoidance scheme reference number")
      assert(document.getElementsByAttributeValue("for", "socialHousingSchemePromoter").text() contains "Social housing Promoter reference number")
    }

    Scenario("show the avoidance scheme page with social housing in 2020") {

      val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefsTaxAvoidance("123456", 2020, Reliefs(
        2020, socialHousing = true, socialHousingDate = Some(LocalDate.parse("2020-04-01"))
      ), TaxAvoidance(), LocalDate.now(), LocalDate.now())

      val html: Html = injectedViewInstance(2020, taxAvoidanceForm, backLink = None)(Some(reliefsTaxAvoidance))

      val document = Jsoup.parse(html.toString())

      assert(document.getElementsByAttributeValue("for", "socialHousingScheme").text() contains "Provider of social housing or housing co-operative Avoidance scheme reference number")
      assert(document.getElementsByAttributeValue("for", "socialHousingSchemePromoter").text() contains "Provider of social housing or housing co-operative Promoter reference number")

    }
  }
}
