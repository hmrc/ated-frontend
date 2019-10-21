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

package services

import connectors.{AtedConnector, DataCacheConnector}
import javax.inject.Inject
import models._
import play.api.Logger
import play.mvc.Http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import utils.AtedConstants._
import utils.ReliefsUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReliefsService @Inject()(atedConnector: AtedConnector,
                               dataCacheConnector: DataCacheConnector) {

  def saveDraftReliefs(atedRefNo: String, periodKey: Int, reliefs: Reliefs)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier) = {
    for {
      newTaxAvoidanceReliefs <- updateReliefs(atedRefNo, periodKey, reliefs)
      response <- atedConnector.saveDraftReliefs(atedRefNo, newTaxAvoidanceReliefs)
    } yield {
      response.status match {
        case OK => response.json.asOpt[ReliefsTaxAvoidance]
        case status =>
          Logger.warn(s"[ReliefsService][saveDraftReliefs] - Invalid status returned when retrieving all drafts - " +
            s"status = $status, body = ${response.body}")
          throw new InternalServerException(s"[ReliefsService][saveDraftReliefs] - status : $status")
      }
    }
  }

  def saveDraftIsTaxAvoidance(atedRefNo: String, periodKey: Int, isAvoidanceScheme: Boolean)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier) = {
    for {
      newTaxAvoidanceReliefs <- updateIsTaxAvoidance(atedRefNo, periodKey, isAvoidanceScheme)
      response <- atedConnector.saveDraftReliefs(atedRefNo, newTaxAvoidanceReliefs)
    } yield {
      response.status match {
        case OK => response.json.asOpt[ReliefsTaxAvoidance]
        case status =>
          Logger.warn(s"[ReliefsService][saveDraftReliefs] - Invalid status returned when retrieving all drafts - " +
            s"status = $status, body = ${response.body}")
          throw new InternalServerException(s"[ReliefsService][saveDraftReliefs] - status : $status")
      }
    }
  }

  def saveDraftTaxAvoidance(atedRefNo: String, periodKey: Int, taxAvoidance: TaxAvoidance)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier) = {
    for {
      newTaxAvoidanceReliefs <- updateTaxAvoidance(atedRefNo, periodKey, taxAvoidance)
      response <- atedConnector.saveDraftReliefs(atedRefNo, newTaxAvoidanceReliefs)
    } yield {
      response.status match {
        case OK => response.json.asOpt[ReliefsTaxAvoidance]
        case status =>
          Logger.warn(s"[ReliefsService][saveDraftTaxAvoidance] - Invalid status returned when retrieving all drafts - " +
            s"status = $status, body = ${response.body}")
          throw new InternalServerException(s"[ReliefsService][saveDraftTaxAvoidance] - status : $status")
      }
    }
  }

  // FIXME: rename method to retrievePeriodDraftReliefs
  def retrieveDraftReliefs(atedRefNo: String, periodKey: Int)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier):
  Future[Option[ReliefsTaxAvoidance]] = {
    for {
      response <- atedConnector.retrievePeriodDraftReliefs(atedRefNo, periodKey)
    }
      yield {
        response.status match {
          case OK  => response.json.asOpt[ReliefsTaxAvoidance]
          case status =>
            Logger.info(s"[ReliefsService][retrieveDraftReliefs] - Invalid status returned when retrieving all drafts - " +
              s"status = $status, body = ${response.body}")
            None
        }
      }
  }

  def submitDraftReliefs(atedRefNo: String, periodKey: Int)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    for {
      httpResponse <- atedConnector.submitDraftReliefs(atedRefNo, periodKey)
      _ <- dataCacheConnector.clearCache()
      _ <-  dataCacheConnector.saveFormData[SubmitReturnsResponse](formId = SubmitReturnsResponseFormId, data = httpResponse.json.as[SubmitReturnsResponse])
    } yield {
      httpResponse
    }
  }

  def clearDraftReliefs(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = atedConnector.deleteDraftReliefs

  private def updateReliefs(atedRefNo: String, periodKey: Int, reliefs: Reliefs)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier) = {
    for {
      draftReliefs <- retrieveDraftReliefs(atedRefNo, periodKey)
    } yield {
      val reliefsTaxAvoidance = draftReliefs.getOrElse(ReliefsUtils.createReliefsTaxAvoidance(periodKey))
      reliefsTaxAvoidance.copy(reliefs = reliefs)
    }
  }

  private def updateIsTaxAvoidance(atedRefNo: String, periodKey: Int, isAvoidanceScheme: Boolean)
                                  (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier) = {

    for {
      draftReliefs <- retrieveDraftReliefs(atedRefNo, periodKey)
    } yield {
      val reliefsTaxAvoidance = draftReliefs.getOrElse(throw new RuntimeException("[ReliefsService][updateIsTaxAvoidance] : No Draft Relief found"))
      val updatedReliefs = reliefsTaxAvoidance.reliefs.copy(isAvoidanceScheme = Some(isAvoidanceScheme))
      reliefsTaxAvoidance.copy(reliefs = updatedReliefs)
    }
  }

  private def updateTaxAvoidance(atedRefNo: String, periodKey: Int, taxAvoidance: TaxAvoidance)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier) = {
    for {
      draftReliefs <- retrieveDraftReliefs(atedRefNo, periodKey)
    } yield {
      draftReliefs.map { oldReliefs =>
        oldReliefs.copy(taxAvoidance = taxAvoidance)
      }.getOrElse(ReliefsUtils.createReliefsTaxAvoidance(periodKey))
    }
  }

  def viewReliefReturn(periodKey: Int, formBundleNo: String)
                      (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[SubmittedReliefReturns]] = {
    for {
      cachedReturns <- dataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](RetrieveReturnsResponseId)
    } yield {
      cachedReturns match {
        case Some(x) =>
          x.allReturns.flatMap(a => a.submittedReturns).flatMap(b => b.reliefReturns).find(c => c.formBundleNo == formBundleNo)
        case None => None
      }
    }
  }

  def deleteDraftReliefs(periodKey: Int)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    atedConnector.deleteDraftReliefsByYear(periodKey)
  }
}
