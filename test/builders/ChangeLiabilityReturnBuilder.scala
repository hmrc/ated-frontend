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

package builders

import models._
import org.joda.time.{DateTime, LocalDate}

object ChangeLiabilityReturnBuilder {

  def generateFormBundleAddress = FormBundleAddress("line1", "line2", None, None, None, "GB")

  def generateFormBundlePropertyDetails = FormBundlePropertyDetails(None, generateFormBundleAddress, None)

  def generateFormBundleReturn = {
    FormBundleReturn("2015", generateFormBundlePropertyDetails, dateOfAcquisition = None, valueAtAcquisition = None, taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false, dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq())
  }

  def generatePropertyDetailsAddress = PropertyDetailsAddress("line1", "line2", None, None, None)

  def generatePropertyDetailsTitle = Some(PropertyDetailsTitle("titleNo"))

  def generatePropertyDetailsCalculated(amt: BigDecimal) = PropertyDetailsCalculated(amountDueOrRefund = Some(amt))

  def generateChangeLiabilityReturn(formBundleNo: String): PropertyDetails = {
    val fAddress = generateFormBundleAddress
    val fProperty = FormBundlePropertyDetails(None, fAddress, None)
    val fReturn = FormBundleReturn("2015", fProperty, dateOfAcquisition = None, valueAtAcquisition = None, taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false,
      dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq())
    val propAddr = generatePropertyDetailsAddress
    val propDetCalculated = generatePropertyDetailsCalculated(112300)
    PropertyDetails(id = formBundleNo,
      periodKey = 2015,
      formBundleReturn = Some(fReturn),
      addressProperty = propAddr,
      title = generatePropertyDetailsTitle,
      calculated = Some(propDetCalculated))
  }

  def generateChangeLiabilityReturnForCalculatedIsNone(formBundleNo: String): PropertyDetails = {
    val fAddress = generateFormBundleAddress
    val fProperty = FormBundlePropertyDetails(None, fAddress, None)
    val fReturn = FormBundleReturn("2015", fProperty, dateOfAcquisition = None, valueAtAcquisition = None, taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false,
      dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq())
    val propAddr = generatePropertyDetailsAddress
    PropertyDetails(id = formBundleNo,
      periodKey = 2015,
      formBundleReturn = Some(fReturn),
      addressProperty = propAddr,
      title = generatePropertyDetailsTitle,
      calculated = None)
  }

  def generateChangeLiabilityReturnForCalculatedIsLessThanZero(formBundleNo: String): PropertyDetails = {
    val fAddress = generateFormBundleAddress
    val fProperty = FormBundlePropertyDetails(None, fAddress, None)
    val fReturn = FormBundleReturn("2015", fProperty, dateOfAcquisition = None, valueAtAcquisition = None, taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false,
      dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq())
    val propAddr = generatePropertyDetailsAddress
    val propDetCalculated = generatePropertyDetailsCalculated(-112300)
    PropertyDetails(id = formBundleNo,
      periodKey = 2015,
      formBundleReturn = Some(fReturn),
      addressProperty = propAddr,
      title = generatePropertyDetailsTitle,
      calculated = Some(propDetCalculated))
  }

  def generateChangeLiabilityReturnForCalculatedEqualsZero(formBundleNo: String): PropertyDetails = {
    val fAddress = generateFormBundleAddress
    val fProperty = FormBundlePropertyDetails(None, fAddress, None)
    val fReturn = FormBundleReturn("2015", fProperty, dateOfAcquisition = None, valueAtAcquisition = None, taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false,
      dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq())
    val propAddr = generatePropertyDetailsAddress
    val propDetCalculated = generatePropertyDetailsCalculated(0)
    PropertyDetails(id = formBundleNo,
      periodKey = 2015,
      formBundleReturn = Some(fReturn),
      addressProperty = propAddr,
      title = generatePropertyDetailsTitle,
      calculated = Some(propDetCalculated))
  }


  def generateCalculated = {
    val liabilityPeriods = List(CalculatedPeriod(BigDecimal(2500000), new LocalDate("2015-4-1"), new LocalDate("2016-3-31"), "Liability"))
    val reliefPeriods = Nil
    PropertyDetailsCalculated(liabilityPeriods = liabilityPeriods, reliefPeriods = reliefPeriods,
      acquistionDateToUse = Some(new LocalDate("2015-5-15")), acquistionValueToUse = None, professionalValuation = Some(true),
      liabilityAmount = Some(2500),
      amountDueOrRefund = Some(BigDecimal(-500.00)))
  }
  def generateEditLiabilityResponse(oldFormBundle: String) = {
    val liability = EditLiabilityReturnsResponse("Post", oldFormBundleNumber = oldFormBundle, formBundleNumber = Some("112233445566"), liabilityAmount = BigDecimal(3500.00), amountDueOrRefund = BigDecimal(-500.00), paymentReference = Some("pay-ref-123"))
    EditLiabilityReturnsResponseModel(processingDate = new DateTime("2016-04-20T12:41:41.839+01:00"), liabilityReturnResponse = Seq(liability), BigDecimal(1200.00))
  }

}
