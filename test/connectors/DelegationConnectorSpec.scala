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

import config.ApplicationConfig
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class DelegationConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with Injecting {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = inject[ExecutionContext]
  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  class Setup extends ConnectorTest {
    val testDelegationConnector : DelegationConnector = new DelegationConnector(mockHttpClient, mockAppConfig)
  }

  "DelegationDataCall" should {
    "POST with the correct information" in new Setup {
      when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, "")))

      val testCall: Future[HttpResponse] = testDelegationConnector.delegationDataCall("testID")
      await(testCall).status mustBe OK
    }
  }

}
