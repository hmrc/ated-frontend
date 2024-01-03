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
      document.title mustBe "You need to sign in with a different Gateway ID - Submit and view your ATED returns - GOV.UK"
    }

    "have the correct heading" in {
      document.select("h1").text mustBe "You need to sign in with a different Gateway ID"
    }
    "have the correct content" in {
      document.select("main h2").get(0).text mustBe "UK businesses, trusts and partnerships"
      document.select("main h2").get(1).text mustBe "Non-UK businesses, trusts and partnerships, including non-resident landlords"
      document.select("main p").get(0).text mustBe "You need to use the Gateway ID you use to access Corporation Tax for a company or Self Assessment for a trust or partnership."
      document.select("main p").get(1).text mustBe "If you are registering for ATED for the first time, and you are registering a business based outside the UK, you will need to create an organisation account."
      document.select("main p").get(2).text mustBe "First create a new Gateway ID from this page by selecting Create sign-in details. When you are asked what type of account you require, select Organisation."
      document.select("main p").get(3).text mustBe "The person or agent who registered your business for ATED should sign in."
      document.select("main p").get(4).text mustBe "If you are stuck, use the Get help with this service link to find out who registered your company for ATED and how to get access. Give them your ATED reference number if you have it."
    }
    "have a link to the sign-in page" in {
      document.select("main a").get(0).text mustBe "create a new Gateway ID from this page"
      document.select("main a").get(0).attr("href") mustBe "https://www.tax.service.gov.uk/bas-gateway/sign-in?continue_url=/ated/home/&origin=ated-frontend"
    }
    "have a link styled like a button" in {
      document.select("main a").get(1).text mustBe "Sign out"
      document.select("main a").get(1).attr("href") mustBe "/ated/sign-out-individual"
      document.select("main a").get(1).hasClass("govuk-button") mustBe true
    }
    "have a Gateway ID warning" in {
      document.select("main div").get(2).text mustBe "! Warning Do not set up a new Gateway ID if your business is already registered for ATED"
      document.select("main div").get(2).hasClass("govuk-warning-text") mustBe true
    }
  }
}
