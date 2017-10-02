/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import connectors.BackLinkCacheConnector
import models.AtedContext
import play.api.mvc.{Call, Result}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait BackLinkController extends AtedBaseController {

  val controllerId: String
  val backLinkCacheConnector: BackLinkCacheConnector

  def setBackLink(pageId: String, returnUrl: Option[String])(implicit atedContext: AtedContext, hc: HeaderCarrier) : Future[Option[String]] = {
    backLinkCacheConnector.saveBackLink(pageId, returnUrl)
  }

  def getBackLink(pageId: String)(implicit atedContext: AtedContext, hc: HeaderCarrier):Future[Option[String]] = {
    backLinkCacheConnector.fetchAndGetBackLink(pageId)
  }

  def currentBackLink(implicit atedContext: AtedContext, hc: HeaderCarrier):Future[Option[String]] = {
    getBackLink(controllerId)
  }

  def clearBackLinks(pageIds: List[String]=Nil)(implicit atedContext: AtedContext, hc: HeaderCarrier):Future[List[Option[String]]] = {
    pageIds match {
      case Nil => Future.successful(Nil)
      case _ => backLinkCacheConnector.clearBackLinks(pageIds)
    }
  }

  def ForwardBackLinkToNextPage(nextPageId: String, redirectCall: Call)(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[Result] = {
    for {
      currentBackLink <- currentBackLink
      cache <- setBackLink(nextPageId, currentBackLink)
    } yield{
      Redirect(redirectCall)
    }
  }


  def RedirectWithBackLink(nextPageId: String, redirectCall: Call, backCall: Option[String], pageIds: List[String]=Nil)(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[Result] = {
    for {
      cache <- setBackLink(nextPageId, backCall)
      clearedLinks <- clearBackLinks(pageIds)
    } yield{
      Redirect(redirectCall)
    }
  }

  def RedirectWithBackLinkDontOverwriteOldLink(nextPageId: String, redirectCall: Call, backCall: Option[String])(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[Result] = {
    for {
      oldBackLink <- getBackLink(nextPageId)
      cache <- oldBackLink match {
          case Some(x) => Future.successful(oldBackLink)
          case None => setBackLink(nextPageId, backCall)
        }
    } yield{
      Redirect(redirectCall)
    }
  }
}
