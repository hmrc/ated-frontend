/*
 * Copyright 2021 HM Revenue & Customs
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

package builders

import models._
import org.joda.time.LocalDate

object DisposeLiabilityReturnBuilder {

  def generateDisposalDate(periodKey: Int) = DisposeLiability(dateOfDisposal = Some(new LocalDate("2015-04-02")), periodKey = periodKey)

  def generateFormBundleAddress = FormBundleAddress("line1", "line2", None, None, None, "GB")

  def generateDisposeLiabilityReturn(oldFormBundleNum: String): DisposeLiabilityReturn = {
    val fAddress = generateFormBundleAddress
    val fProperty = FormBundlePropertyDetails(None, fAddress, None)
    val dispDate = generateDisposalDate(2015)
    val fReturn = FormBundleReturn("2015", fProperty, dateOfAcquisition = None, valueAtAcquisition = None, taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false,
      dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq())

    DisposeLiabilityReturn(id = oldFormBundleNum, fReturn, Some(dispDate), None, None)
  }


  def generateCalculated = DisposeCalculated(liabilityAmount = BigDecimal(2500.00), amountDueOrRefund = -500.00)

}
