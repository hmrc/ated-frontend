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

package services

import config.ApplicationConfig
import connectors.{AtedConnector, DataCacheConnector}
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import utils.{PeriodUtils, ReliefsUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SummaryReturnsService @Inject()(atedConnector: AtedConnector,
                                      dataCacheConnector: DataCacheConnector)
                                     (implicit val appConfig: ApplicationConfig) {

  def getSummaryReturns(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[SummaryReturnsModel] = {
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

                  val allCurrentTaxYearReturns: Seq[PeriodSummaryReturns] = x.returnsCurrentTaxYear ++
                    returnsResponse.returnsCurrentTaxYear

                  val allOtherYearReturns: Seq[PeriodSummaryReturns] = x.returnsOtherTaxYears ++
                    returnsResponse.returnsOtherTaxYears

                  val currentYearPeriodKeys: Seq[Int] = allCurrentTaxYearReturns.map(_.periodKey)
                  val allOtherYearPeriodKeys: Seq[Int] = allOtherYearReturns.map(_.periodKey)

                  val currentYearPeriodSummary =
                    if (currentYearPeriodKeys.nonEmpty) {
                      val allPKsSorted = currentYearPeriodKeys.distinct.sortWith(_.toInt > _.toInt)
                      allPKsSorted.map(
                        a => allCurrentTaxYearReturns.filter(_.periodKey == a)).map(a => convertSeqOfPeriodSummariesToObject(a)
                      )
                    } else {
                      x.returnsCurrentTaxYear
                    }

                  val otherYearsPeriodSummary =
                    if (allOtherYearPeriodKeys.nonEmpty) {
                      val allPKsSorted = allOtherYearPeriodKeys.distinct.sortWith(_.toInt > _.toInt)
                      allPKsSorted.map(
                        a => allOtherYearReturns.filter(_.periodKey == a)).map(a => convertSeqOfPeriodSummariesToObject(a)
                      )

                    } else {
                      x.returnsOtherTaxYears
                    }

                  val summaryReturnsModel = x.copy(
                    returnsCurrentTaxYear = currentYearPeriodSummary,
                    returnsOtherTaxYears = otherYearsPeriodSummary
                  )

                  summaryReturnsModel

                case status =>
                  Logger.warn(s"[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - status: $status " +
                    s"& body = ${response.body} & cache = $x")
                  throw new RuntimeException("[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - " +
                    "Status other than 200 returned - Has Cache")
              }
          }

          case None => atedConnector.getFullSummaryReturns flatMap {
            response =>
              response.status match {
                case OK =>
                  val resp = response.json.as[SummaryReturnsModel]

                  val returnsFilteredCurrentTaxYear: Seq[PeriodSummaryReturns] = resp.returnsCurrentTaxYear.filter(
                    x => x.periodKey > 2000)

                  val returnsFilteredOtherTaxYears: Seq[PeriodSummaryReturns] = resp.returnsOtherTaxYears.filter(
                    x => x.periodKey > 2000)

                  val summaryReturnsToCache = resp.copy(
                    returnsCurrentTaxYear = returnsFilteredCurrentTaxYear.map(a => a.copy(draftReturns = Nil)),
                    returnsOtherTaxYears = returnsFilteredOtherTaxYears.map(a => a.copy(draftReturns = Nil))
                  )

                  dataCacheConnector.saveFormData[SummaryReturnsModel](RetrieveReturnsResponseId,
                    summaryReturnsToCache) map (_ =>
                    resp.copy(
                      returnsCurrentTaxYear = returnsFilteredCurrentTaxYear,
                      returnsOtherTaxYears = returnsFilteredOtherTaxYears)
                    )

                case status =>
                  Logger.warn("[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - " +
                    s"status: $status & body = ${response.body} & cache = None")
                  throw new RuntimeException("[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - " +
                    s"Status other than 200 returned - No Cache, status: $status")
              }
          }
        }
      }
    } yield summaryReturns
  }

  def getPeriodSummaryReturns(period: Int)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[PeriodSummaryReturns]] = {
    for {
      summaryReturnsModel <- getSummaryReturns
    } yield {
      (summaryReturnsModel.returnsOtherTaxYears ++ summaryReturnsModel.returnsCurrentTaxYear).find(_.periodKey == period)
    }
  }

  def filterPeriodSummaryReturnReliefs(periodSummaryReturns: PeriodSummaryReturns, past: Boolean): PeriodSummaryReturns = {
    val optFilteredReliefReturns = periodSummaryReturns.submittedReturns
      .map(_.reliefReturns)
      .map {reliefReturns =>
        val partition = ReliefsUtils.partitionNewestReliefForType(reliefReturns)

        if (past) partition._2 else partition._1
      }

    optFilteredReliefReturns match {
      case Some(filteredReturns) => periodSummaryReturns.copy(
        submittedReturns = periodSummaryReturns.submittedReturns.map(_.copy(reliefReturns = filteredReturns))
      )
      case _ => periodSummaryReturns
    }
  }

  def getPreviousSubmittedLiabilityDetails(selectedPeriodKey: Int)(implicit authContext: StandardAuthRetrievals,
                                                                   hc: HeaderCarrier): Future[Seq[PreviousReturns]] =
    getSummaryReturns.flatMap { returnSummary =>
      val periodSummaryReturns = returnSummary.returnsCurrentTaxYear ++ returnSummary.returnsOtherTaxYears
      val submittedReturns = periodSummaryReturns.flatMap(x => x.submittedReturns).filter(_.periodKey == selectedPeriodKey - 1)
      val oldLiabilityReturns = submittedReturns.flatMap(x => x.oldLiabilityReturns)
      val newLiabilityReturns = submittedReturns.flatMap(x => x.currentLiabilityReturns)
      val pastReturnDetails = (oldLiabilityReturns ++ newLiabilityReturns) map (r => PreviousReturns(r.description, r.formBundleNo, r.dateOfSubmission))
      savePastReturnDetails(pastReturnDetails)
    }

  private def savePastReturnDetails(pastReturnDetails: Seq[PreviousReturns])
                                   (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Seq[PreviousReturns]] = {
    dataCacheConnector.saveFormData[Seq[PreviousReturns]](PreviousReturnsDetailsList, pastReturnDetails)
  }

  def retrieveCachedPreviousReturnAddressList(implicit authContext: StandardAuthRetrievals,
                                              hc: HeaderCarrier): Future[Option[Seq[PreviousReturns]]] = {
    dataCacheConnector.fetchAndGetFormData[Seq[PreviousReturns]](PreviousReturnsDetailsList)
  }

  def generateCurrentTaxYearReturns(returns: Seq[PeriodSummaryReturns]): Future[(Seq[AccountSummaryRowModel], Int, Boolean)] = {

    val currentTaxYear = PeriodUtils.calculatePeriod()

    val submittedReturns = returns flatMap(_.submittedReturns)

    val draftReturns: Seq[AccountSummaryRowModel] = {
      returns.flatMap(_.draftReturns.map(
        rtn => AccountSummaryRowModel(
          description = rtn.description,
          returnType = draftType,
          route = rtn.returnType match {
            case TypeReliefDraft =>
              controllers.routes.PeriodSummaryController
                .viewReturn(currentTaxYear).toString
            case TypeLiabilityDraft =>
              controllers.routes.PeriodSummaryController
                .viewChargeable(currentTaxYear, rtn.id).toString
            case TypeChangeLiabilityDraft =>
              controllers.routes.PeriodSummaryController
                .viewChargeableEdit(currentTaxYear, rtn.id).toString
            case TypeDisposeLiabilityDraft =>
              controllers.routes.PeriodSummaryController
                .viewDisposal(currentTaxYear, rtn.id).toString
          }
        )
      ))
    }

    val currentLiabilityReturns: Seq[AccountSummaryRowModel] = submittedReturns.flatMap(_.currentLiabilityReturns.map(
        rtn => AccountSummaryRowModel(
          description = rtn.description,
          formBundleNo = Some(rtn.formBundleNo),
          returnType = submittedType,
          route = controllers.routes.FormBundleReturnController.view(
            rtn.formBundleNo, currentTaxYear
          ).toString
        )
      )
    )

    val reliefReturns: Seq[AccountSummaryRowModel] = submittedReturns.flatMap(_.reliefReturns.map(
      rtn => AccountSummaryRowModel(
        description = rtn.reliefType,
        formBundleNo = Some(rtn.formBundleNo),
        returnType = submittedType,
        route = controllers.reliefs.routes.ViewReliefReturnController.viewReliefReturn(
          currentTaxYear, rtn.formBundleNo).toString)
      )
    )

    val hasPreviousReturns: Boolean = submittedReturns.flatMap(_.oldLiabilityReturns).nonEmpty

    val allAccountSummaryCurrentReturns = draftReturns ++ currentLiabilityReturns ++ reliefReturns

    Future(allAccountSummaryCurrentReturns.take(5), allAccountSummaryCurrentReturns.size, hasPreviousReturns)
  }

}
