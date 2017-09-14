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
import models.ReturnType
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

  val returnType = ReturnType(Some("CR"))

  "DataCacheConnector" must {
    import AuthBuilder._

    implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))

    "saveFormData" must {
      "save form data in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(returnType)))
        when(mockSessionCache.cache[ReturnType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        await(TestDataCacheConnector.saveFormData[ReturnType]("form-id", returnType)) must be(returnType)
        val result = TestDataCacheConnector.saveFormData[ReturnType]("form-id", returnType)
        await(result) must be(returnType)
      }
    }
//    "fetchClientData" must {
//      "fetch client data from Keystore" in {
//        implicit val hc: HeaderCarrier = HeaderCarrier()
//        when(mockSessionCache.fetchAndGetEntry[ReturnType](Matchers.contains("form-id"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(returnType)))
//        await(TestDataCacheConnector.fetchAndGetFormData[ReturnType]("form-id")) must be(Some(returnType))
//      }
//    }
    "fetchAndGetFormData" must {
      "fetch data from Keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.fetchAndGetEntry[ReturnType](Matchers.contains("form-id"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(returnType)))
        await(TestDataCacheConnector.fetchAndGetFormData[ReturnType]("form-id")) must be(Some(returnType))
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

    "fetchAtedRefData" must {
      "fetch data from Keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.fetchAndGetEntry[String](Matchers.contains("XN1200000100001"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
        val result = TestDataCacheConnector.fetchAtedRefData[String]("XN1200000100001")
        await(result) must be(Some("XN1200000100001"))
      }
    }

  }

}
