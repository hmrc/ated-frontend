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

package controllers.auth

import config.ApplicationConfig
import connectors.DataCacheConnector
import models.StandardAuthRetrievals
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ClientHelper {

  val dataCacheConnector: DataCacheConnector
  val appConfig: ApplicationConfig

  def ensureClientContext(result: Future[Result])
                         (implicit authorisedRequest: StandardAuthRetrievals,
                          req: Request[AnyContent],
                          hc: HeaderCarrier,
                          messages: Messages): Future[Result] = {
    dataCacheConnector.fetchAtedRefData[String](DelegatedClientAtedRefNumber) flatMap {
      case refNo @ Some(_) if refNo.get == authorisedRequest.atedReferenceNumber => result
      case _ => Logger.warn(s"[ClientHelper][compareClient] - Client different from context")
        Future.successful(Ok(appConfig.templateError(
          "ated.selected-client-error.wrong.client.header",
          "ated.selected-client-error.wrong.client.title",
          "ated.selected-client-error.wrong.client.message",
          None,
          Some("ated.selected-client-error.wrong.client.HrefLink"),
          Some("ated.selected-client-error.wrong.client.HrefMessage"),
          Some("ated.selected-client-error.wrong.client.PostHrefMessage"),
          Html(""),
          appConfig
        )))
    }
  }

}
