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

package services

import connectors.AtedConnector
import models._
import play.api.Logger
import play.api.http.Status._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier, InternalServerException }

trait FormBundleReturnsService {
  val atedConnector: AtedConnector

  def getFormBundleReturns(formBundleNumber: String)(implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Option[FormBundleReturn]] = {
    atedConnector.retrieveFormBundleReturns(formBundleNumber).map {
      response =>
        response.status match {
          case OK =>
            response.json.asOpt[FormBundleReturn]
          case NOT_FOUND => None
          case BAD_REQUEST =>
            Logger.warn(s"[FormBundleReturnsService] [getFormBundleReturns] BadRequestException: [response.body] = ${response.body}")
            throw new BadRequestException(s"[FormBundleReturnsService] [getFormBundleReturns] " +
              s"Bad Request: Failed to retrieve form bundle return [response.body] = ${response.body}")
          case status =>
            Logger.warn(s"[FormBundleReturnsService] [getFormBundleReturns] [status] = $status && [response.body] = ${response.body}")
            throw new InternalServerException(s"[FormBundleReturnsService] [getFormBundleReturns]" +
              s"Internal Server Exception : Failed to retrieve form bundle return [status] = $status && [response.body] = ${response.body}")
        }
    }
  }

}

object FormBundleReturnsService extends FormBundleReturnsService {
  val atedConnector = AtedConnector
}
