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

package connectors

import controllers.ControllerBaseSpec
import models.requests.{NavContent, NavLinks}
import org.mockito.Mockito.when
import play.twirl.api.Html
import uk.gov.hmrc.http.GatewayTimeoutException
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class ServiceInfoPartialConnectorSpec extends ControllerBaseSpec {
  val btanl: BtaNavigationLinks = injector.instanceOf[BtaNavigationLinks]
  val validHtml: Html = Html("<nav>BTA lINK</nav>")
  val navLinks: NavLinks = NavLinks("en", "/nav", None)
  val navContent: NavContent = NavContent(navLinks, navLinks, navLinks, navLinks)

  private trait Test extends ConnectorTest {
    val result: Future[Option[NavContent]] = Future.successful(Some(navContent))

    lazy val connector: ServiceInfoPartialConnector = {

      when(requestBuilderExecute[Option[NavContent]]).thenReturn(result)

      when(mockAppConfig.btaBaseUrl).thenReturn("http://localhost:9020")

      new ServiceInfoPartialConnector(mockHttpClient, mockAppConfig)
    }

  }

  "ServiceInfoPartialConnector" should {
    "generate the correct url" in new Test {
      connector.btaNavLinksUrl mustBe "http://localhost:9020/business-account/partial/nav-links"
    }
  }

  "getServiceInfoPartial" when {
    "an unexpected Exception is returned" should {
      "return none" in new Test {
        override val result: Future[Option[NavContent]] = Future.failed(new GatewayTimeoutException(""))
        await(connector.getNavLinks(ec, hc)) mustBe None
      }
    }

    "a successful response is returned" should {
      "return the Some(NavContent)" in new Test {
        override val result: Future[Option[NavContent]] = Future.successful(Some(navContent))
        await(connector.getNavLinks(ec, hc)) mustBe Some(navContent)
      }
    }
  }
}
