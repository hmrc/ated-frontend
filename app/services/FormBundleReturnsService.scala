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

package services

import connectors.AtedConnector

import javax.inject.Inject
import models._
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{InternalServerException, BadRequestException, HeaderCarrier}

import scala.concurrent.{ExecutionContext, Future}

class FormBundleReturnsService @Inject()(atedConnector: AtedConnector)(implicit ec: ExecutionContext) extends Logging {

  def getFormBundleReturns(formBundleNumber: String)(implicit authContext: StandardAuthRetrievals,
                                                     headerCarrier: HeaderCarrier): Future[Option[FormBundleReturn]] = {
    atedConnector.retrieveFormBundleReturns(formBundleNumber).map {
      response =>
        response.status match {
          case OK =>
            val JSON = Json.parse(response.body.replaceAll("\\s", " "))
            JSON.asOpt[FormBundleReturn]
          case NOT_FOUND => None
          case BAD_REQUEST =>
            logger.warn(s"[FormBundleReturnsService] [getFormBundleReturns] BadRequestException: [response.body] = ${response.body}")
            throw new BadRequestException(s"[FormBundleReturnsService] [getFormBundleReturns] " +
              s"Bad Request: Failed to retrieve form bundle return [response.body] = ${response.body}")
          case status =>
            logger.warn(s"[FormBundleReturnsService] [getFormBundleReturns] [status] = $status && [response.body] = ${response.body}")
            throw new InternalServerException(s"[FormBundleReturnsService] [getFormBundleReturns]" +
              s"Internal Server Exception : Failed to retrieve form bundle return [status] = $status && [response.body] = ${response.body}")
        }
    }
  }
}
