/*
 * Copyright 2020 HM Revenue & Customs
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
import models.{ReturnType, StandardAuthRetrievals}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class DataCacheConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("test")))
  val mockSessionCache: SessionCache = mock[SessionCache]
  val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]

  class Setup {
    val testDataCacheConnector: DataCacheConnector = new DataCacheConnector(
      mockHttp,
      mockAppConfig
    )
  }

  val returnType = ReturnType(Some("CR"))

  "DataCacheConnector" must {

    "saveFormData" must {
      "save form data in keystore" in new Setup {
        val returnedCacheMap = CacheMap("form-id", Map("test" -> Json.toJson(returnType)))
        when(mockHttp.PUT[ReturnType, CacheMap]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(returnedCacheMap))

        await(testDataCacheConnector.saveFormData[ReturnType]("form-id", returnType)) must be(returnType)

        val result: Future[ReturnType] = testDataCacheConnector.saveFormData[ReturnType]("form-id", returnType)
        await(result) must be(returnType)
      }
    }

    "fetchAndGetFormData" must {
      "fetch data from Keystore" in new Setup {
        when(mockHttp.GET[CacheMap](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(CacheMap("test", Map("form-id" -> Json.toJson(returnType)))))

        await(testDataCacheConnector.fetchAndGetFormData[ReturnType]("form-id")) must be(Some(returnType))
      }
    }

    "clear the data" must {
      "clear data from Keystore" in new Setup {
        val successResponse: JsValue = Json.parse("""{"processingDate": "2001-12-17T09:30:47Z"}""")
        val returnedCacheMap = CacheMap("test", Map("form-id" -> Json.toJson(returnType)))

        when(mockHttp.DELETE[HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))

        val result: Future[HttpResponse] = testDataCacheConnector.clearCache()
        val response: HttpResponse = await(result)
        response.status must be(OK)
        response.json must be(successResponse)
      }
    }

    "fetchAtedRefData" must {
      "fetch data from Keystore" in new Setup {
        when(mockHttp.GET[CacheMap](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(CacheMap("test", Map("form-id" -> JsString("XN1200000100001")))))

        val result: Future[Option[String]] = testDataCacheConnector.fetchAtedRefData[String]("form-id")
        await(result) must be(Some("XN1200000100001"))
      }
    }
  }
}
