/*
 * Copyright 2024 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import testhelpers.MockAuthUtil
import views.html.propertyDetails.propertyDetailsExit

class propertyDetailsExitSpec extends PlaySpec with MockitoSugar with MockAuthUtil with GuiceOneAppPerSuite{

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
 implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedView = app.injector.instanceOf[propertyDetailsExit]
  val view = injectedView()
  val doc = Jsoup.parse(view.toString)

   "propertyDetailsExitSpec" when {
     "Exit page" should {

       "have the correct title" in {
         assert(doc.title() == "You cannot submit this chargeable return - Submit and view your ATED returns - GOV.UK")
       }

       "have the heading 'You cannot submit this chargeable return'" in {
         assert(doc.getElementsByTag("h1").text() == "You cannot submit this chargeable return")
       }

       "have the paragraph The property must be revalued before you can submit this chargeable return. " in {
         assert(doc.getElementsByTag("p").first().text() == "The property must be revalued before you can submit this chargeable return.")
       }
       "have the button with text Back to your ATED summary" in {
         assert(doc.select(".govuk-button").first().text() == "Back to your ATED summary")
       }

     }

   }

}
