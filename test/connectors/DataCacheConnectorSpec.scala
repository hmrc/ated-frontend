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

import builders.AuthBuilder
import models.AppointAgent
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class DataCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockSessionCache = mock[SessionCache]

  object TestDataCacheConnector extends DataCacheConnector {
    override val sessionCache: SessionCache = mockSessionCache
  }

  override def beforeEach = {
    reset()
  }

  "DataCacheConnector" must {
    import AuthBuilder._

    implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
    "saveClientData" must {
      "save data in keystore for a particular client" in {
        val appointAgent = AppointAgent(agentReferenceNumber = "JARN1234567")
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(appointAgent)))
        when(mockSessionCache.cache[AppointAgent](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveFormData[AppointAgent]("form-id", appointAgent)
        await(result) must be(appointAgent)
      }
    }
    "saveFormData" must {
      "save form data in keystore" in {
        val appointAgent = AppointAgent(agentReferenceNumber = "JARN1234567")
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(appointAgent)))
        when(mockSessionCache.cache[AppointAgent](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveFormData[AppointAgent]("form-id", appointAgent)
        await(result) must be(appointAgent)
      }
    }
    "fetchClientData" must {
      "fetch client data from Keystore" in {
        val appointAgent = AppointAgent(agentReferenceNumber = "JARN1234567")
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.fetchAndGetEntry[AppointAgent](Matchers.contains("form-id"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(appointAgent)))
        val result = TestDataCacheConnector.fetchClientData[AppointAgent]("form-id")
        await(result) must be(Some(appointAgent))
      }
    }
    "fetchAndGetFormData" must {
      "fetch data from Keystore" in {
        val appointAgent = AppointAgent(agentReferenceNumber = "JARN1234567")
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.fetchAndGetEntry[AppointAgent](Matchers.contains("form-id"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(appointAgent)))
        val result = TestDataCacheConnector.fetchAndGetFormData[AppointAgent]("form-id")
        await(result) must be(Some(appointAgent))
      }
    }
    "clear the data" must {
      "clear data from Keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val successResponse = Json.parse("""{"processingDate": "2001-12-17T09:30:47Z"}""")
        when(mockSessionCache.remove()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        val result = TestDataCacheConnector.clearCache()
        val response = await(result)
        response.status must be(OK)
        response.json must be(successResponse)
      }
    }
    "saveAtedRefData" must {
      "save data in keystore for a particular client" in {
        val appointAgent = AppointAgent(agentReferenceNumber = "JARN1234567")
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson("XN1200000100001")))
        when(mockSessionCache.cache[String](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveFormData[String]("form-id", "XN1200000100001")
        await(result) must be("XN1200000100001")
      }
    }
    "fetchAtedRefData" must {
      "fetch data from Keystore" in {
        val appointAgent = AppointAgent(agentReferenceNumber = "JARN1234567")
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.fetchAndGetEntry[String](Matchers.contains("XN1200000100001"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
        val result = TestDataCacheConnector.fetchAtedRefData[String]("XN1200000100001")
        await(result) must be(Some("XN1200000100001"))
      }
    }

  }

}
