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

class ReliefsUtilsSpec extends PlaySpec with GuiceOneServerPerSuite with ReliefConstants {

  "ReliefsUtils" must {

    "return the ATED relief description for multiple properties when passed the ETMP description" in {
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(RentalBusinessDesc) must be("ated.choose-reliefs.rentalBusiness")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(OpenToPublicDesc) must be("ated.choose-reliefs.openToPublic")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(PropDevDesc) must be("ated.choose-reliefs.propertyDeveloper")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(EmpOccDesc) must be("ated.choose-reliefs.employeeOccupation")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(PropTradingDesc) must be("ated.choose-reliefs.propertyTrading")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(LendingDesc) must be("ated.choose-reliefs.lending")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(FarmHouseDesc) must be("ated.choose-reliefs.farmHouses")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(SocialHouseDesc) must be("ated.choose-reliefs.socialHousing")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs("not found description") must be("not found description")
    }

    "return the ATED relief description when passed the ETMP description" in {
      ReliefsUtils.convertETMPReliefNameForSingleRelief(RentalBusinessDesc) must be("ated.choose-single-relief.rentalBusiness")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(OpenToPublicDesc) must be("ated.choose-single-relief.openToPublic")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(PropDevDesc) must be("ated.choose-single-relief.propertyDeveloper")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(EmpOccDesc) must be("ated.choose-single-relief.employeeOccupation")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(PropTradingDesc) must be("ated.choose-single-relief.propertyTrading")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(LendingDesc) must be("ated.choose-single-relief.lending")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(FarmHouseDesc) must be("ated.choose-single-relief.farmHouses")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(SocialHouseDesc) must be("ated.choose-single-relief.socialHousing")
      ReliefsUtils.convertETMPReliefNameForSingleRelief("not found description") must be("not found description")
    }
  }

}
