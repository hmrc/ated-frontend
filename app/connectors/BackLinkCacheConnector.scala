/*
 * Copyright 2018 HM Revenue & Customs
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

import config.AtedSessionCache
import models.BackLinkModel
import uk.gov.hmrc.http.cache.client.SessionCache

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object BackLinkCacheConnector extends BackLinkCacheConnector {
  val sessionCache: SessionCache = AtedSessionCache
  val sourceId: String = "ATED_Back_Link"
}

trait BackLinkCacheConnector {

  def sessionCache: SessionCache

  def sourceId: String

  private def getKey(pageId: String) = {
    s"$sourceId:$pageId"
  }
  def fetchAndGetBackLink(pageId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    sessionCache.fetchAndGetEntry[BackLinkModel](getKey(pageId)).map(_.flatMap(_.backLink))
  }

  def saveBackLink(pageId: String, returnUrl: Option[String])(implicit hc: HeaderCarrier): Future[Option[String]] = {
    sessionCache.cache[BackLinkModel](getKey(pageId), BackLinkModel(returnUrl)).map(cacheMap => returnUrl)
  }

  def clearBackLinks(pageIds: List[String] = Nil)(implicit hc: HeaderCarrier): Future[List[Option[String]]] = {
    if (!pageIds.isEmpty){
      Future.sequence(pageIds.map{ pageId =>
        saveBackLink(pageId, None)
      })
    }
    else
      Future.successful(Nil)
  }

}
