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

import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class DraftReturns(periodKey: Int, // periodKey so that we know which draft belongs to which period
                        id: String,
                        description: String,
                        charge: Option[BigDecimal] = None,
                        returnType: String // can be - relief, chargeable, dispose, move-to-relief,change
                       )


object DraftReturns {
  implicit val formats = Json.format[DraftReturns]
}

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
                               allReturns: Seq[PeriodSummaryReturns] = Nil)

object SummaryReturnsModel {
  implicit val formats = Json.format[SummaryReturnsModel]
}
