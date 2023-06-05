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

package views.error

import config.ApplicationConfig
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.error.individual

class individualViewSpec extends PlaySpec with GuiceOneAppPerSuite {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  val injectedViewInstance: individual = app.injector.instanceOf[views.html.error.individual]

  "individual" must {
    val html = injectedViewInstance()
    val document = Jsoup.parse(html.toString())
    "have the correct title" in {
      document.title mustBe "There has been a problem - GOV.UK"
    }
    "have the correct heading" in {
      document.select("h1").text mustBe "There has been a problem"
    }
    "have the correct content" in {
      document.select("main p").get(0).text mustBe "You have tried to sign in to your ATED account using your Self Assessment Government Gateway user ID."
      document.select("main p").get(1).text mustBe "If you are an overseas landlord or client you need to sign out and then sign in using the Government Gateway user ID for your business to access your ATED account."
    }
    "have a link styled like a button" in {
      document.getElementById("startAgain").text mustBe "Sign out"
      document.getElementById("startAgain").attr("href") mustBe "/ated/sign-out"
      document.getElementById("startAgain").hasClass("govuk-button") mustBe true
    }
  }
}
