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
import models.{ReturnType, StandardAuthRetrievals}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.test.Injecting
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class DataCacheConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with Injecting {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("test")))
  implicit val ec: ExecutionContext = inject[ExecutionContext]
  val mockSessionCache: SessionCache = mock[SessionCache]
  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]

  class Setup extends ConnectorTest {
    val testDataCacheConnector: DataCacheConnector = new DataCacheConnector(
      mockHttpClient,
      mockAppConfig
    )
  }

  val returnType = ReturnType(Some("CR"))

  "DataCacheConnector" must {

    "saveFormData" must {
      "save form data in keystore" in new Setup {
        val returnedCacheMap = CacheMap("form-id", Map("test" -> Json.toJson(returnType)))
        when(requestBuilderExecute[CacheMap]).thenReturn(Future.successful(returnedCacheMap))

        await(testDataCacheConnector.saveFormData[ReturnType]("form-id", returnType)) must be(returnType)

        val result: Future[ReturnType] = testDataCacheConnector.saveFormData[ReturnType]("form-id", returnType)
        await(result) must be(returnType)
      }
    }

    "fetchAndGetFormData" must {
      "fetch data from Keystore" in new Setup {
        when(requestBuilderExecute[CacheMap]).thenReturn(Future.successful(CacheMap("test", Map("form-id" -> Json.toJson(returnType)))))

        await(testDataCacheConnector.fetchAndGetFormData[ReturnType]("form-id")) must be(Some(returnType))
      }
    }

    "clear the data" must {
      "clear data from Keystore" in new Setup {
        val successResponse: JsValue = Json.parse("""{"processingDate": "2001-12-17T09:30:47Z"}""")

        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(200, successResponse.toString)))

        val result: Future[Unit] = testDataCacheConnector.clearCache()
        await(result) must be(())
      }
    }

    "fetchAtedRefData" must {
      "fetch data from Keystore" in new Setup {
        when(requestBuilderExecute[CacheMap]).thenReturn(Future.successful(CacheMap("test", Map("form-id" -> JsString("XN1200000100001")))))

        val result: Future[Option[String]] = testDataCacheConnector.fetchAtedRefData[String]("form-id")
        await(result) must be(Some("XN1200000100001"))
      }
    }
  }
}
