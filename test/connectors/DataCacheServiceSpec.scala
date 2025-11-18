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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.{ExecutionContext, Future}

class DataCacheServiceSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with Injecting {

  implicit val hc: HeaderCarrier                   = HeaderCarrier(sessionId = Some(SessionId("test")))
  implicit val ec: ExecutionContext                = inject[ExecutionContext]
  val mockAppConfig: ApplicationConfig             = app.injector.instanceOf[ApplicationConfig]
  val mockSessionCacheRepo: SessionCacheRepository = mock[SessionCacheRepository]
  implicit val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]

  class Setup extends ConnectorTest {

    val testDataCacheService: DataCacheService = new DataCacheService(
      mockSessionCacheRepo
    )

  }

  val returnType = ReturnType(Some("CR"))

  "DataCacheService" must {

    "saveFormData" must {
      "save form data in keystore" in new Setup {
        when(
          mockSessionCacheRepo
            .putSession[ReturnType](DataKey(any), ReturnType(any()))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(returnType))

        await(testDataCacheService.saveFormData[ReturnType]("form-id", returnType)) must be(returnType)

        val result: Future[ReturnType] = testDataCacheService.saveFormData[ReturnType]("form-id", returnType)
        await(result) must be(returnType)
      }
    }

    "fetchAndGetFormData" must {
      "fetch data from Keystore" in new Setup {
        when(
          mockSessionCacheRepo
            .getFromSession[ReturnType](DataKey(any))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(returnType)))

        await(testDataCacheService.fetchAndGetData[ReturnType]("form-id")) must be(Some(returnType))
      }
    }

    "clear the data" must {
      "clear data from Keystore" in new Setup {
        when(
          mockSessionCacheRepo
            .deleteFromSession(ArgumentMatchers.any()))
          .thenReturn(Future.successful(()))

        val result: Future[Unit] = testDataCacheService.clearCache()
        await(result) must be(())
      }
    }

    "fetchAtedRefData" must {
      "fetch data from Keystore" in new Setup {
        when(
          mockSessionCacheRepo
            .getFromSession[String](DataKey(any))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some("XN1200000100001")))

        val result: Future[Option[String]] = testDataCacheService.fetchAndGetData[String]("form-id")
        await(result) must be(Some("XN1200000100001"))
      }
    }
  }

}
