/*
 * Copyright 2017 HM Revenue & Customs
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

import builders._
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost}
import utils.AtedConstants

import scala.concurrent.Future

class AddressLookupConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  class MockHttp extends WSGet with WSPost with WSDelete {
    override val hooks = NoneRequired
  }

  val mockWSHttp = mock[MockHttp]

  object TestAtedConnector extends AddressLookupConnector {
    override val http: HttpGet with HttpPost with HttpDelete = mockWSHttp
    override val serviceURL = baseUrl("address-lookup")
  }

  override def beforeEach = {
    reset(mockWSHttp)
  }

  "AddressLookupConnector" must {
    import AuthBuilder._
    val address =  AddressSearchResult(List("line1", "line2"), Some("town"), Some("country"), "postCode", AddressLookupCountry("",""))
    val addressLookupRecord = AddressLookupRecord("1", address)

    "post code lookup" must {

      "retrieve the Addresses Based on Post Code" in {

        val response = List(addressLookupRecord)
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.GET[List[AddressLookupRecord]]
          (Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.successful(response))

        val result = TestAtedConnector.findByPostcode(AddressLookup("postCode", None))
        await(result).headOption must be(Some(addressLookupRecord))
      }

      "return nil if something goes wrong" in {

        val response = List(addressLookupRecord)
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.GET[List[AddressLookupRecord]]
          (Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("")))

        val result = TestAtedConnector.findByPostcode(AddressLookup("postCode", Some("houseName")))
        await(result).isEmpty must be(true)
      }
    }

    "id lookup" must {

      "retrieve the from the id" in {

        val response = Some(addressLookupRecord)
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.GET[Option[AddressLookupRecord]]
          (Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.successful(response))

        val result = TestAtedConnector.findById("1")
        await(result) must be(Some(addressLookupRecord))
      }

      "return None if something goes wrong" in {

        val response = List(AddressLookupRecord("1", address))
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.GET[Option[AddressLookupRecord]]
          (Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.failed(new NotFoundException("")))

        val result = TestAtedConnector.findById("1")
        await(result).isDefined must be(false)
      }
    }
  }
}
