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
import models.BackLinkModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.test.Injecting
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.{ExecutionContext, Future}

class BackLinkCacheConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with Injecting {

  implicit val hc: HeaderCarrier                   = HeaderCarrier(sessionId = Some(SessionId("test")))
  implicit val ec: ExecutionContext                = inject[ExecutionContext]
  val mockSessionCache: SessionCache               = mock[SessionCache]
  val mockAppConfig: ApplicationConfig             = app.injector.instanceOf[ApplicationConfig]
  val mockSessionCacheRepo: SessionCacheRepository = mock[SessionCacheRepository]

  class Setup extends ConnectorTest {

    val testBackLinkCacheConnector: BackLinkCacheConnector = new BackLinkCacheConnector(
      mockSessionCacheRepo
    )

  }

  "BackLinkCacheConnector" must {

    "fetchAndGetBackLink" must {

      "fetch saved BusinessDetails from SessionCache" in new Setup {
        val backLink: Option[BackLinkModel] = Some(BackLinkModel(Some("testBackLink")))

        when(
          mockSessionCacheRepo
            .getFromSession[BackLinkModel](DataKey(any))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(backLink))

        when(requestBuilderExecute[CacheMap]).thenReturn(
          Future.successful(CacheMap("test", Map("ATED_Back_Link:testPageId" -> Json.toJson(BackLinkModel(Some("testBackLink")))))))

        val result: Future[Option[String]] = testBackLinkCacheConnector.fetchAndGetBackLink("testPageId")
        await(result) must be(backLink.get.backLink)
      }
    }

    "saveAndReturnBusinessDetails" must {

      "save the back link" in new Setup {
        val backLink: BackLinkModel = BackLinkModel(Some("testBackLink"))

        when(
          mockSessionCacheRepo
            .putSession[BackLinkModel](DataKey(any), BackLinkModel(any()))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(backLink))

        when(requestBuilderExecute[CacheMap]).thenReturn(
          Future.successful(CacheMap("test", Map("ATED_Back_Link:testPageId" -> Json.toJson(BackLinkModel(Some("testBackLink")))))))

        val result: Future[Option[String]] = testBackLinkCacheConnector.saveBackLink("testPageId", backLink.backLink)
        await(result) must be(backLink.backLink)
      }
    }

    "clearBackLinks" must {

      "clear the back links and we have links" in new Setup {
        val backLink: BackLinkModel = BackLinkModel(Some("testBackLink"))

        when(
          mockSessionCacheRepo
            .putSession[BackLinkModel](DataKey(any), BackLinkModel(any()))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(backLink))

        val result: Future[List[Option[String]]] = testBackLinkCacheConnector.clearBackLinks(List("testPageId", "testPageId2"))
        await(result) must be(List(None, None))

      }

      "clear the back link details when we have none" in new Setup {
        val result: Future[List[Option[String]]] = testBackLinkCacheConnector.clearBackLinks(Nil)
        await(result) must be(Nil)

      }
    }
  }

}
