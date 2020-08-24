/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait BackLinkController {

  implicit val ec: ExecutionContext
  val controllerId: String
  val backLinkCacheConnector: BackLinkCacheConnector

  def setBackLink(pageId: String, returnUrl: Option[String])(implicit hc: HeaderCarrier) : Future[Option[String]] = {
    backLinkCacheConnector.saveBackLink(pageId, returnUrl)
  }

  def getBackLink(pageId: String)(implicit hc: HeaderCarrier):Future[Option[String]] = {
    backLinkCacheConnector.fetchAndGetBackLink(pageId)
  }

  def currentBackLink(implicit hc: HeaderCarrier):Future[Option[String]] = {
    getBackLink(controllerId)
  }

  def clearBackLinks(pageIds: List[String]=Nil)(implicit hc: HeaderCarrier):Future[List[Option[String]]] = {
    pageIds match {
      case Nil => Future.successful(Nil)
      case _ => backLinkCacheConnector.clearBackLinks(pageIds)
    }
  }

  def forwardBackLinkToNextPage(nextPageId: String, redirectCall: Call)
                               (implicit hc: HeaderCarrier): Future[Result] = {
    for {
      currentBackLink <- currentBackLink
      _ <- setBackLink(nextPageId, currentBackLink)
    } yield{
      Redirect(redirectCall)
    }
  }

  def redirectWithBackLink(nextPageId: String, redirectCall: Call, backCall: Option[String], pageIds: List[String]=Nil)
                          (implicit hc: HeaderCarrier): Future[Result] = {
    for {
      _ <- setBackLink(nextPageId, backCall)
      _ <- clearBackLinks(pageIds)
    } yield{
      Redirect(redirectCall)
    }
  }

  def redirectWithBackLinkDontOverwriteOldLink(nextPageId: String, redirectCall: Call, backCall: Option[String])
                                              (implicit hc: HeaderCarrier): Future[Result] = {
    for {
      oldBackLink <- getBackLink(nextPageId)
      _ <- oldBackLink match {
          case Some(_) => Future.successful(oldBackLink)
          case None => setBackLink(nextPageId, backCall)
        }
    } yield{
      Redirect(redirectCall)
    }
  }
}
