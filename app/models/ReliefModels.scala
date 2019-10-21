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
import play.api.libs.json.{Json, OFormat}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._


case class TaxAvoidance(
                         rentalBusinessScheme: Option[String] = None,
                         rentalBusinessSchemePromoter: Option[String] = None,
                         openToPublicScheme: Option[String] = None,
                         openToPublicSchemePromoter: Option[String] = None,
                         propertyDeveloperScheme: Option[String] = None,
                         propertyDeveloperSchemePromoter: Option[String] = None,
                         propertyTradingScheme: Option[String] = None,
                         propertyTradingSchemePromoter: Option[String] = None,
                         lendingScheme: Option[String] = None,
                         lendingSchemePromoter: Option[String] = None,
                         employeeOccupationScheme: Option[String] = None,
                         employeeOccupationSchemePromoter: Option[String] = None,
                         farmHousesScheme: Option[String] = None,
                         farmHousesSchemePromoter: Option[String] = None,
                         socialHousingScheme: Option[String] = None,
                         socialHousingSchemePromoter: Option[String] = None,
                         equityReleaseScheme: Option[String] = None,
                         equityReleaseSchemePromoter: Option[String] = None
                         )

object TaxAvoidance {
  implicit val formats: OFormat[TaxAvoidance] = Json.format[TaxAvoidance]
}

case class Reliefs(periodKey: Int,
                   rentalBusiness: Boolean = false,
                   rentalBusinessDate: Option[LocalDate] = None,
                   openToPublic: Boolean = false,
                   openToPublicDate: Option[LocalDate] = None,
                   propertyDeveloper: Boolean = false,
                   propertyDeveloperDate: Option[LocalDate] = None,
                   propertyTrading: Boolean = false,
                   propertyTradingDate: Option[LocalDate] = None,
                   lending: Boolean = false,
                   lendingDate: Option[LocalDate] = None,
                   employeeOccupation: Boolean = false,
                   employeeOccupationDate: Option[LocalDate] = None,
                   farmHouses: Boolean = false,
                   farmHousesDate: Option[LocalDate] = None,
                   socialHousing: Boolean = false,
                   socialHousingDate: Option[LocalDate] = None,
                   equityRelease: Boolean = false,
                   equityReleaseDate: Option[LocalDate] = None,
                   isAvoidanceScheme: Option[Boolean] = None)

object Reliefs {
  implicit val formats: OFormat[Reliefs] = Json.format[Reliefs]
}


case class IsTaxAvoidance(isAvoidanceScheme: Option[Boolean] = None)


object IsTaxAvoidance {
  implicit val formats: OFormat[IsTaxAvoidance] = Json.format[IsTaxAvoidance]
}


case class ReliefsTaxAvoidance(periodKey: Int,
                               reliefs: Reliefs,
                               taxAvoidance: TaxAvoidance,
                               periodStartDate: LocalDate,
                               periodEndDate: LocalDate)

object ReliefsTaxAvoidance {
  implicit val formats: OFormat[ReliefsTaxAvoidance] = Json.format[ReliefsTaxAvoidance]
}
