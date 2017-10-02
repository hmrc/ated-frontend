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

package controllers.auth

import connectors.DataCacheConnector
import controllers.AtedBaseController
import models.AtedContext
import play.api.Logger
import play.api.mvc.Result
import utils.AtedConstants._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait ClientHelper extends AtedBaseController{

  def dataCacheConnector: DataCacheConnector

  def ensureClientContext(result: Future[Result])(implicit atedContext: AtedContext, hc: HeaderCarrier) = {
   dataCacheConnector.fetchAtedRefData[String](DelegatedClientAtedRefNumber) flatMap  {
      case Some(refNumber) if(refNumber == atedContext.user.atedReferenceNumber) => result
      case _ =>  Logger.warn(s"[ClientHelper][compareClient] - Client different from context")
        Future.successful(Ok(views.html.global_error(Messages("ated.selected-client-error.wrong.client.header"),
          Messages("ated.selected-client-error.wrong.client.title"),
          Messages("ated.selected-client-error.wrong.client.message"))))
    }
  }

}
