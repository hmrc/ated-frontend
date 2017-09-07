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

package services

import connectors.{AtedConnector, DataCacheConnector}
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AtedConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ChangeLiabilityReturnService {

  def atedConnector: AtedConnector

  def dataCacheConnector: DataCacheConnector

  def retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo: String, fromSelectedPastReturn: Option[Boolean] = None, periodKey: Option[SelectPeriod] = None)
                             (implicit atedContext: AtedContext, hc: HeaderCarrier): Future[Option[PropertyDetails]] = {
    (fromSelectedPastReturn, periodKey) match {
      case (Some(true), Some(x)) =>
        atedConnector.retrieveAndCachePreviousLiabilityReturn(oldFormBundleNo, x.period.get.toInt) map {
          response => response.status match {
            case OK => response.json.asOpt[PropertyDetails]
            case status =>
              Logger.warn(s"[ChangeLiabilityReturnService][retrieveLiabilityReturn] - status : $status, body = ${response.body}")
              None
          }
        }
      case _ =>
        atedConnector.retrieveAndCacheLiabilityReturn(oldFormBundleNo) map {
          response => response.status match {
            case OK => response.json.asOpt[PropertyDetails]
            case status =>
              Logger.warn(s"[ChangeLiabilityReturnService][retrieveLiabilityReturn] - status : $status, body = ${response.body}")
              None
          }
        }
    }
  }

  def cacheChangeLiabilityReturnHasBankDetails(oldFormBundleNo: String, updatedValue: Boolean)
                                    (implicit atedContext: AtedContext, hc: HeaderCarrier) = {
    atedConnector.cacheDraftChangeLiabilityReturnHasBank(oldFormBundleNo, updatedValue) map {
      response => response.status match {
        case OK => response.json.asOpt[PropertyDetails]
        case status => None
      }
    }
  }


  def cacheChangeLiabilityReturnBank(oldFormBundleNo: String, updatedValue: BankDetails)
                                    (implicit atedContext: AtedContext, hc: HeaderCarrier) = {
    atedConnector.cacheDraftChangeLiabilityReturnBank(oldFormBundleNo, updatedValue) map {
      response => response.status match {
        case OK => response.json.asOpt[PropertyDetails]
        case status => None
      }
    }
  }

  def submitDraftChangeLiability(oldFormBundleNo: String)(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[EditLiabilityReturnsResponseModel] = {
    atedConnector.submitDraftChangeLiabilityReturn(oldFormBundleNo) flatMap {
      changeLiabilityResponse => changeLiabilityResponse.status match {
        case OK =>
          dataCacheConnector.clearCache() flatMap {
            response =>
              dataCacheConnector.saveFormData[EditLiabilityReturnsResponseModel](formId = SubmitEditedLiabilityReturnsResponseFormId,
                data = changeLiabilityResponse.json.as[EditLiabilityReturnsResponseModel])
          }
        case status => Future.successful(EditLiabilityReturnsResponseModel(DateTime.now(), Nil, BigDecimal(0.00)))
      }
    }
  }

}

object ChangeLiabilityReturnService extends ChangeLiabilityReturnService {
  val atedConnector = AtedConnector
  val dataCacheConnector = DataCacheConnector
}
