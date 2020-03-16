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

package models

import config.ApplicationConfig
import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.json.Reads._
import utils.PeriodUtils
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads.DefaultJodaLocalDateReads
import play.shaded.ahc.io.netty.handler.ssl.ApplicationProtocolConfig

case class DraftReturns(periodKey: Int, // periodKey so that we know which draft belongs to which period
                        id: String,
                        description: String,
                        charge: Option[BigDecimal] = None,
                        returnType: String // can be - relief, chargeable, dispose, move-to-relief,change
                       )


object DraftReturns {
  implicit val formats = Json.format[DraftReturns]
}

case class AccountSummaryRowModel(
                                 formBundleNo: Option[String] = None,
                                 returnType: String,
                                 description: String,
                                 route: String
                                 )

case class SubmittedReliefReturns(formBundleNo: String,
                                  reliefType: String,
                                  dateFrom: LocalDate,
                                  dateTo: LocalDate,
                                  dateOfSubmission: LocalDate,
                                  avoidanceSchemeNumber: Option[String] = None,
                                  promoterReferenceNumber: Option[String] = None)

object SubmittedReliefReturns {
  implicit val formats = Json.format[SubmittedReliefReturns]
}

case class SubmittedLiabilityReturns(formBundleNo: String,
                                     description: String,
                                     liabilityAmount: BigDecimal,
                                     dateFrom: LocalDate,
                                     dateTo: LocalDate,
                                     dateOfSubmission: LocalDate,
                                     changeAllowed: Boolean,
                                     paymentReference: String)

object SubmittedLiabilityReturns {
  implicit val formats = Json.format[SubmittedLiabilityReturns]
}

case class SubmittedReturns(periodKey: Int, // periodKey so that we don't create any model in ated-fe.this model is cached there as a Seq
                            reliefReturns: Seq[SubmittedReliefReturns] = Nil,
                            currentLiabilityReturns: Seq[SubmittedLiabilityReturns] = Nil,
                            oldLiabilityReturns: Seq[SubmittedLiabilityReturns] = Nil)

object SubmittedReturns {
  implicit val formats = Json.format[SubmittedReturns]
}

case class PeriodSummaryReturns(periodKey: Int, // this is used for any other purpose
                                draftReturns: Seq[DraftReturns] = Nil,
                                submittedReturns: Option[SubmittedReturns] = None)

object PeriodSummaryReturns {
  implicit val formats = Json.format[PeriodSummaryReturns]
}

case class SummaryReturnsModel(atedBalance: Option[BigDecimal] = None,
                               returnsCurrentTaxYear: Seq[PeriodSummaryReturns] = Nil,
                               returnsOtherTaxYears: Seq[PeriodSummaryReturns] = Nil
                              )

object SummaryReturnsModel {
  implicit def reads(implicit applicationConfig: ApplicationConfig): Reads[SummaryReturnsModel] = new Reads[SummaryReturnsModel] {

    def reads(json: JsValue): JsResult[SummaryReturnsModel] = {

      val currentTaxYear = PeriodUtils.calculatePeakStartYear(LocalDate.now())
      val atedBalance: Option[BigDecimal] = (json \ "atedBalance").asOpt[BigDecimal]
      val allReturns: Seq[PeriodSummaryReturns] = (json \ "allReturns").as[Seq[PeriodSummaryReturns]]

      val returnsCurrentTaxYear: Seq[PeriodSummaryReturns] = allReturns.filter(
        _.periodKey == currentTaxYear
      )

      val returnsOtherTaxYears: Seq[PeriodSummaryReturns] = allReturns.filterNot(
        _.periodKey == currentTaxYear
      )

      JsSuccess(
        SummaryReturnsModel(atedBalance, returnsCurrentTaxYear, returnsOtherTaxYears)
      )
    }
  }

  implicit val writes: Writes[SummaryReturnsModel] = Writes { returnsModel =>

    val allReturns = returnsModel.returnsCurrentTaxYear ++ returnsModel.returnsOtherTaxYears

    Json.obj(
      "atedBalance" -> returnsModel.atedBalance,
      "allReturns" -> allReturns
      )
  }

  implicit def summaryReturnsModelFormat(implicit applicationConfig: ApplicationConfig): Format[SummaryReturnsModel] =
    Format(reads, writes)

}
