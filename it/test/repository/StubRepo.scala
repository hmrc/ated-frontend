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

package repository

import play.api.libs.json._
import repositories.CacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import utils.AtedConstants.{DelegatedClientAtedRefNumber, RetrieveReturnsResponseId, RetrieveSubscriptionDataId}

import javax.inject.Singleton
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StubRepo extends CacheRepository with ISpecCacheFixtures {

  private def store[T] = mutable.Map.empty[String, T]

  override def putSession[T: Writes](dataKey: DataKey[T], data: T)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[T] = {
    store.put(dataKey.unwrap, data)
    Future.successful(data)
  }

  override def getFromSession[T: Reads](dataKey: DataKey[T])(implicit hc: HeaderCarrier): Future[Option[T]] = {

    dataKey.unwrap match {
      case RetrieveReturnsResponseId =>
        Future.successful(Some(summaryReturnsModel).map(value => value.asInstanceOf[T]))
      case RetrieveSubscriptionDataId =>
        Future.successful(Some(successData).map(value => value.asInstanceOf[T]))
      case DelegatedClientAtedRefNumber =>
        Future.successful(Some(delegatedClientAtedRefNumber).map(value => value.asInstanceOf[T]))
      case _ => throw new IllegalArgumentException()
    }

  }

  override def deleteFromSession(implicit hc: HeaderCarrier): Future[Unit] = Future.successful(store.clear())
}
