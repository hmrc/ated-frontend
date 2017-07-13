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
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AtedConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SummaryReturnsService {

  def atedConnector: AtedConnector

  def dataCacheConnector: DataCacheConnector

  def getSummaryReturns(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[SummaryReturnsModel] = {
    def convertSeqOfPeriodSummariesToObject(x: Seq[PeriodSummaryReturns]): PeriodSummaryReturns = {
      val allDrafts = x.flatMap(a => a.draftReturns)
      val allSubmitted = x.flatMap(a => a.submittedReturns)
      val currentLiabilities = allSubmitted.flatMap(a => a.currentLiabilityReturns)
      val oldLiabilities = allSubmitted.flatMap(a => a.oldLiabilityReturns)
      val allReliefs = allSubmitted.flatMap(a => a.reliefReturns)
      val submitted = SubmittedReturns(x.head.periodKey, allReliefs, currentLiabilities, oldLiabilities)
      PeriodSummaryReturns(x.head.periodKey, allDrafts, Some(submitted))
    }

    for {
      cachedReturns <- dataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](RetrieveReturnsResponseId)
      summaryReturns: SummaryReturnsModel <- {
        cachedReturns match {
          case Some(x) => atedConnector.getPartialSummaryReturns map {
            response =>
              response.status match {
                case OK =>
                  val returnsResponse = response.json.as[SummaryReturnsModel]
                  val allPsrs: Seq[PeriodSummaryReturns] = x.allReturns ++ returnsResponse.allReturns
                  val allPeriodKeys: Seq[Int] = x.allReturns.map(_.periodKey) ++ returnsResponse.allReturns.map(_.periodKey)
                  if (allPeriodKeys.nonEmpty) {
                    val allPKsSorted = allPeriodKeys.distinct.sortWith(_.toInt > _.toInt) // all period keys must be integers
                    val periodSummary = allPKsSorted.map(a => allPsrs.filter(_.periodKey == a)).map(a => convertSeqOfPeriodSummariesToObject(a))
                    val summaryReturnsModel = x.copy(allReturns = periodSummary)
                    summaryReturnsModel
                  } else {
                    x
                  }
                case status =>
                  Logger.warn(s"[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - status: $status & body = ${response.body} & cache = $x")
                  throw new RuntimeException("[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - Status other than 200 returned - Has Cache")
              }
          }
          case None => atedConnector.getFullSummaryReturns flatMap {
            response =>
              response.status match {
                case OK =>
                  val resp = response.json.as[SummaryReturnsModel]
                  val summaryReturnsToCache = resp.copy(allReturns = resp.allReturns.filter(x => x.periodKey > 2000).map(a => a.copy(draftReturns = Nil))) // this would be cached in keystore
                  dataCacheConnector.saveFormData[SummaryReturnsModel](RetrieveReturnsResponseId, summaryReturnsToCache) map (summaryReturn =>
                    resp.copy(allReturns = resp.allReturns.filter(r => r.periodKey > 2000)))
                case status =>
                  Logger.warn(s"[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - status: $status & body = ${response.body} & cache = None")
                  throw new RuntimeException(s"[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - Status other than 200 returned - No Cache, status: $status")
              }
          }
        }
      }
    } yield summaryReturns
  }

  def getPeriodSummaryReturns(period: Int)(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[Option[PeriodSummaryReturns]] = {
    for {
      summaryReturnsModel <- getSummaryReturns
    } yield {
      summaryReturnsModel.allReturns.find(_.periodKey == period)
    }
  }

  def getPreviousSubmittedLiabilityDetails(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[Seq[PreviousReturns]] = getSummaryReturns.flatMap { returnSummary =>
    val periodSummaryReturns = returnSummary.allReturns
    val submittedReturns = periodSummaryReturns.flatMap(x => x.submittedReturns)
    val oldLiabilityReturns = submittedReturns.flatMap(x => x.oldLiabilityReturns)
    val newLiabilityReturns = submittedReturns.flatMap(x => x.currentLiabilityReturns)
    val pastReturnDetails = (oldLiabilityReturns ++ newLiabilityReturns) map (r => PreviousReturns(r.description, r.formBundleNo))
    savePastReturnDetails(pastReturnDetails)
  }

  private def savePastReturnDetails(pastReturnDetails: Seq[PreviousReturns])(implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Seq[PreviousReturns]] = {
    dataCacheConnector.saveFormData[Seq[PreviousReturns]](PreviousReturnsDetailsList, pastReturnDetails)
  }

  def retrieveCachedPreviousReturnAddressList(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[Option[Seq[PreviousReturns]]] = {
    dataCacheConnector.fetchAndGetFormData[Seq[PreviousReturns]](PreviousReturnsDetailsList)
  }

}

object SummaryReturnsService extends SummaryReturnsService {
  val atedConnector = AtedConnector
  val dataCacheConnector = DataCacheConnector
}
