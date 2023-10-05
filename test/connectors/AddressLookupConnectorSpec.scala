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

import java.util.UUID

import config.ApplicationConfig
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import testhelpers.MockAuthUtil
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class AddressLookupConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  class Setup {
    val testAddressLookupConnector = new AddressLookupConnector(
      mockAppConfig,
      mockHttp
    )
  }
  override def beforeEach(): Unit = {
    reset(mockHttp)
  }

  "AddressLookupConnector" must {
    val address =  AddressSearchResult(List("line1", "line2"), Some("town"), Some("country"), "postCode", AddressLookupCountry("",""))
    val addressLookupRecord = AddressLookupRecord(1, address)

    "post code lookup" must {

      "retrieve the Addresses Based on Post Code" in new Setup {

        val response = List(addressLookupRecord)
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, List[AddressLookupRecord]]
          (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.successful(response))

        val result: Future[List[AddressLookupRecord]] = testAddressLookupConnector.findByPostcode(AddressLookup("postCode", None))
        await(result) must be(List(addressLookupRecord))
      }

      "return nil if something goes wrong" in new Setup {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, List[AddressLookupRecord]]
          (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.failed(new Exception("")))

        val result: Future[List[AddressLookupRecord]] = testAddressLookupConnector.findByPostcode(AddressLookup("postCode", Some("houseName")))
        await(result).isEmpty must be(true)
      }
    }

    "id lookup" must {

      "retrieve the from the id" in new Setup {

        val response = List(addressLookupRecord)
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, List[AddressLookupRecord]]
          (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.successful(response))

        val result: Future[List[AddressLookupRecord]] = testAddressLookupConnector.findById("1")
        await(result) must be(List(addressLookupRecord))
      }

      "return None if something goes wrong" in new Setup {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, List[AddressLookupRecord]]
          (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.failed(new NotFoundException("")))

        val result: Future[List[AddressLookupRecord]] = testAddressLookupConnector.findById("1")
        await(result).isEmpty must be(true)
      }
    }
  }
}
