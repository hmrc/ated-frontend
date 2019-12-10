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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class ReliefsUtilsSpec extends PlaySpec {

  "ReliefsUtils" must {
    "return the ATED relief description for multiple properties when passed the ETMP description" in {
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(ReliefsUtils.RentalBusinessDesc) must be("ated.choose-reliefs.rentalBusiness")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(ReliefsUtils.OpenToPublicDesc) must be("ated.choose-reliefs.openToPublic")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(ReliefsUtils.PropDevDesc) must be("ated.choose-reliefs.propertyDeveloper")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(ReliefsUtils.EmpOccDesc) must be("ated.choose-reliefs.employeeOccupation")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(ReliefsUtils.PropTradingDesc) must be("ated.choose-reliefs.propertyTrading")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(ReliefsUtils.LendingDesc) must be("ated.choose-reliefs.lending")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(ReliefsUtils.FarmHouseDesc) must be("ated.choose-reliefs.farmHouses")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(ReliefsUtils.SocialHouseDesc) must be("ated.choose-reliefs.socialHousing")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs("not found description") must be("not found description")
    }

    "return the ATED relief description when passed the ETMP description" in {
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.RentalBusinessDesc) must be("ated.choose-single-relief.rentalBusiness")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.OpenToPublicDesc) must be("ated.choose-single-relief.openToPublic")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.PropDevDesc) must be("ated.choose-single-relief.propertyDeveloper")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.EmpOccDesc) must be("ated.choose-single-relief.employeeOccupation")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.PropTradingDesc) must be("ated.choose-single-relief.propertyTrading")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.LendingDesc) must be("ated.choose-single-relief.lending")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.FarmHouseDesc) must be("ated.choose-single-relief.farmHouses")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.SocialHouseDesc) must be("ated.choose-single-relief.socialHousing")
      ReliefsUtils.convertETMPReliefNameForSingleRelief("not found description") must be("not found description")
    }
  }

  "cleanDateTuples" should {
    "remove date fields where the checkbox is unticked" in {
      val mapTuple: Map[String, Seq[String]] = ReliefsUtils.dataCleanseMap.keys
        .toList
        .zipWithIndex
        .flatMap {
          case (key, i) =>
            val fields = List(key + ".year", key + ".month", key + ".day")
            if (i % 2 == 0) {
              fields
            } else {
              fields :+ ReliefsUtils.dataCleanseMap(key)
            }
        }
        .map { key => (key, "true") }
        .toMap
        .mapValues(str => Seq(str))

      ReliefsUtils.cleanDateTuples(mapTuple).size mustBe 16
    }
  }

}
