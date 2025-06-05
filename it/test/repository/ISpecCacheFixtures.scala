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

package repository

import models.{CachedData, PeriodSummaryReturns, SubmittedLiabilityReturns, SubmittedReturns, SubscriptionData, SummaryReturnsModel}

import java.time.LocalDate

trait ISpecCacheFixtures {

  val periodKey: Int        = 2015
  val formBundleNo2: String = "123456789013"

  val submittedLiabilityReturns1: SubmittedLiabilityReturns =
    SubmittedLiabilityReturns(
      formBundleNo2,
      "addr1+2",
      BigDecimal(1234.00),
      LocalDate.parse("2015-05-05"),
      LocalDate.parse("2015-05-05"),
      LocalDate.parse("2015-05-05"),
      changeAllowed = true,
      "payment-ref-01"
    )

  val submittedReturns: SubmittedReturns         = SubmittedReturns(periodKey, Seq(), Seq(submittedLiabilityReturns1))
  val periodSummaryReturns: PeriodSummaryReturns = PeriodSummaryReturns(periodKey, Seq(), Some(submittedReturns))
  val summaryReturnsModel: SummaryReturnsModel   = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))

  val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
  val successData: CachedData           = CachedData(successResponse)

  val delegatedClientAtedRefNumber: String = "XN1200000100001"
}
