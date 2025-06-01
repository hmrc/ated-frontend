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

package repositories

import config.ApplicationConfig
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.{DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionCacheRepository @Inject() (
    timestampSupport: TimestampSupport
)(implicit
    ec: ExecutionContext,
    appConfig: ApplicationConfig
) {

  private val cacheRepo = new MongoCacheRepository[HeaderCarrier](
    mongoComponent = MongoComponent(appConfig.mongoUri),
    collectionName = "sessions",
    ttl = appConfig.mongoDbExpireAfterMinutes,
    timestampSupport = timestampSupport,
    cacheIdType = SessionCacheId
  )

  def putSession[T: Writes](
      dataKey: DataKey[T],
      data: T
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[T] =
    cacheRepo
      .put[T](hc)(dataKey, data)
      .map(_ => data)

  def getFromSession[T: Reads](dataKey: DataKey[T])(implicit hc: HeaderCarrier): Future[Option[T]] =
    cacheRepo.get[T](hc)(dataKey)

}
