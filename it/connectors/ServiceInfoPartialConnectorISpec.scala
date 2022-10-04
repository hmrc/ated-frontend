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

package connectors

import helpers.IntegrationBase
import helpers.stubs.{AuthAudit, ServiceInfoPartialConnectorStub}
import models.requests.{NavContent, NavLinks}
import org.scalatestplus.play.PlaySpec
import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ServiceInfoPartialConnectorISpec extends PlaySpec with IntegrationBase with Injecting with AuthAudit{

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = inject[ExecutionContext]

  lazy val connector: ServiceInfoPartialConnector = inject[ServiceInfoPartialConnector]

  val testNavLinkJson: String =
    """
      |{
      | "home":{
      |         "en" : "Home",
      |         "url": "http://localhost:9020/business-account"
      |       },
      | "account":{
      |           "en" : "Manage account",
      |           "url" : "http://localhost:9020/business-account/manage-account"
      |       },
      | "messages":{
      |             "en" : "Messages",
      |             "url" : "http://localhost:9020/business-account/messages",
      |             "alerts": 5
      |       },
      | "help":{
      |         "en" : "Help and contact",
      |         "url" : "http://localhost:9733/business-account/help"
      |       }
      | }""".stripMargin


  "ServiceInfoPartialConnector" when {

    "Requesting NavLinks Content" should {
      "Return the correct json for Navlinks" in {

        val expectedNavlinks = Some(NavContent(
          home = NavLinks("Home", "http://localhost:9020/business-account"),
          account = NavLinks("Manage account", "http://localhost:9020/business-account/manage-account"),
          messages = NavLinks("Messages", "http://localhost:9020/business-account/messages", Some(5)),
          help = NavLinks("Help and contact", "http://localhost:9733/business-account/help")))

        stubAuthAudit()
        ServiceInfoPartialConnectorStub.withResponseForNavLinks()(200, Some(testNavLinkJson))

        val result: Future[Option[NavContent]] = connector.getNavLinks(ec, hc)

        await(result) mustBe expectedNavlinks

        ServiceInfoPartialConnectorStub.verifyNavlinksContent(1)

      }

      "Return None with failed status" in {
        stubAuthAudit()
        ServiceInfoPartialConnectorStub.withResponseForNavLinks()(500, None)

        val result: Future[Option[NavContent]] = connector.getNavLinks(ec, hc)

        await(result) mustBe None

        ServiceInfoPartialConnectorStub.verifyNavlinksContent(1)

      }
    }
  }



}

