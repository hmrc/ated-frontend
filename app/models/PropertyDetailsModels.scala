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

package models

import config.ApplicationConfig

import java.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._

sealed trait PeriodValidity

case class PeriodInvalid(inputDateType: String) extends PeriodValidity

case object PeriodValid extends PeriodValidity

case class PropertyDetailsAddress(line_1: String, line_2: String, line_3: Option[String], line_4: Option[String],
                                  postcode: String) {
  override def toString: String = {

    val line3display = line_3.map(line3 => s", $line3, " ).fold("")(x=>x)
    val line4display = line_4.map(line4 => s"$line4, " ).fold("")(x=>x)
    s"$line_1, $line_2 $line3display$line4display$postcode"
  }
}

object PropertyDetailsAddress {
  given formats: OFormat[PropertyDetailsAddress] = Json.format[PropertyDetailsAddress]
}

case class PropertyDetailsTitle(titleNumber: String)

object PropertyDetailsTitle {
  given formats: OFormat[PropertyDetailsTitle] = Json.format[PropertyDetailsTitle]
}

case class PropertyDetailsValue( anAcquisition: Option[Boolean] = None,
                                 isPropertyRevalued: Option[Boolean] = None,
                                 revaluedValue: Option[BigDecimal] = None,
                                 revaluedDate: Option[LocalDate] = None,
                                 partAcqDispDate: Option[LocalDate] = None,
                                 isOwnedBeforePolicyYear: Option[Boolean] = None,
                                 ownedBeforePolicyYearValue: Option[BigDecimal] = None,
                                 isNewBuild: Option[Boolean] = None,
                                 newBuildValue: Option[BigDecimal] = None,
                                 isBuildDateKnown: Option[Boolean] = None,
                                 newBuildDate: Option[LocalDate] = None,
                                 isLocalAuthRegDateKnown: Option[Boolean] = None,
                                 localAuthRegDate: Option[LocalDate] = None,
                                 notNewBuildValue: Option[BigDecimal] = None,
                                 notNewBuildDate: Option[LocalDate] = None,
                                 isValuedByAgent: Option[Boolean] = None,
                                 valuationDate: Option[LocalDate] = None,
                                 hasValueChanged: Option[Boolean] = None
                               )

object PropertyDetailsValue {
  given formats: OFormat[PropertyDetailsValue] = Json.format[PropertyDetailsValue]
}

case class PropertyDetailsAcquisition(anAcquisition: Option[Boolean] = None)

object PropertyDetailsAcquisition {
  given formats: OFormat[PropertyDetailsAcquisition] = Json.format[PropertyDetailsAcquisition]
}

case class HasValueChanged(hasValueChanged: Option[Boolean] = None)

object HasValueChanged {
  given formats: OFormat[HasValueChanged] = Json.format[HasValueChanged]
}

case class HasBeenRevalued(isPropertyRevalued: Option[Boolean])

object HasBeenRevalued {
  given formats: OFormat[HasBeenRevalued] = Json.format[HasBeenRevalued]
}
case class PropertyDetailsRevalued(isPropertyRevalued: Option[Boolean] = None,
                                   revaluedValue: Option[BigDecimal] = None,
                                   revaluedDate: Option[LocalDate] = None,
                                   partAcqDispDate: Option[LocalDate] = None)

object PropertyDetailsRevalued {
  given formats: OFormat[PropertyDetailsRevalued] = Json.format[PropertyDetailsRevalued]
}

case class PropertyDetailsNewValuation(revaluedValue: Option[BigDecimal] = None)

object  PropertyDetailsNewValuation {
  given formats: OFormat[PropertyDetailsNewValuation] = Json.format[PropertyDetailsNewValuation]
}

case class DateOfChange(dateOfChange: Option[LocalDate])

object DateOfChange {
  given formats: OFormat[DateOfChange] = Json.format[DateOfChange]
}

case class DateOfRevalue(dateOfRevalue: Option[LocalDate])

object DateOfRevalue {
  given formats: OFormat[DateOfRevalue] = Json.format[DateOfRevalue]
}

sealed trait OwnedBeforePolicyYear

case object IsOwnedBefore2012 extends OwnedBeforePolicyYear

case object IsOwnedBefore2017 extends OwnedBeforePolicyYear

case object IsOwnedBefore2022 extends OwnedBeforePolicyYear

case object NotOwnedBeforePolicyYear extends OwnedBeforePolicyYear

case class PropertyDetailsOwnedBefore(isOwnedBeforePolicyYear: Option[Boolean] = None,
                                      ownedBeforePolicyYearValue: Option[BigDecimal] = None) {
  def policyYear(periodKey: Int)(using appConf: ApplicationConfig) : OwnedBeforePolicyYear = {
    val valuation2022Active: Boolean = appConf.val2022Date

    isOwnedBeforePolicyYear match {
      case Some(true) => periodKey match {
        case p if valuation2022Active && p >= 2023 => IsOwnedBefore2022
        case p if p >= 2018 && (!valuation2022Active || p < 2023) => IsOwnedBefore2017
        case p if p >= 2013 && p < 2018 => IsOwnedBefore2012
        case _ => throw new RuntimeException("Invalid liability period")
      }
      case _ => NotOwnedBeforePolicyYear
    }
  }
}

object PropertyDetailsOwnedBefore {

  given propertyDetailsOwnedBeforeReads: Reads[PropertyDetailsOwnedBefore] = (
    (JsPath \ "isOwnedBeforePolicyYear").read[Boolean].map(Option(_)).orElse((JsPath \ "isOwnedBefore2012").readNullable[Boolean]) and
      (JsPath \ "ownedBeforePolicyYearValue").read[BigDecimal].map(Option(_)).orElse((JsPath \ "ownedBefore2012Value").readNullable[BigDecimal])
    )(PropertyDetailsOwnedBefore.apply _)

  given propertyDetailsOwnedBeforeWrites: OWrites[PropertyDetailsOwnedBefore]=Json.writes[PropertyDetailsOwnedBefore]
}

case class PropertyDetailsProfessionallyValued(isValuedByAgent: Option[Boolean] = None)

object PropertyDetailsProfessionallyValued {
  given formats: OFormat[PropertyDetailsProfessionallyValued] = Json.format[PropertyDetailsProfessionallyValued]
}

case class PropertyDetailsNewBuild(isNewBuild: Option[Boolean] = None)

object PropertyDetailsNewBuild {
  given formats: OFormat[PropertyDetailsNewBuild] = Json.format[PropertyDetailsNewBuild]
}

case class DateFirstOccupiedKnown(isDateFirstOccupiedKnown: Option[Boolean] = None)

object DateFirstOccupiedKnown {
  given formats: OFormat[DateFirstOccupiedKnown] = Json.format[DateFirstOccupiedKnown]
}

case class DateCouncilRegisteredKnown(isDateCouncilRegisteredKnown: Option[Boolean] = None)

object DateCouncilRegisteredKnown {
  given formats: OFormat[DateCouncilRegisteredKnown] = Json.format[DateCouncilRegisteredKnown]
}

case class DateFirstOccupied(dateFirstOccupied: Option[LocalDate])

object DateFirstOccupied {
  given formats: Format[DateFirstOccupied] = Json.format[DateFirstOccupied]
}

case class DateCouncilRegistered(dateCouncilRegistered: Option[LocalDate])

object DateCouncilRegistered {
  given formats: Format[DateCouncilRegistered] = Json.format[DateCouncilRegistered]
}

case class PropertyDetailsNewBuildDates(newBuildOccupyDate: Option[LocalDate],
                                        newBuildRegisterDate: Option[LocalDate])

object PropertyDetailsNewBuildDates {
  given formats: Format[PropertyDetailsNewBuildDates] = Json.format[PropertyDetailsNewBuildDates]
}

case class PropertyDetailsWhenAcquiredDates(acquiredDate: Option[LocalDate])

object PropertyDetailsWhenAcquiredDates{
  given formats: Format[PropertyDetailsWhenAcquiredDates] = Json.format[PropertyDetailsWhenAcquiredDates]
}

case class PropertyDetailsNewBuildValue(newBuildValue: Option[BigDecimal])

object PropertyDetailsNewBuildValue {
  given formats: Format[PropertyDetailsNewBuildValue] = Json.format[PropertyDetailsNewBuildValue]
}

case class PropertyDetailsValueOnAcquisition(acquiredValue: Option[BigDecimal])

object PropertyDetailsValueOnAcquisition {
  given formats: Format[PropertyDetailsValueOnAcquisition] = Json.format[PropertyDetailsValueOnAcquisition]
}

case class PropertyDetailsFullTaxPeriod(isFullPeriod: Option[Boolean] = None)

object PropertyDetailsFullTaxPeriod {
  given formats: OFormat[PropertyDetailsFullTaxPeriod] = Json.format[PropertyDetailsFullTaxPeriod]
}

case class PropertyDetailsDatesLiable(startDate: Option[LocalDate],
                                      endDate: Option[LocalDate])

object PropertyDetailsDatesLiable {
  given formats: OFormat[PropertyDetailsDatesLiable] = Json.format[PropertyDetailsDatesLiable]
}

case class IsFullTaxPeriod(isFullPeriod: Boolean, datesLiable: Option[PropertyDetailsDatesLiable])

object IsFullTaxPeriod {
  given formats: OFormat[IsFullTaxPeriod] = Json.format[IsFullTaxPeriod]
}


case class PeriodChooseRelief(reliefDescription: String)

object PeriodChooseRelief {
  given formats: OFormat[PeriodChooseRelief] = Json.format[PeriodChooseRelief]
}


case class PropertyDetailsDatesInRelief(startDate: Option[LocalDate],
                                        endDate: Option[LocalDate],
                                        description: Option[String] = None)

object PropertyDetailsDatesInRelief {
  given formats: OFormat[PropertyDetailsDatesInRelief] = Json.format[PropertyDetailsDatesInRelief]
}


case class PropertyDetailsInRelief(isInRelief: Option[Boolean] = None)


object PropertyDetailsInRelief {
  given formats: OFormat[PropertyDetailsInRelief] = Json.format[PropertyDetailsInRelief]
}

case class PropertyDetailsTaxAvoidanceScheme(isTaxAvoidance: Option[Boolean] = None)


object PropertyDetailsTaxAvoidanceScheme {
  given formats: OFormat[PropertyDetailsTaxAvoidanceScheme] = Json.format[PropertyDetailsTaxAvoidanceScheme]
}

case class PropertyDetailsTaxAvoidanceReferences(
                                       taxAvoidanceScheme: Option[String] = None,
                                       taxAvoidancePromoterReference: Option[String] = None)

object PropertyDetailsTaxAvoidanceReferences {
  given formats: OFormat[PropertyDetailsTaxAvoidanceReferences] = Json.format[PropertyDetailsTaxAvoidanceReferences]
}

case class PropertyDetailsSupportingInfo(supportingInfo: String)


object PropertyDetailsSupportingInfo {
  given formats: OFormat[PropertyDetailsSupportingInfo] = Json.format[PropertyDetailsSupportingInfo]
}

case class LineItem(lineItemType: String, startDate: LocalDate, endDate: LocalDate, description: Option[String] = None)

object LineItem {
  given formats: OFormat[LineItem] = Json.format[LineItem]
}

case class LineItemValue(propertyValue: BigDecimal, dateOfChange: LocalDate)

object LineItemValue {
  given formats: OFormat[LineItemValue] = Json.format[LineItemValue]
}

case class PropertyDetailsPeriod(isFullPeriod: Option[Boolean] = None,
                                 isTaxAvoidance: Option[Boolean] = None,
                                 taxAvoidanceScheme: Option[String] = None,
                                 taxAvoidancePromoterReference: Option[String] = None,
                                 supportingInfo: Option[String] = None,
                                 isInRelief: Option[Boolean] = None,
                                 liabilityPeriods: List[LineItem] = Nil,
                                 reliefPeriods: List[LineItem] = Nil)

object PropertyDetailsPeriod {
  given formats: OFormat[PropertyDetailsPeriod] = Json.format[PropertyDetailsPeriod]
}

case class CalculatedPeriod(value : BigDecimal,
                            startDate: LocalDate,
                            endDate: LocalDate,
                            lineItemType: String,
                            description: Option[String] = None
                           )

object CalculatedPeriod {
  given formats: OFormat[CalculatedPeriod] = Json.format[CalculatedPeriod]
}

case class PropertyDetailsCalculated(acquistionValueToUse : Option[BigDecimal] = None,
                                     acquistionDateToUse : Option[LocalDate] = None,
                                     professionalValuation: Option[Boolean] = None,
                                     liabilityPeriods: Seq[CalculatedPeriod] = Nil,
                                     reliefPeriods: Seq[CalculatedPeriod] = Nil,
                                     liabilityAmount: Option[BigDecimal] = None,
                                     amountDueOrRefund: Option[BigDecimal] = None)

object PropertyDetailsCalculated {
  given formats: OFormat[PropertyDetailsCalculated] = Json.format[PropertyDetailsCalculated]
}

case class PropertyDetails(id: String,
                           periodKey: Int,
                           addressProperty: PropertyDetailsAddress,
                           title: Option[PropertyDetailsTitle] = None,
                           value : Option[PropertyDetailsValue] = None,
                           period : Option[PropertyDetailsPeriod] = None,
                           calculated : Option[PropertyDetailsCalculated] = None,
                           formBundleReturn : Option[FormBundleReturn] = None,
                           bankDetails: Option[BankDetailsModel] = None)

object PropertyDetails {
  given formats: OFormat[PropertyDetails] = Json.format[PropertyDetails]
}
