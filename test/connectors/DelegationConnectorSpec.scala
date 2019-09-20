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

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import uk.gov.hmrc.http._

import scala.concurrent.Future

class DelegationConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait MockedVerbs extends CoreGet with CorePost with CoreDelete
  val mockWSHttp: CoreGet with CorePost with CoreDelete = mock[MockedVerbs]

  object TestDelegationConnector extends DelegationConnector {
    override val serviceURL: String = "delegationURl"
    val http: CoreGet with CorePost with CoreDelete = mockWSHttp
  }


  "DelegationDataCall" should {
    "POST with the correct information" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val testCall: Future[HttpResponse] = TestDelegationConnector.delegationDataCall("testID")
      await(testCall).status mustBe OK
    }
  }

}
