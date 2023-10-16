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

import config.ApplicationConfig
import models.SubmittedReliefReturns
import org.joda.time.LocalDate
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class ReliefsUtilsSpec extends PlaySpec with MockitoSugar with GuiceOneServerPerSuite {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

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

    "return the ATED relief description when passed the ETMP description, in 2015" in {
      val periodKey = 2015

      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.RentalBusinessDesc, periodKey) must be("ated.choose-single-relief.rentalBusiness")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.OpenToPublicDesc, periodKey) must be("ated.choose-single-relief.openToPublic")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.PropDevDesc, periodKey) must be("ated.choose-single-relief.propertyDeveloper")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.EmpOccDesc, periodKey) must be("ated.choose-single-relief.employeeOccupation")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.PropTradingDesc, periodKey) must be("ated.choose-single-relief.propertyTrading")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.LendingDesc, periodKey) must be("ated.choose-single-relief.lending")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.FarmHouseDesc, periodKey) must be("ated.choose-single-relief.farmHouses")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.SocialHouseDesc, periodKey) must be("ated.choose-single-relief.socialHousing")
      ReliefsUtils.convertETMPReliefNameForSingleRelief("not found description", periodKey) must be("not found description")
    }

    "return the ATED relief description when passed the ETMP description, in 2020" in {
      val periodKey = 2020

      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.RentalBusinessDesc, periodKey) must be("ated.choose-single-relief.rentalBusiness")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.OpenToPublicDesc, periodKey) must be("ated.choose-single-relief.openToPublic")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.PropDevDesc, periodKey) must be("ated.choose-single-relief.propertyDeveloper")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.EmpOccDesc, periodKey) must be("ated.choose-single-relief.employeeOccupation")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.PropTradingDesc, periodKey) must be("ated.choose-single-relief.propertyTrading")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.LendingDesc, periodKey) must be("ated.choose-single-relief.lending")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.FarmHouseDesc, periodKey) must be("ated.choose-single-relief.farmHouses")
      ReliefsUtils.convertETMPReliefNameForSingleRelief(ReliefsUtils.SocialHouseDesc, periodKey) must be("ated.choose-single-relief.providerSocialOrHousing")
      ReliefsUtils.convertETMPReliefNameForSingleRelief("not found description", periodKey) must be("not found description")
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
        .view.mapValues(str => Seq(str)).toMap

      ReliefsUtils.cleanDateTuples(mapTuple).size mustBe 16
    }
  }

  "partitionNewestReliefForType" should {
    "provide reliefs of one of each most recent type given a set of reliefs" in {
      val newerType1Return = SubmittedReliefReturns("no1", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(1))
      val olderType1Return = SubmittedReliefReturns("no2", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(2))

      val testReliefReturns = Seq(newerType1Return, olderType1Return)

      ReliefsUtils.partitionNewestReliefForType(testReliefReturns) mustBe Tuple2(Seq(newerType1Return), Seq(olderType1Return))
    }

    "provide reliefs of one of each most recent type given a set of reliefs with 2 older 1 new of the same type" in {
      val newerType1Return = SubmittedReliefReturns("no1", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(1))
      val olderType1Return = SubmittedReliefReturns("no2", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(2))
      val older2Type1Return = SubmittedReliefReturns("no3", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(3))

      val testReliefReturns = Seq(newerType1Return, olderType1Return, older2Type1Return)

      ReliefsUtils.partitionNewestReliefForType(testReliefReturns) mustBe Tuple2(Seq(newerType1Return), Seq(olderType1Return, older2Type1Return))
    }

    "provide reliefs for 2 types, one with two reliefs, one with just one" in {
      val newerType1Return = SubmittedReliefReturns("no1", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(1))
      val olderType1Return = SubmittedReliefReturns("no2", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(2))
      val type2Return = SubmittedReliefReturns("no3", "type 2", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(3))

      val testReliefReturns = Seq(newerType1Return, olderType1Return, type2Return)
      println(ReliefsUtils.partitionNewestReliefForType(testReliefReturns))
      ReliefsUtils.partitionNewestReliefForType(testReliefReturns) mustBe Tuple2(Seq(type2Return, newerType1Return), Seq(olderType1Return))
    }

    "provide reliefs for 2 types, one with two reliefs of the same date, one with two different dates" in {
      val sameDate1Type1Return = SubmittedReliefReturns("no1", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(1))
      val sameDate2Type1Return = SubmittedReliefReturns("no2", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(1))
      val newertype2Return = SubmittedReliefReturns("no3", "type 2", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(3))
      val oldertype2Return = SubmittedReliefReturns("no3", "type 2", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(4))

      val testReliefReturns = Seq(sameDate1Type1Return, sameDate2Type1Return, newertype2Return, oldertype2Return)

      val partitioned = ReliefsUtils.partitionNewestReliefForType(testReliefReturns)
      partitioned._1 must contain(sameDate1Type1Return)
      partitioned._1 must contain(sameDate2Type1Return)
      partitioned._1 must contain(newertype2Return)
      partitioned._2 must contain(oldertype2Return)
    }
  }

}
