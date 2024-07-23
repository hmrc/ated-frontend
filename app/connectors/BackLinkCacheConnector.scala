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

import javax.inject.Inject
import models.BackLinkModel
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.client.HttpClientV2
import scala.concurrent.{ExecutionContext, Future}

class BackLinkCacheConnector @Inject()(val http: HttpClientV2,
                                       appConfig: ApplicationConfig)
                                      (implicit ec: ExecutionContext)extends SessionCache {

  val baseUri: String = appConfig.baseUri
  val defaultSource: String = appConfig.defaultSource
  val domain: String = appConfig.domain

  val sourceId: String = "ATED_Back_Link"

  private def getKey(pageId: String) = {
    s"$sourceId:$pageId"
  }
  def fetchAndGetBackLink(pageId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    fetchAndGetEntry[BackLinkModel](getKey(pageId)).map(_.flatMap(_.backLink))
  }

  def saveBackLink(pageId: String, returnUrl: Option[String])(implicit hc: HeaderCarrier): Future[Option[String]] = {
    cache[BackLinkModel](getKey(pageId), BackLinkModel(returnUrl)).map(_ => returnUrl)
  }

  def clearBackLinks(pageIds: List[String] = Nil)(implicit hc: HeaderCarrier): Future[List[Option[String]]] = {
    if (pageIds.nonEmpty) {
      Future.sequence(pageIds.map { pageId =>
        saveBackLink(pageId, None)
      })
    } else {
      Future.successful(Nil)
    }
  }

  def httpClientV2: HttpClientV2 = http
}
