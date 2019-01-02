/*
 * Copyright 2019 HM Revenue & Customs
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

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentClientMandateFrontendConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  trait MockedVerbs extends CoreGet
  val mockWSHttp: CoreGet = mock[MockedVerbs]

  object TestAgentClientMandateFrontendConnector extends AgentClientMandateFrontendConnector {
    val crypto = SessionCookieCryptoFilter.encrypt _
    override val http = mockWSHttp
  }

  override def beforeEach = {
    reset(mockWSHttp)
  }

  "AgentClientMandateFrontendConnector" must {
    "return the partial successfully" in {
      implicit val request = FakeRequest()
      implicit val hc = HeaderCarrier()
      implicit val hcwc = HeaderCarrierForPartials(hc,"")
      val html = "<h1>helloworld</h1>"
      when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseString = Some(html))))
      TestAgentClientMandateFrontendConnector.getClientBannerPartial("clientId", "ated").map {
        response => response.successfulContentOrEmpty must equal(html)
      }
    }

    "return the client mandate details succcessfully" in {
      implicit val request = FakeRequest()
      implicit val hc = HeaderCarrier()
      implicit val hcwc = HeaderCarrierForPartials(hc,"")
      when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseString = Some(""))))
      val result = TestAgentClientMandateFrontendConnector.getClientDetails("clientId", "ated")
      await(result).status must be(OK)
    }
  }
}
