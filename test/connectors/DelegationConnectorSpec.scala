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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class DelegationConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockSessionCache: SessionCache = mock[SessionCache]
  val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  class Setup {
    val testDelegationConnector : DelegationConnector = new DelegationConnector(mockHttp, mockAppConfig)
  }

  "DelegationDataCall" should {
    "POST with the correct information" in new Setup {
      when(mockHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val testCall: Future[HttpResponse] = testDelegationConnector.delegationDataCall("testID")
      await(testCall).status mustBe OK
    }
  }

}
