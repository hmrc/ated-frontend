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

package utils

import models._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

object ReliefsUtils extends ReliefConstants {

  def createReliefsTaxAvoidance(periodKey: Int, isAvoidanceScheme : Option[Boolean] = None) = {
    ReliefsTaxAvoidance(periodKey = periodKey,
      reliefs = Reliefs(periodKey = periodKey, isAvoidanceScheme = isAvoidanceScheme),
      taxAvoidance = TaxAvoidance(),
      periodStartDate = PeriodUtils.periodStartDate(periodKey),
      periodEndDate = PeriodUtils.periodEndDate(periodKey))
  }

  def convertETMPReliefNameForMultipleReliefs(etmpReliefName: String): String = {
    val reliefsDescription = Map(
      RentalBusinessDesc -> Messages("ated.choose-reliefs.rentalBusiness"),
      OpenToPublicDesc -> Messages("ated.choose-reliefs.openToPublic"),
      PropDevDesc -> Messages("ated.choose-reliefs.propertyDeveloper"),
      PropTradingDesc -> Messages("ated.choose-reliefs.propertyTrading"),
      LendingDesc -> Messages("ated.choose-reliefs.lending"),
      EmpOccDesc -> Messages("ated.choose-reliefs.employeeOccupation"),
      FarmHouseDesc -> Messages("ated.choose-reliefs.farmHouses"),
      SocialHouseDesc -> Messages("ated.choose-reliefs.socialHousing"),
      EquityReleaseDesc -> Messages("ated.choose-reliefs.equityRelease")
    )
    reliefsDescription.getOrElse(etmpReliefName, etmpReliefName)
  }

  def convertETMPReliefNameForSingleRelief(etmpReliefName: String): String = {
    val reliefsDescription = Map(
      RentalBusinessDesc -> Messages("ated.choose-single-relief.rentalBusiness"),
      OpenToPublicDesc -> Messages("ated.choose-single-relief.openToPublic"),
      PropDevDesc -> Messages("ated.choose-single-relief.propertyDeveloper"),
      PropTradingDesc -> Messages("ated.choose-single-relief.propertyTrading"),
      LendingDesc -> Messages("ated.choose-single-relief.lending"),
      EmpOccDesc -> Messages("ated.choose-single-relief.employeeOccupation"),
      FarmHouseDesc -> Messages("ated.choose-single-relief.farmHouses"),
      SocialHouseDesc -> Messages("ated.choose-single-relief.socialHousing"),
      EquityReleaseDesc -> Messages("ated.choose-reliefs.equityRelease")
    )
    reliefsDescription.getOrElse(etmpReliefName, etmpReliefName)
  }

}
