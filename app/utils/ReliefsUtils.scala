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

package utils

import models._

object ReliefsUtils extends {

  val RentalBusinessDesc = "Property rental businesses"
  val OpenToPublicDesc = "Dwellings opened to the public"
  val PropDevDesc = "Property developers"
  val EmpOccDesc = "Dwellings used for trade purposes"
  val PropTradingDesc = "Property traders carrying on a property trading business"
  val LendingDesc = "Financial institutions acquiring dwellings in the course of lending"
  val FarmHouseDesc = "Farmhouses"
  val SocialHouseDesc = "Registered providers of Social Housing"
  val EquityReleaseDesc = "Equity Release Scheme"

  def createReliefsTaxAvoidance(periodKey: Int, isAvoidanceScheme: Option[Boolean] = None, atedRefNo: String): ReliefsTaxAvoidance = {
    ReliefsTaxAvoidance(atedRefNo, periodKey = periodKey,
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

  def convertETMPReliefNameForSingleRelief(etmpReliefName: String, periodKey: Int): String = {
    val reliefsDescription = Map(
      RentalBusinessDesc -> "ated.choose-single-relief.rentalBusiness",
      OpenToPublicDesc -> "ated.choose-single-relief.openToPublic",
      PropDevDesc -> "ated.choose-single-relief.propertyDeveloper",
      PropTradingDesc -> "ated.choose-single-relief.propertyTrading",
      LendingDesc -> "ated.choose-single-relief.lending",
      EmpOccDesc -> "ated.choose-single-relief.employeeOccupation",
      FarmHouseDesc -> "ated.choose-single-relief.farmHouses",
      SocialHouseDesc -> (if (periodKey >= 2020) {
        "ated.choose-single-relief.providerSocialOrHousing"
      } else {
        "ated.choose-single-relief.socialHousing"
      }),
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

  def partitionNewestReliefForType(refReturns: Seq[SubmittedReliefReturns]): (Seq[SubmittedReliefReturns], Seq[SubmittedReliefReturns]) = {
    @scala.annotation.tailrec
    def parseRecentReturns(remReturns: Seq[SubmittedReliefReturns], answer: Seq[SubmittedReliefReturns]): Seq[SubmittedReliefReturns] =
      remReturns match {
        case remReturn :: rest if answer.nonEmpty =>
          val newAnswer = answer.head.dateOfSubmission.compareTo(remReturn.dateOfSubmission) match {
            case -1 => Seq(remReturn)
            case 0  => answer :+ remReturn
            case _  => answer
          }

          parseRecentReturns(rest, newAnswer)
        case remReturn :: rest => parseRecentReturns(rest, Seq(remReturn))
        case _ if remReturns.size == 1 => Seq(remReturns.head)
        case _ => answer
      }

    val newestMap = refReturns
      .groupBy(_.reliefType)
      .map { case (_, returnsForType) =>
        val recentReturnsForType: Seq[SubmittedReliefReturns] = parseRecentReturns(returnsForType, Nil)
        returnsForType.partition(recentReturnsForType.contains(_))
      }

    (newestMap.keys.flatten.toSeq, newestMap.values.flatten.toSeq)
  }
}
