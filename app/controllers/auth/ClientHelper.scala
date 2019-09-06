/*
 * Copyright 2019 HM Revenue & Customs
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
import models.StandardAuthRetrievals
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants._

import scala.concurrent.Future

trait ClientHelper extends AtedBaseController{

  def dataCacheConnector: DataCacheConnector

  //TODO: ClientHelper test?
  def ensureClientContext(result: Future[Result])
                         (implicit authorisedRequest: StandardAuthRetrievals, req: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    dataCacheConnector.fetchAtedRefData[String](DelegatedClientAtedRefNumber) flatMap {
      case refNo @ Some(_) if refNo == authorisedRequest.atedReferenceNumber => result
      case test => Logger.warn(s"[ClientHelper][compareClient] - Client different from context")
        Future.successful(Ok(views.html.global_error(Messages("ated.selected-client-error.wrong.client.header"),
          Messages("ated.selected-client-error.wrong.client.title"),
          Messages("ated.selected-client-error.wrong.client.message"))))
    }
  }

}
