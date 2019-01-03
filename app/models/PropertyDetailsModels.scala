/*
 * Copyright 2019 HM Revenue & Customs
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


sealed trait PeriodValidity

case class PeriodInvalid(inputDateType: String) extends PeriodValidity

case object PeriodValid extends PeriodValidity

case class PropertyDetailsAddress(line_1: String, line_2: String, line_3: Option[String], line_4: Option[String],
                                  postcode: Option[String] = None) {
  override def toString = {

    val line3display = line_3.map(line3 => s", $line3, " ).fold("")(x=>x)
    val line4display = line_4.map(line4 => s"$line4, " ).fold("")(x=>x)
    val postcodeDisplay = postcode.map(postcode1 => s"$postcode1").fold("")(x=>x)
    s"$line_1, $line_2 $line3display$line4display$postcodeDisplay"
  }
}

object PropertyDetailsAddress {
  implicit val formats = Json.format[PropertyDetailsAddress]
}

case class PropertyDetailsTitle(titleNumber: String)

object PropertyDetailsTitle {
  implicit val formats = Json.format[PropertyDetailsTitle]
}


case class PropertyDetailsValue( anAcquisition: Option[Boolean] = None,
                                 isPropertyRevalued: Option[Boolean] = None,
                                 revaluedValue: Option[BigDecimal] = None,
                                 revaluedDate: Option[LocalDate] = None,
                                 partAcqDispDate: Option[LocalDate] = None,
                                 isOwnedBefore2012: Option[Boolean] = None,
                                 ownedBefore2012Value: Option[BigDecimal] = None,
                                 isNewBuild: Option[Boolean] = None,
                                 newBuildValue: Option[BigDecimal] = None,
                                 newBuildDate: Option[LocalDate] = None,
                                 localAuthRegDate: Option[LocalDate] = None,
                                 notNewBuildValue: Option[BigDecimal] = None,
                                 notNewBuildDate: Option[LocalDate] = None,
                                 isValuedByAgent: Option[Boolean] = None,
                                 valuationDate: Option[LocalDate] = None,
                                 hasValueChanged: Option[Boolean] = None
                               )

object PropertyDetailsValue {
  implicit val formats = Json.format[PropertyDetailsValue]
}

case class PropertyDetailsAcquisition( anAcquisition: Option[Boolean] = None)

object PropertyDetailsAcquisition {
  implicit val formats = Json.format[PropertyDetailsAcquisition]
}

case class HasValueChanged(hasValueChanged: Option[Boolean] = None)

object HasValueChanged {
  implicit val formats = Json.format[HasValueChanged]
}

case class PropertyDetailsRevalued(isPropertyRevalued: Option[Boolean] = None,
                                   revaluedValue: Option[BigDecimal] = None,
                                   revaluedDate: Option[LocalDate] = None,
                                   partAcqDispDate: Option[LocalDate] = None)

object PropertyDetailsRevalued {
  implicit val formats = Json.format[PropertyDetailsRevalued]
}

case class PropertyDetailsOwnedBefore(isOwnedBefore2012: Option[Boolean] = None,
                                      ownedBefore2012Value: Option[BigDecimal] = None)

object PropertyDetailsOwnedBefore {
  implicit val formats = Json.format[PropertyDetailsOwnedBefore]
}

case class PropertyDetailsProfessionallyValued(isValuedByAgent: Option[Boolean] = None)

object PropertyDetailsProfessionallyValued {
  implicit val formats = Json.format[PropertyDetailsProfessionallyValued]
}

case class PropertyDetailsNewBuild(
                                    isNewBuild: Option[Boolean] = None,
                                    newBuildValue: Option[BigDecimal] = None,
                                    newBuildDate: Option[LocalDate] = None,
                                    localAuthRegDate: Option[LocalDate] = None,
                                    notNewBuildValue: Option[BigDecimal] = None,
                                    notNewBuildDate: Option[LocalDate] = None
                                  )

object PropertyDetailsNewBuild {
  implicit val formats = Json.format[PropertyDetailsNewBuild]
}

case class PropertyDetailsFullTaxPeriod(isFullPeriod: Option[Boolean] = None)


object PropertyDetailsFullTaxPeriod {
  implicit val formats = Json.format[PropertyDetailsFullTaxPeriod]
}

case class PropertyDetailsDatesLiable(startDate: LocalDate,
                                      endDate: LocalDate)

object PropertyDetailsDatesLiable {
  implicit val formats = Json.format[PropertyDetailsDatesLiable]
}

case class IsFullTaxPeriod(isFullPeriod: Boolean, datesLiable: Option[PropertyDetailsDatesLiable])

object IsFullTaxPeriod {
  implicit val formats = Json.format[IsFullTaxPeriod]
}


case class PeriodChooseRelief(reliefDescription: String)

object PeriodChooseRelief {
  implicit val formats = Json.format[PeriodChooseRelief]
}


case class PropertyDetailsDatesInRelief(startDate: LocalDate,
                                        endDate: LocalDate,
                                        description: Option[String] = None)

object PropertyDetailsDatesInRelief {
  implicit val formats = Json.format[PropertyDetailsDatesInRelief]
}


case class PropertyDetailsInRelief(isInRelief: Option[Boolean] = None)


object PropertyDetailsInRelief {
  implicit val formats = Json.format[PropertyDetailsInRelief]
}

case class PropertyDetailsTaxAvoidance(isTaxAvoidance: Option[Boolean] = None,
                                       taxAvoidanceScheme: Option[String] = None,
                                       taxAvoidancePromoterReference: Option[String] = None)


object PropertyDetailsTaxAvoidance {
  implicit val formats = Json.format[PropertyDetailsTaxAvoidance]
}

case class PropertyDetailsSupportingInfo(supportingInfo: String)


object PropertyDetailsSupportingInfo {
  implicit val formats = Json.format[PropertyDetailsSupportingInfo]
}

case class LineItem(lineItemType: String, startDate: LocalDate, endDate: LocalDate, description: Option[String] = None)

object LineItem {
  implicit val formats = Json.format[LineItem]
}

case class LineItemValue(propertyValue: BigDecimal, dateOfChange: LocalDate)

object LineItemValue {
  implicit val formats = Json.format[LineItemValue]
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
  implicit val formats = Json.format[PropertyDetailsPeriod]
}

case class CalculatedPeriod(value : BigDecimal,
                            startDate: LocalDate,
                            endDate: LocalDate,
                            lineItemType: String,
                            description: Option[String] = None
                           )

object CalculatedPeriod {
  implicit val formats = Json.format[CalculatedPeriod]
}

case class PropertyDetailsCalculated(acquistionValueToUse : Option[BigDecimal] = None,
                                     acquistionDateToUse : Option[LocalDate] = None,
                                     professionalValuation: Option[Boolean] = None,
                                     liabilityPeriods: Seq[CalculatedPeriod] = Nil,
                                     reliefPeriods: Seq[CalculatedPeriod] = Nil,
                                     liabilityAmount: Option[BigDecimal] = None,
                                     amountDueOrRefund: Option[BigDecimal] = None)

object PropertyDetailsCalculated {
  implicit val formats = Json.format[PropertyDetailsCalculated]
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
  implicit val formats = Json.format[PropertyDetails]
}
