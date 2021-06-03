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

package connectors

import config.ApplicationConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentClientMandateFrontendConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  class Setup {
    val testAgentClientMandateFrontendConnector: AgentClientMandateFrontendConnector = new AgentClientMandateFrontendConnector (
      mockAppConfig, mockHttp
    )
  }

  override def beforeEach: Unit = {
    reset(mockHttp)
  }

  "AgentClientMandateFrontendConnector" must {
    "return the partial successfully" in new Setup {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val html = "<h1>helloworld</h1>"
      when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, html)))
      testAgentClientMandateFrontendConnector.getClientBannerPartial("clientId", "ated").map {
        response => response.successfulContentOrEmpty must equal(html)
      }
    }

    "return no partial silently" in new Setup {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))
      testAgentClientMandateFrontendConnector.getClientBannerPartial("clientId", "ated").map {
        response => response.successfulContentOrEmpty must equal(Html(""))
      }
    }

    "return the client mandate details successfully" in new Setup {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))
      val result: Future[HttpResponse] = testAgentClientMandateFrontendConnector.getClientDetails("clientId", "ated")
      await(result).status must be(OK)
    }
  }
}
