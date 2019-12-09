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
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

import scala.collection.immutable

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
      RentalBusinessDesc -> "ated.choose-reliefs.rentalBusiness",
      OpenToPublicDesc -> "ated.choose-reliefs.openToPublic",
      PropDevDesc -> "ated.choose-reliefs.propertyDeveloper",
      PropTradingDesc -> "ated.choose-reliefs.propertyTrading",
      LendingDesc -> "ated.choose-reliefs.lending",
      EmpOccDesc -> "ated.choose-reliefs.employeeOccupation",
      FarmHouseDesc -> "ated.choose-reliefs.farmHouses",
      SocialHouseDesc -> "ated.choose-reliefs.socialHousing",
      EquityReleaseDesc -> "ated.choose-reliefs.equityRelease"
    )
    reliefsDescription.getOrElse(etmpReliefName, etmpReliefName)
  }

  def convertETMPReliefNameForSingleRelief(etmpReliefName: String): String = {
    val reliefsDescription = Map(
      RentalBusinessDesc -> "ated.choose-single-relief.rentalBusiness",
      OpenToPublicDesc -> "ated.choose-single-relief.openToPublic",
      PropDevDesc -> "ated.choose-single-relief.propertyDeveloper",
      PropTradingDesc -> "ated.choose-single-relief.propertyTrading",
      LendingDesc -> "ated.choose-single-relief.lending",
      EmpOccDesc -> "ated.choose-single-relief.employeeOccupation",
      FarmHouseDesc -> "ated.choose-single-relief.farmHouses",
      SocialHouseDesc -> "ated.choose-single-relief.socialHousing",
      EquityReleaseDesc -> "ated.choose-reliefs.equityRelease"
    )
    reliefsDescription.getOrElse(etmpReliefName, etmpReliefName)
  }

  private[utils] val dataCleanseMap = Map(
    "rentalBusinessDate"      -> "rentalBusiness",
    "openToPublicDate"        -> "openToPublic",
    "propertyDeveloperDate"   -> "propertyDeveloper",
    "propertyTradingDate"     -> "propertyTrading",
    "lendingDate"             -> "lending",
    "employeeOccupationDate"  -> "employeeOccupation",
    "farmHousesDate"          -> "farmHouses",
    "socialHousingDate"       -> "socialHousing",
    "equityReleaseDate"       -> "equityRelease"
  )

  def cleanDateTuples(data: Map[String, Seq[String]]): Map[String, Seq[String]] = {
    val keysToKeep: List[String] = data.flatMap { case (key, entry) =>
      entry.headOption
        .filter(_ == "true")
        .map(_ => key)
    }.toList

    data.filter { case (key, _) =>
      val takeWhileKey: String = key.takeWhile(_ != '.')

      dataCleanseMap.get(takeWhileKey) match {
        case Some(dateField)  => keysToKeep.contains(dateField)
        case _                => true
      }
    }
  }

}
