/*
 * Copyright 2025 HM Revenue & Customs
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
import javax.inject.Inject
import models._
import java.time.ZonedDateTime
import play.api.Logging
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants._

import scala.concurrent.{ExecutionContext, Future}

class ChangeLiabilityReturnService @Inject()(mcc: MessagesControllerComponents,
                                             atedConnector: AtedConnector,
                                             dataCacheConnector: DataCacheConnector) extends FrontendController(mcc) with Logging {
  implicit val ec: ExecutionContext = mcc.executionContext


  def retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo: String, fromSelectedPastReturn: Option[Boolean] = None, periodKey: Option[SelectPeriod] = None)
                             (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[PropertyDetails]] = {
    (fromSelectedPastReturn, periodKey) match {
      case (Some(true), Some(x)) =>
        atedConnector.retrieveAndCachePreviousLiabilityReturn(oldFormBundleNo, x.period.get.toInt) map {
          response => response.status match {
            case OK => response.json.asOpt[PropertyDetails]
            case status =>
              logger.warn(s"[ChangeLiabilityReturnService][retrieveLiabilityReturn] - status : $status, body = ${response.body}")
              None
          }
        }
      case _ =>
        atedConnector.retrieveAndCacheLiabilityReturn(oldFormBundleNo) map {
          response => response.status match {
            case OK => response.json.asOpt[PropertyDetails]
            case status =>
              logger.warn(s"[ChangeLiabilityReturnService][retrieveLiabilityReturn] - status : $status, body = ${response.body}")
              None
          }
        }
    }
  }

  def cacheChangeLiabilityReturnHasBankDetails(oldFormBundleNo: String, updatedValue: Boolean)
                                    (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[PropertyDetails]] = {
    atedConnector.cacheDraftChangeLiabilityReturnHasBank(oldFormBundleNo, updatedValue) map {
      response => response.status match {
        case OK => response.json.asOpt[PropertyDetails]
        case _  => None
      }
    }
  }


  def cacheChangeLiabilityReturnBank(oldFormBundleNo: String, updatedValue: BankDetails)
                                    (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[PropertyDetails]]  = {
    atedConnector.cacheDraftChangeLiabilityReturnBank(oldFormBundleNo, updatedValue) map {
      response => response.status match {
        case OK => response.json.asOpt[PropertyDetails]
        case _  => None
      }
    }
  }

  def cacheChangeLiabilityHasUkBankAccount(oldFormBundleNo: String, hasUkBankAccount: Boolean)
                                    (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[PropertyDetails]]  = {
    atedConnector.cacheDraftChangeLiabilityHasUkBankAccount(oldFormBundleNo, hasUkBankAccount) map {
      response => response.status match {
        case OK => response.json.asOpt[PropertyDetails]
        case _  => None
      }
    }
  }

  def submitDraftChangeLiability(oldFormBundleNo: String)
                                (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[EditLiabilityReturnsResponseModel] = {
    atedConnector.submitDraftChangeLiabilityReturn(oldFormBundleNo) flatMap { changeLiabilityResponse =>
      changeLiabilityResponse.status match {
        case OK =>
          dataCacheConnector.clearCache() flatMap { _ =>
              dataCacheConnector.saveFormData[EditLiabilityReturnsResponseModel](formId = SubmitEditedLiabilityReturnsResponseFormId,
                data = changeLiabilityResponse.json.as[EditLiabilityReturnsResponseModel])
          }
        case _ => Future.successful(EditLiabilityReturnsResponseModel(ZonedDateTime.now(), Nil, BigDecimal(0.00)))
      }
    }
  }

}
