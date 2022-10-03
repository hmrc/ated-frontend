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

import controllers.ControllerBaseSpec
import models.requests.{NavContent, NavLinks}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.http.{GatewayTimeoutException, HttpClient}
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.play.partials.HtmlPartial.{Failure, Success}
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class ServiceInfoPartialConnectorSpec extends ControllerBaseSpec {
  val header: AtedHeaderCarrierForPartialsConverter = injector.instanceOf[AtedHeaderCarrierForPartialsConverter]
  val btanl: BtaNavigationLinks = injector.instanceOf[BtaNavigationLinks]
  val validHtml: Html = Html("<nav>BTA lINK</nav>")
  val navLinks = NavLinks("en", "/nav", None)
  val navContent = NavContent(navLinks, navLinks, navLinks, navLinks, navLinks)

  private trait Test {
    val result: Future[Option[NavContent]] = Future.successful(Some(navContent))
    val mockHttp: HttpClient = mock[HttpClient]


    lazy val connector: ServiceInfoPartialConnector = {

      when(mockHttp.GET[Option[NavContent]](any(), any(), any())(any(), any(), any()))
        .thenReturn(result)

      new ServiceInfoPartialConnector(mockHttp, mockAppConfig)
    }

  }

  "ServiceInfoPartialConnector" should {
    "generate the correct url" in new Test {
      connector.btaNavLinksUrl mustBe "/business-account/partial/service-info"
    }
  }

  "getServiceInfoPartial" when {
    "a connectionExceptionsAsHtmlPartialFailure error is returned" should {
      "return the fall back partial" in new Test {
        override val result: Future[Option[NavContent]] = Future.failed(new GatewayTimeoutException(""))
        await(connector.getNavLinks(ec, hc) mustBe None)
      }
    }

    "an unexpected Exception is returned" should {
      "return the fall back partial" in new Test {
        override val result: Future[Failure] = Future.successful(Failure(Some(Status.INTERNAL_SERVER_ERROR)))
        await(connector.getServiceInfoPartial()) mustBe btanl()
      }
    }
    //temporary solution to the BTA banner
    "a successful response is returned" should {
      "return the Bta partial" in new Test {
        await(connector.getServiceInfoPartial()) mustBe validHtml
      }
    }
  }
}