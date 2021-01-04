/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import mocks.MockAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.inject.Injector
import play.twirl.api.Html
import views.html.BtaNavigationLinks

class BtaNavigationLinksTemplateSpec extends PlaySpec with MockFactory with GuiceOneAppPerSuite {

  val injector: Injector = app.injector
  val messagesApi: MessagesApi = injector.instanceOf[MessagesApi]
  implicit val mockAppConfig: MockAppConfig = new MockAppConfig(app.configuration)
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  lazy implicit val lang: Lang = injector.instanceOf[Lang]

  def formatHtml(body: Html): String = Jsoup.parseBodyFragment(s"\n$body\n").toString.trim
  val btaNavigationLinksView: BtaNavigationLinks = injector.instanceOf[BtaNavigationLinks]

  val btaHome = "Home"
  val btaMessages = "Messages"
  val btaManageAccount = "Manage account"
  val btaHelpAndContact = "Help and contact"

  "btaNavigationLinks" should {

    val view: Html = btaNavigationLinksView()(messages,mockAppConfig)
    lazy implicit val document: Document = Jsoup.parse(view.body)


    "have a link to BTA home" which {

      lazy val homeLink = document.getElementById("service-info-home-link")

      "should have the text home" in {
        homeLink.text() mustBe btaHome
      }

      "should have a link to home" in {
        homeLink.attr("href") mustBe mockAppConfig.btaHomeUrl
      }

    }

    "have a link to BTA Manage Account" which {

      lazy val manageAccountLink = document.getElementById("service-info-manage-account-link")

      "should have the text Manage account" in {
        manageAccountLink.text() mustBe btaManageAccount
      }

      "should have a link to Manage account" in {
        manageAccountLink.attr("href") mustBe mockAppConfig.btaManageAccountUrl
      }

    }

    "have a link to BTA Messages" which {

      lazy val messagesLink = document.getElementById("service-info-messages-link")

      "should have the text Messages" in {
        messagesLink.text() mustBe btaMessages
      }

      "should have a link to Messages" in {
        messagesLink.attr("href") mustBe mockAppConfig.btaMessagesUrl
      }

    }

    "have a link to BTA Help and contact" which {

      lazy val helpAndContactLink = document.getElementById("service-info-help-and-contact-link")

      "should have the text Help and contact" in {
        helpAndContactLink.text() mustBe btaHelpAndContact
      }

      "should have a link to Help and contact" in {
        helpAndContactLink.attr("href") mustBe mockAppConfig.btaHelpAndContactUrl
      }

    }

  }
}


