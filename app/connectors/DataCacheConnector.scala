/*
 * Copyright 2022 HM Revenue & Customs
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
import javax.inject.Inject
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataCacheConnector @Inject()(val http: DefaultHttpClient,
                                   appConfig: ApplicationConfig) extends SessionCache {

  val baseUri: String = appConfig.baseUri
  val defaultSource: String = appConfig.defaultSource
  val domain: String = appConfig.domain

  def saveFormData[T](formId: String, data: T)(implicit hc: HeaderCarrier, formats: Format[T]): Future[T] = {
    cache[T](formId, data) map { _ => data }
  }

  def fetchAndGetFormData[T](formId: String)(implicit hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] = {
    fetchAndGetEntry[T](key = formId)
  }

  def fetchAtedRefData[T](formId: String)(implicit hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] = {
    fetchAndGetEntry[T](key = formId)
  }

  def clearCache()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    remove()
  }

}
