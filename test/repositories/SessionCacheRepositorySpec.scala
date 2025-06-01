/*
 * Copyright 2025 HM Revenue & Customs
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

package repositories

import org.scalatestplus.play.PlaySpec
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer
import play.api.libs.json.{Json, OFormat}
import play.api.test.Injecting
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.DataKey
import utils.BaseSpec

import scala.util.{Failure, Success, Try}

class SessionCacheRepositorySpec extends PlaySpec with BaseSpec with Injecting {

  val testCon = new MongoDBAtlasLocalContainer(
    "mongodb/mongodb-atlas-local:7.0.9"
  )

  testCon.start()

  val repository = new SessionCacheRepository(
    timestampSupport = new CurrentTimestampSupport()
  )

  case class FakeData(field: String)

  object FakeData {
    implicit val formats: OFormat[FakeData] = Json.format[FakeData]
  }

  "the session cache repository" should {
    "cache data based on the session id" in {
      val data    = FakeData("test")
      val hc      = HeaderCarrier(sessionId = Some(SessionId("SessionId-0000")))
      val wrongHc = HeaderCarrier(sessionId = Some(SessionId("SessionId-xxxx")))
      repository.putSession[FakeData](DataKey[FakeData]("testId"), data)(implicitly, hc, implicitly)

      val result      = repository.getFromSession(DataKey[FakeData]("testId"))(implicitly, hc).futureValue.get
      val resultWrong = repository.getFromSession(DataKey[FakeData]("testId"))(implicitly, wrongHc).futureValue

      result mustBe data
      resultWrong mustBe None
    }

    "cache data based on the page id" in {
      val pageId = "ATED_Back_Link"
      val wrongPageId = "Not_ATED_Back_Link"

      val data    = FakeData("test")
      val hc      = HeaderCarrier(sessionId = Some(SessionId("SessionId-0000")))

      repository.putSession[FakeData](DataKey[FakeData](pageId), data)(implicitly, hc, implicitly)

      val result      = repository.getFromSession(DataKey[FakeData](pageId))(implicitly, hc).futureValue.get
      val resultWrong = repository.getFromSession(DataKey[FakeData](wrongPageId))(implicitly, hc).futureValue

      result mustBe data
      resultWrong mustBe None
    }

    "an exception is thrown" when {
      "A session id is not present" in {
        val data = FakeData("test")
        val hc   = HeaderCarrier(sessionId = None)

        val result = Try(repository.putSession[FakeData](DataKey[FakeData]("testId"), data)(implicitly, hc, implicitly))

        result match {
          case Success(_)         => ???
          case Failure(exception) => exception mustBe a[SessionCacheId.NoSessionException.type]
        }
      }
    }
  }

}
