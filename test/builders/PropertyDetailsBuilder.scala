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

package builders

import builders.ChangeLiabilityReturnBuilder.generateFormBundlePropertyDetails
import config.ApplicationConfig
import models._
import org.joda.time.LocalDate
import utils.AtedConstants
import utils.PeriodUtils._

object PropertyDetailsBuilder {

  val formBundleProp = FormBundleProperty(BigDecimal(100), new LocalDate("2015-09-08"), new LocalDate("2015-10-12"), AtedConstants.LiabilityReturnType, None)

  def generateFormBundleReturn: FormBundleReturn = {
    FormBundleReturn("2015", generateFormBundlePropertyDetails, dateOfAcquisition = None, valueAtAcquisition = None, taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false, dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq(formBundleProp))
  }


  def getPropertyDetailsValueRevalued(periodKey: Int): Option[PropertyDetailsValue] = {
    Some(PropertyDetailsValue(anAcquisition = Some(true),
      isPropertyRevalued = Some(true),
      revaluedValue = Some(BigDecimal(1500000)),
      revaluedDate = Some(new LocalDate(s"$periodKey-04-01"))
    ))
  }

  def getPropertyDetailsValueRevaluedByAgent(periodKey: Int): Option[PropertyDetailsValue] = {
    Some(PropertyDetailsValue(anAcquisition = Some(true),
      isPropertyRevalued = Some(true),
      revaluedValue = Some(BigDecimal(1500000)),
      revaluedDate = Some(new LocalDate(s"$periodKey-04-01")),
      isValuedByAgent =  Some(true)
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

  def getPropertyDetails(id: String,
                         postCode: Option[String] = None,
                         liabilityAmount: Option[BigDecimal] = None)(implicit appConfig: ApplicationConfig): PropertyDetails = {
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
                         liabilityAmount: Option[BigDecimal] = None)(implicit appConfig: ApplicationConfig): PropertyDetails = {
    val periodKey: Int = calculatePeakStartYear()

    PropertyDetails(
      id,
      periodKey,
      getPropertyDetailsAddress(postCode),
      getPropertyDetailsTitle,
      getPropertyDetailsValueRevaluedByAgent(periodKey),
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
