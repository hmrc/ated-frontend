/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class ReliefsUtilsSpec extends PlaySpec with OneServerPerSuite with ReliefConstants {

  "ReliefsUtils" must {

    "return the ATED relief description for multiple properties when passed the ETMP description" in {
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(RentalBusinessDesc) must be("Rental businesses")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(OpenToPublicDesc) must be("Open to the public")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(PropDevDesc) must be("Property developers")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(EmpOccDesc) must be("Employee occupation")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(PropTradingDesc) must be("Property trading")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(LendingDesc) must be("Lending")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(FarmHouseDesc) must be("Farmhouses")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs(SocialHouseDesc) must be("Social housing")
      ReliefsUtils.convertETMPReliefNameForMultipleReliefs("not found description") must be("not found description")
    }

    "return the ATED relief description when passed the ETMP description" in {
      ReliefsUtils.convertETMPReliefNameForSingleRelief(RentalBusinessDesc) must be("Rental business")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(OpenToPublicDesc) must be("Open to the public")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(PropDevDesc) must be("Property developer")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(EmpOccDesc) must be("Employee occupation")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(PropTradingDesc) must be("Property trading")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(LendingDesc) must be("Lending")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(FarmHouseDesc) must be("Farmhouse")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(SocialHouseDesc) must be("Social housing")
      ReliefsUtils.convertETMPReliefNameForSingleRelief("not found description") must be("not found description")
    }
  }

}
