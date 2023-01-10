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

import builders.ChangeLiabilityReturnBuilder.generateFormBundlePropertyDetails
import config.ApplicationConfig
import models._
import org.joda.time.LocalDate
import utils.AtedConstants
import utils.PeriodUtils._

object PropertyDetailsBuilder {

  val formBundleProp: FormBundleProperty = FormBundleProperty(BigDecimal(100), new LocalDate("2015-09-08"), new LocalDate("2015-10-12"), AtedConstants.LiabilityReturnType, None)
  val formBundlePropRefund: FormBundleProperty = FormBundleProperty(BigDecimal(727000), new LocalDate("2015-04-01"), new LocalDate("2016-01-01"), AtedConstants.ReliefReturnType, Some("Relief"))

  def generateFormBundleReturn: FormBundleReturn = {
    FormBundleReturn("2015", generateFormBundlePropertyDetails, dateOfAcquisition = None, valueAtAcquisition = None, taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false, dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq(formBundleProp))
  }

  def generateFormBundleReturnRefund: FormBundleReturn = {
    FormBundleReturn("2015", generateFormBundlePropertyDetails, dateOfAcquisition = Some(new LocalDate ("2011-05-26")), valueAtAcquisition = Some(727000.00), taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = true, dateOfSubmission = new LocalDate("2016-05-10"), liabilityAmount = BigDecimal(9375.12), paymentReference = "payment-ref-123", lineItem = Seq(formBundlePropRefund))
  }

  def getPropertyDetailsValueRevalued(periodKey: Int): Option[PropertyDetailsValue] = {
    Some(PropertyDetailsValue(anAcquisition = Some(true),
      isPropertyRevalued = Some(true),
      revaluedValue = Some(BigDecimal(1500000)),
      revaluedDate = Some(new LocalDate(s"$periodKey-04-01"))
    ))
  }

  def getPropertyDetailsValueRevaluedByAgent(periodKey: Int, valueChanged: Option[Boolean] = Some(true)): Option[PropertyDetailsValue] = {
    Some(PropertyDetailsValue(anAcquisition = Some(true),
      isPropertyRevalued = Some(true),
      revaluedValue = Some(BigDecimal(1500000)),
      revaluedDate = Some(new LocalDate(s"$periodKey-04-01")),
      isValuedByAgent =  Some(true),
      hasValueChanged = valueChanged
    ))
  }

  def getPropertyDetailsNewBuildDates: Option[PropertyDetailsNewBuildDates] = {
    Some(PropertyDetailsNewBuildDates(
      Some(new LocalDate("2010-01-01")),
      Some(new LocalDate("2010-02-01")))
    )
  }

  def getPropertyDetailsWhenAcquired: Option[PropertyDetailsWhenAcquiredDates] = {
    Some(PropertyDetailsWhenAcquiredDates(
      Some(new LocalDate("2010-01-01")))
    )
  }

  def getPropertyDetailsNewBuildValue: Option[PropertyDetailsNewBuildValue] = {
    Some(PropertyDetailsNewBuildValue(Some(BigDecimal(10000000.00))))
  }

  def getPropertyDetailsValueOnAcquisition: Option[PropertyDetailsValueOnAcquisition] = {
    Some(PropertyDetailsValueOnAcquisition(Some(BigDecimal(10000000.00))))
  }

  def getPropertyDetailsValueFull: Option[PropertyDetailsValue] = {
    Some(new PropertyDetailsValue(
      anAcquisition = Some(true),
      isPropertyRevalued = Some(true),
      revaluedValue = Some(BigDecimal(1111.11)),
      revaluedDate = Some(new LocalDate("1970-01-01")),
      isOwnedBeforePolicyYear = Some(true),
      ownedBeforePolicyYearValue = Some(BigDecimal(1111.11)),
      isNewBuild =  Some(true),
      newBuildValue = Some(BigDecimal(1111.11)),
      newBuildDate = Some(new LocalDate("1970-01-01")),
      notNewBuildValue = Some(BigDecimal(1111.11)),
      notNewBuildDate = Some(new LocalDate("1970-01-01")),
      isValuedByAgent =  Some(true),
      valuationDate = Some(new LocalDate("1970-01-01"))
    ))
  }

  def getPropertyDetailsValueWithRefund: Option[PropertyDetailsValue] = {
    Some(new PropertyDetailsValue(
      anAcquisition = Some(false),
      isOwnedBeforePolicyYear = Some(true),
      ownedBeforePolicyYearValue = Some(BigDecimal(2000000.00)),
      isValuedByAgent =  Some(false),
      hasValueChanged = Some(true)
    ))
  }

  def getPropertyDetailsPeriod: Option[PropertyDetailsPeriod] = {
    Some(new PropertyDetailsPeriod(
      isFullPeriod = Some(false),
      isTaxAvoidance =  Some(false),
      isInRelief =  Some(false)
    ))
  }

  def getPropertyDetailsPeriodDatesLiable(startDate : LocalDate, endDate: LocalDate): Option[PropertyDetailsPeriod] = {
    val liabilityPeriods = List(LineItem("Liability",startDate, endDate))
    Some(new PropertyDetailsPeriod(
      isFullPeriod = Some(false),
      liabilityPeriods = liabilityPeriods,
      reliefPeriods = Nil,
      isTaxAvoidance =  Some(true),
      taxAvoidanceScheme =  Some("taxAvoidanceScheme"),
      supportingInfo = Some("supportingInfo"),
      isInRelief =  Some(true)
    ))
  }

  def getPropertyDetailsPeriodFull(periodKey : Int = 2015): Option[PropertyDetailsPeriod] = {
    val liabilityPeriods = List(LineItem("Liability",new LocalDate(s"$periodKey-4-1"), new LocalDate(s"$periodKey-8-31")))
    val reliefPeriods = List(LineItem("Relief",new LocalDate(s"$periodKey-9-1"), new LocalDate(s"${periodKey+1}-3-31"), Some("Property rental businesses")))
    Some(new PropertyDetailsPeriod(
      isFullPeriod = Some(false),
      liabilityPeriods = liabilityPeriods,
      reliefPeriods = reliefPeriods,
      isTaxAvoidance =  Some(true),
      taxAvoidanceScheme =  Some("taxAvoidanceScheme"),
      taxAvoidancePromoterReference = Some("taxAvoidancePromoterReference"),
      supportingInfo = Some("supportingInfo"),
      isInRelief =  Some(true)
    ))
  }

  def getPropertyDetailsPeriodRefund(periodKey : Int = 2015): Option[PropertyDetailsPeriod] = {
    val liabilityPeriods = List(LineItem("Liability",new LocalDate(s"$periodKey-4-1"), new LocalDate(s"${periodKey+1}-3-31")))
    Some(new PropertyDetailsPeriod(
      isFullPeriod = Some(false),
      isTaxAvoidance =  Some(true),
      taxAvoidanceScheme =  Some("taxAvoidanceScheme"),
      taxAvoidancePromoterReference = Some("taxAvoidancePromoterReference"),
      supportingInfo = Some("supportingInfo"),
      isInRelief =  Some(false),
      liabilityPeriods = liabilityPeriods,
    ))
  }

  def getPropertyDetailsTitle: Option[PropertyDetailsTitle] = {
    Some(new PropertyDetailsTitle("titleNo"))
  }

  def getPropertyDetailsAddress(postCode: Option[String] = None): PropertyDetailsAddress = {
    new PropertyDetailsAddress("addr1", "addr2", Some("addr3"), Some("addr4"), postCode)
  }

  def getPropertyDetailsCalculated(liabilityAmount: Option[BigDecimal] = None, periodKey : Int = 2015): Option[PropertyDetailsCalculated] = {
    val liabilityPeriods = List(CalculatedPeriod(BigDecimal(1111.11), new LocalDate(s"$periodKey-4-1"), new LocalDate(s"$periodKey-8-31"), "Liability"))
    val reliefPeriods = List(CalculatedPeriod(BigDecimal(1111.11),new LocalDate(s"$periodKey-9-1"), new LocalDate(s"${periodKey+1}-3-31"), "Relief", Some("Property rental businesses")))
    Some(new PropertyDetailsCalculated(liabilityAmount = liabilityAmount,
      liabilityPeriods = liabilityPeriods,
      reliefPeriods = reliefPeriods,
      professionalValuation = Some(true),
      acquistionDateToUse = Some(new LocalDate("1970-01-01"))
    ))
  }

  def getPropertyDetailsCalculatedRefund(liabilityAmount: Option[BigDecimal] = Some(8875.12), periodKey : Int = 2015): Option[PropertyDetailsCalculated] = {
    val liabilityPeriods = List(CalculatedPeriod(BigDecimal(2000000.00), new LocalDate(s"$periodKey-4-1"), new LocalDate(s"${periodKey+1}-8-31"), "Liability"))
    Some(new PropertyDetailsCalculated(
      acquistionValueToUse = Some(2000000.00),
      acquistionDateToUse = Some(new LocalDate("2012-04-01")),
      professionalValuation = Some(false),
      liabilityPeriods = liabilityPeriods,
      liabilityAmount = liabilityAmount,
      amountDueOrRefund = Some(-500)
    ))
  }

  def getPropertyDetails(id: String,
                         postCode: Option[String] = None,
                         liabilityAmount: Option[BigDecimal] = None): PropertyDetails = {
    val periodKey: Int = 2019
    PropertyDetails(
      id,
      periodKey,
      getPropertyDetailsAddress(postCode),
      getPropertyDetailsTitle,
      getPropertyDetailsValueRevalued(periodKey),
      getPropertyDetailsPeriodFull(periodKey),
      getPropertyDetailsCalculated(liabilityAmount))
  }

  def getPropertyDetailsValuedByAgent(id: String,
                         postCode: Option[String] = None,
                         liabilityAmount: Option[BigDecimal] = None,
                         valueChanged: Option[Boolean] = Some(true))(implicit appConfig: ApplicationConfig): PropertyDetails = {
    val periodKey: Int = calculatePeakStartYear()

    PropertyDetails(
      id,
      periodKey,
      getPropertyDetailsAddress(postCode),
      getPropertyDetailsTitle,
      getPropertyDetailsValueRevaluedByAgent(periodKey, valueChanged),
      getPropertyDetailsPeriod,
      getPropertyDetailsCalculated(liabilityAmount))
  }

  def getPropertyDetailsWithNoValue(id: String,
                         postCode: Option[String] = None,
                         liabilityAmount: Option[BigDecimal] = None)(implicit appConfig: ApplicationConfig): PropertyDetails = {
    val periodKey: Int = calculatePeakStartYear()

    PropertyDetails(
      id,
      periodKey,
      getPropertyDetailsAddress(postCode),
      getPropertyDetailsTitle,
      value = None,
      getPropertyDetailsPeriod,
      getPropertyDetailsCalculated(liabilityAmount))
  }



  def getFullPropertyDetails(id: String,
                             postCode: Option[String] = None,
                             liabilityAmount: Option[BigDecimal] = None
                            )(implicit appConfig: ApplicationConfig): PropertyDetails = {
    val periodKey: Int = calculatePeakStartYear()

    PropertyDetails(
      id,
      periodKey,
      getPropertyDetailsAddress(postCode),
      getPropertyDetailsTitle,
      getPropertyDetailsValueFull,
      getPropertyDetailsPeriodFull(periodKey),
      getPropertyDetailsCalculated(liabilityAmount)
    )
  }

  val bankDetailsHasNoBankAccount: Option[BankDetailsModel] = Some(BankDetailsModel(hasBankDetails = false))
  val bankDetailsYesButNoDetails: Option[BankDetailsModel] = Some(BankDetailsModel(hasBankDetails = true, bankDetails = None))
  val completedBankDetails: Option[BankDetailsModel] = Some(BankDetailsModel(hasBankDetails = true,
    bankDetails = Some(BankDetails(hasUKBankAccount = Some(true),
      accountName = Some("Account name"),
      accountNumber = Some("12312312"),
      sortCode = Some(SortCode("12","12","12")))))
  )
  val completedBankDetailsOverseas: Option[BankDetailsModel] = Some(BankDetailsModel(hasBankDetails = true,
    bankDetails = Some(BankDetails(hasUKBankAccount = Some(false),
      accountName = Some("Overseas account name"),
      iban = Some(Iban("111222333444555")),
      bicSwiftCode = Some(BicSwiftCode("12345678999")))))
  )

  def getBankDetails(hasBankDetails: Boolean, bankDetailsType: Option[String] = None): Option[BankDetailsModel] ={
    if(!hasBankDetails){
     bankDetailsHasNoBankAccount
    } else if(hasBankDetails && bankDetailsType.isEmpty){
      bankDetailsYesButNoDetails
    }else if(hasBankDetails && bankDetailsType.contains("UK")){
      completedBankDetails
    }else if(hasBankDetails && bankDetailsType.contains("NonUK")){
      completedBankDetailsOverseas
    }else {
      None
    }
  }

  def getFullPropertyDetailsWithRefund(id: String,
                             postCode: Option[String] = None,
                             liabilityAmount: Option[BigDecimal] = None,
                             hasBankDetails: Boolean,
                             bankDetailsType: Option[String] = None
                            ): PropertyDetails = {
    val periodKey: Int = 2015

    PropertyDetails(
      id,
      periodKey,
      getPropertyDetailsAddress(postCode),
      getPropertyDetailsTitle,
      getPropertyDetailsValueWithRefund,
      getPropertyDetailsPeriodRefund(periodKey),
      getPropertyDetailsCalculatedRefund(liabilityAmount),
      Option(generateFormBundleReturn),
      getBankDetails(hasBankDetails, bankDetailsType)
    )
  }

  def getPropertyDetailsWithFormBundleReturn(id: String,
                             postCode: Option[String] = None,
                             liabilityAmount: Option[BigDecimal] = None
                            )(implicit appConfig: ApplicationConfig): PropertyDetails = {
    val periodKey: Int = calculatePeakStartYear()

    PropertyDetails(
      id,
      periodKey,
      getPropertyDetailsAddress(postCode),
      getPropertyDetailsTitle,
      getPropertyDetailsValueFull,
      getPropertyDetailsPeriodFull(periodKey),
      getPropertyDetailsCalculated(liabilityAmount),
      Option(generateFormBundleReturn)
    )
  }

}
