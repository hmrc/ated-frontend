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
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class PeriodUtilsSpec extends PlaySpec {

  val `2014` = 2014
  val `2015` = 2015
  val `2016` = 2016
  val `2017` = 2017
  val `2018` = 2018

  val rentalBusinessDesc = "Property rental businesses"
  val openToPublicDesc = "Dwellings opened to the public"

  "PeriodUtils" must {
    "are valid start and end Date" in {
      PeriodUtils.periodStartDate(`2015`) must be(new LocalDate("2015-04-01"))
      PeriodUtils.periodStartDate(`2016`) must be(new LocalDate("2016-04-01"))

      PeriodUtils.periodEndDate(`2015`) must be(new LocalDate("2016-03-31"))
      PeriodUtils.periodEndDate(`2016`) must be(new LocalDate("2017-03-31"))
    }

    "calculatePeriod" must {
      "return the correct periodKey for the current tax year" in {
        PeriodUtils.calculatePeriod() must be(PeriodUtils.calculatePeriod())
      }

      "return the correct periodKey for before April" in {
        PeriodUtils.calculatePeriod(new LocalDate("2016-3-1")) must be(`2015`)
      }

      "return the correct periodKey for after April" in {
        PeriodUtils.calculatePeriod(new LocalDate("2016-5-1")) must be(`2016`)
      }

      "return the correct periodKey for the previous tax year" in {
        PeriodUtils.calculatePeriod(new LocalDate("2014-5-1")) must be(`2014`)
      }
    }

    "isPeriodTooEarly" must {
      "calculate if the period is within the periodDate" in {
        PeriodUtils.isPeriodTooEarly(PeriodUtils.calculatePeriod(), Some(new LocalDate().minusYears(1))) must be(true)
      }

      "calculate if the period is too early for the periodDate" in {
        PeriodUtils.isPeriodTooEarly(`2015`, None) must be(false)
      }
    }

    "isPeriodTooEarlyBefore2012" must {
      "calculate if the period is before the 01-04-2012" in {
        PeriodUtils.isPeriodTooEarlyBefore2012(Some(new LocalDate("2012-03-01"))) must be(true)
      }

      "calculate if the period is after the 01-04-2012" in {
        PeriodUtils.isPeriodTooEarlyBefore2012(Some(new LocalDate("2012-06-01"))) must be(false)
      }

      "check false when no data is passed" in {
        PeriodUtils.isPeriodTooEarlyBefore2012(None) must be(false)
      }
    }

    "isAfterPresentDay" must {
      "check if the period is after today" in {
        PeriodUtils.isAfterPresentDay(Some(LocalDate.now().plusDays(1))) must be(true)
      }
      "check if the period is before today" in {
        PeriodUtils.isAfterPresentDay(Some(LocalDate.now.minusDays(1))) must be(false)
      }
      "check false is returned when no date is passed" in {
        PeriodUtils.isAfterPresentDay(None) must be(false)
      }
    }

    "isPeriodTooLate" must {
      "calculate if the period is within the periodDate" in {
        PeriodUtils.isPeriodTooLate(PeriodUtils.calculatePeriod(), Some(new LocalDate().plusYears(1))) must be(true)
      }
      "calculate if the period is too late for the periodDate" in {
        PeriodUtils.isPeriodTooLate(`2015`, None) must be(false)
      }
    }
  }


  "convert period" must {
    "return None if we have period" in {
      PeriodUtils.getDisplayPeriods(None).isEmpty must be(true)
    }

    "return an ordered list if we have periods" in {
      val periodKey = 2015
      val liabilityPeriod1 = LineItem(AtedConstants.LiabilityReturnType,new LocalDate(s"$periodKey-4-1"),
        new LocalDate(s"$periodKey-8-31"))
      val liabilityPeriod2 = LineItem(AtedConstants.LiabilityReturnType,new LocalDate(s"${periodKey + 1}-2-1"),
        new LocalDate(s"${periodKey + 1}-3-31"))
      val reliefPeriod1 = LineItem(AtedConstants.ReliefReturnType,new LocalDate(s"$periodKey-9-1"),
        new LocalDate(s"${periodKey + 1}-1-31"), Some("Property rental businesses"))
      val liabilityPeriods = List(liabilityPeriod2, liabilityPeriod1)
      val reliefPeriods = List(reliefPeriod1)

      val propertyDetailsPeriods = Some(new PropertyDetailsPeriod(
        isFullPeriod = Some(false),
        liabilityPeriods = liabilityPeriods,
        reliefPeriods = reliefPeriods,
        isTaxAvoidance =  Some(true),
        taxAvoidanceScheme =  Some("taxAvoidanceScheme"),
        supportingInfo = Some("supportingInfo"),
        isInRelief =  Some(true)
      ))
      val lineItems = PeriodUtils.getDisplayPeriods(propertyDetailsPeriods)
      val expected = List(liabilityPeriod1.copy(description = Some("ated.property-details-period.liability.return-type")),
        reliefPeriod1.copy(description = Some("ated.choose-single-relief.rentalBusiness")),
        liabilityPeriod2.copy(description = Some("ated.property-details-period.liability.return-type"))
      )

      lineItems.isEmpty must be(false)
      lineItems must be (expected)
    }
  }

  "convert line items for display" must {
    "return None if we have period" in {
      PeriodUtils.getDisplayFormBundleProperties(Nil).isEmpty must be(true)
    }

    "return an ordered list of line items wherever the value or type has changed : Each item has changed and we have disposed of the property" in {
      val periodKey = 2015
      val liabilityPeriod1 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"$periodKey-4-1"),
        new LocalDate(s"$periodKey-8-31"),  AtedConstants.LiabilityReturnType, None)
      val liabilityPeriod2 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"${periodKey + 1}-2-1"),
        new LocalDate(s"${periodKey + 1}-3-01"), AtedConstants.LiabilityReturnType, None)
      val reliefPeriod1 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"$periodKey-9-1"),
        new LocalDate(s"${periodKey + 1}-1-31"), AtedConstants.ReliefReturnType, Some("Property rental businesses"))
      val disposePeriod = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"${periodKey + 1}-3-02"),
        new LocalDate(s"${periodKey + 1}-3-31"), AtedConstants.DisposeReturnType, None)

      val liabilityPeriods = List(liabilityPeriod2, liabilityPeriod1, disposePeriod)
      val reliefPeriods = List(reliefPeriod1)
      val lineItems = PeriodUtils.getDisplayFormBundleProperties(liabilityPeriods ++ reliefPeriods)

      val expected = List(
        LineItem(liabilityPeriod1.`type`, liabilityPeriod1.dateFrom, liabilityPeriod1.dateTo, Some("ated.property-details-period.liability.return-type")),
        LineItem(reliefPeriod1.`type`, reliefPeriod1.dateFrom, reliefPeriod1.dateTo, Some("ated.choose-single-relief.rentalBusiness")),
        LineItem(liabilityPeriod2.`type`, liabilityPeriod2.dateFrom, liabilityPeriod2.dateTo, Some("ated.property-details-period.liability.return-type")),
        LineItem(disposePeriod.`type`, disposePeriod.dateFrom, disposePeriod.dateTo, Some("ated.property-details-period.dispose.return-type"))
      )
      lineItems.isEmpty must be(false)
      lineItems must be (expected)
    }

    "return an ordered list of line items wherever the value or type has changed : Merge two periods where value has changed and we have " in {
      val periodKey = 2015
      val liabilityPeriod1a = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"$periodKey-4-1"),
        new LocalDate(s"$periodKey-6-30"),  AtedConstants.LiabilityReturnType, None)
      val liabilityPeriod1b = FormBundleProperty(BigDecimal(999.45), new LocalDate(s"$periodKey-7-1"),
        new LocalDate(s"$periodKey-8-31"),  AtedConstants.LiabilityReturnType, None)
      val liabilityPeriods = List(liabilityPeriod1a, liabilityPeriod1b)
      val lineItems = PeriodUtils.getDisplayFormBundleProperties(liabilityPeriods)
      val mergedLiability =  LineItem(
        liabilityPeriod1a.`type`,
        liabilityPeriod1a.dateFrom,
        liabilityPeriod1b.dateTo,
        Some("ated.property-details-period.liability.return-type")
      )
      val expected = List(
        mergedLiability
      )

      lineItems.isEmpty must be(false)
      lineItems must be (expected)
    }

    "return an ordered list of line items wherever the value or type has changed : Merge multiple periods where value has changed" in {
      val periodKey = 2015
      val liabilityPeriod1a = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"$periodKey-4-1"),
        new LocalDate(s"$periodKey-6-30"),  AtedConstants.LiabilityReturnType, None)
      val liabilityPeriod1b = FormBundleProperty(BigDecimal(999.45), new LocalDate(s"$periodKey-7-1"),
        new LocalDate(s"$periodKey-8-31"),  AtedConstants.LiabilityReturnType, None)

      val reliefPeriod1a = FormBundleProperty(BigDecimal(999.45), new LocalDate(s"$periodKey-9-1"),
        new LocalDate(s"$periodKey-10-31"), AtedConstants.ReliefReturnType, Some(rentalBusinessDesc))
      val reliefPeriod1b = FormBundleProperty(BigDecimal(10009.45), new LocalDate(s"$periodKey-10-1"),
        new LocalDate(s"${periodKey + 1}-12-31"), AtedConstants.ReliefReturnType, Some(rentalBusinessDesc))

      val reliefPeriod2 = FormBundleProperty(BigDecimal(10009.45), new LocalDate(s"$periodKey-12-1"),
        new LocalDate(s"${periodKey + 1}-1-31"), AtedConstants.ReliefReturnType, Some(openToPublicDesc))

      val liabilityPeriod2 = FormBundleProperty(BigDecimal(10009.45), new LocalDate(s"${periodKey + 1}-2-1"),
        new LocalDate(s"${periodKey + 1}-3-31"), AtedConstants.LiabilityReturnType, None)


      val liabilityPeriods = List(liabilityPeriod2, liabilityPeriod1a, liabilityPeriod1b)
      val reliefPeriods = List(reliefPeriod1a, reliefPeriod1b, reliefPeriod2)

      val lineItems = PeriodUtils.getDisplayFormBundleProperties(liabilityPeriods ++ reliefPeriods)

      val mergedLiability =  LineItem(
        liabilityPeriod1a.`type`,
        liabilityPeriod1a.dateFrom,
        liabilityPeriod1b.dateTo,
        Some("ated.property-details-period.liability.return-type")
      )
      val mergedRelief =  LineItem(reliefPeriod1a.`type`, reliefPeriod1a.dateFrom, reliefPeriod1b.dateTo, Some("ated.choose-single-relief.rentalBusiness"))
      val expected = List(
        mergedLiability,
        mergedRelief,
        LineItem(reliefPeriod2.`type`, reliefPeriod2.dateFrom, reliefPeriod2.dateTo, Some("ated.choose-single-relief.openToPublic")),
        LineItem(liabilityPeriod2.`type`, liabilityPeriod2.dateFrom, liabilityPeriod2.dateTo, Some("ated.property-details-period.liability.return-type"))
      )

      lineItems.isEmpty must be(false)
      lineItems must be (expected)
    }
  }


  "getPeriods" must {
    "return correct list of periods for dates before april" in {
      val startDate = new LocalDate(`2015`, 1, 1)
      val endDate = new LocalDate(`2017`, 1, 1)

      PeriodUtils.getPeriods(startDate, endDate).reverse must be (List("2014" -> "2014 to 2015", "2015" -> "2015 to 2016", "2016" -> "2016 to 2017"))
    }

    "return correct list of periods for dates after april" in {
      val startDate = new LocalDate(`2015`, 4, 1)
      val endDate = new LocalDate(`2017`, 3, 8)

      PeriodUtils.getPeriods(startDate, endDate).reverse must be (List("2015" -> "2015 to 2016", "2016" -> "2016 to 2017", "2017" -> "2017 to 2018"))
    }

    "return correct list of periods for date 1 March (don't show period for next tax year)" in {
      val startDate = new LocalDate(`2015`, 4, 1)
      val endDate = new LocalDate(`2017`, 3, 1)

      PeriodUtils.getPeriods(startDate, endDate).reverse must be (List("2015" -> "2015 to 2016", "2016" -> "2016 to 2017"))
    }

    "return correct list of periods for date 2 March (don't show period for next tax year)" in {

      val startDate = new LocalDate(`2015`, 4, 1)
      val endDate = new LocalDate(`2017`, 3, 2)

      PeriodUtils.getPeriods(startDate, endDate).reverse must be (List("2015" -> "2015 to 2016", "2016" -> "2016 to 2017"))
    }

    "return correct list of periods for date 3 March (don't show period for next tax year)" in {

      val startDate = new LocalDate(`2015`, 4, 1)
      val endDate = new LocalDate(`2017`, 3, 3)

      PeriodUtils.getPeriods(startDate, endDate).reverse must be (List("2015" -> "2015 to 2016", "2016" -> "2016 to 2017"))
    }

    "return correct list of periods for date  4 March (don't show period for next tax year)" in {

      val startDate = new LocalDate(`2015`, 4, 1)
      val endDate = new LocalDate(`2017`, 3, 4)

      PeriodUtils.getPeriods(startDate, endDate).reverse must be (List("2015" -> "2015 to 2016", "2016" -> "2016 to 2017"))
    }

    "return correct list of periods for date after 5 March (show period for next tax year)" in {

      val startDate = new LocalDate(`2015`, 4, 1)
      val endDate = new LocalDate(`2017`, 3, 5)

      PeriodUtils.getPeriods(startDate, endDate).reverse must be (List("2015" -> "2015 to 2016", "2016" -> "2016 to 2017", "2017" -> "2017 to 2018"))
    }
  }

  "getOrderedReturnPeriodValues" must {
    val periodKey = 2015
    val dateOfValuation = new LocalDate(s"${periodKey}-4-1")

    "return Nil if we have no line items" in {

      val valueTuples = PeriodUtils.getOrderedReturnPeriodValues(Nil, None)
      valueTuples.isEmpty must be (true)
    }

    "return the single value and date when we have one line item" in {

      val liabilityPeriod1 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"$periodKey-4-1"),
        new LocalDate(s"$periodKey-8-31"),  AtedConstants.LiabilityReturnType, None)

      val expected = List( LineItemValue(liabilityPeriod1.propertyValue, dateOfValuation))

      val valueTuples = PeriodUtils.getOrderedReturnPeriodValues(List(liabilityPeriod1), None)
      valueTuples.isEmpty must be (false)
      valueTuples must be (expected)
    }

    "return the single value and date when we have one line item with the date being the latest date before the first period" in {
      val liabilityPeriod1 = FormBundleProperty(BigDecimal(123.45),
        new LocalDate(s"$periodKey-4-1"), new LocalDate(s"$periodKey-8-31"),  AtedConstants.LiabilityReturnType, None)
      val valuationDate = new LocalDate(s"$periodKey-4-1")

      val expected = List( LineItemValue(liabilityPeriod1.propertyValue, valuationDate))

      val valueTuples = PeriodUtils.getOrderedReturnPeriodValues(List(liabilityPeriod1), Some(valuationDate))
      valueTuples.isEmpty must be (false)
      valueTuples must be (expected)
    }

    "return ordered value and dates when we have more than one line item each with a different value" in {
      val liabilityPeriod1 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"$periodKey-4-1"),
        new LocalDate(s"$periodKey-8-31"),  AtedConstants.LiabilityReturnType, None)
      val liabilityPeriod2 = FormBundleProperty(BigDecimal(456.45), new LocalDate(s"${periodKey + 1}-2-1"),
        new LocalDate(s"${periodKey + 1}-3-31"), AtedConstants.LiabilityReturnType, None)
      val reliefPeriod1 = FormBundleProperty(BigDecimal(789.45), new LocalDate(s"$periodKey-9-1"),
        new LocalDate(s"${periodKey + 1}-1-31"), AtedConstants.ReliefReturnType, Some("Property rental businesses"))
      val liabilityPeriods = List(liabilityPeriod2, liabilityPeriod1)
      val reliefPeriods = List(reliefPeriod1)

      val expected = List( LineItemValue(liabilityPeriod1.propertyValue, dateOfValuation),
        LineItemValue(reliefPeriod1.propertyValue, reliefPeriod1.dateFrom),
        LineItemValue(liabilityPeriod2.propertyValue, liabilityPeriod2.dateFrom)
      )
      val valueTuples = PeriodUtils.getOrderedReturnPeriodValues(liabilityPeriods ++ reliefPeriods, None)
      valueTuples.isEmpty must be (false)
      valueTuples must be (expected)
    }

    "only return the first date and value if all values match" in {
      val liabilityPeriod1 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"$periodKey-4-1"),
        new LocalDate(s"$periodKey-8-31"),  AtedConstants.LiabilityReturnType, None)
      val liabilityPeriod2 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"${periodKey + 1}-2-1"),
        new LocalDate(s"${periodKey + 1}-3-31"), AtedConstants.LiabilityReturnType, None)
      val reliefPeriod1 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"$periodKey-9-1"),
        new LocalDate(s"${periodKey + 1}-1-31"), AtedConstants.ReliefReturnType, Some("Property rental businesses"))
      val liabilityPeriods = List(liabilityPeriod2, liabilityPeriod1)
      val reliefPeriods = List(reliefPeriod1)

      val expected = List( LineItemValue(liabilityPeriod1.propertyValue, dateOfValuation))

      val valueTuples = PeriodUtils.getOrderedReturnPeriodValues(liabilityPeriods ++ reliefPeriods, None)
      valueTuples.isEmpty must be (false)
      valueTuples must be (expected)
    }


    "return two values if the value changed mid year, with the date being the date of the change" in {
      val liabilityPeriod1 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"$periodKey-4-1"),
        new LocalDate(s"$periodKey-8-31"),  AtedConstants.LiabilityReturnType, None)
      val liabilityPeriod2 = FormBundleProperty(BigDecimal(999.45), new LocalDate(s"${periodKey + 1}-2-1"),
        new LocalDate(s"${periodKey + 1}-3-31"), AtedConstants.LiabilityReturnType, None)
      val reliefPeriod1 = FormBundleProperty(BigDecimal(999.45), new LocalDate(s"$periodKey-9-1"),
        new LocalDate(s"${periodKey + 1}-1-31"), AtedConstants.ReliefReturnType, Some("Property rental businesses"))
      val liabilityPeriods = List(liabilityPeriod2, liabilityPeriod1)
      val reliefPeriods = List(reliefPeriod1)

      val expected = List(
        LineItemValue(liabilityPeriod1.propertyValue, dateOfValuation),
        LineItemValue(reliefPeriod1.propertyValue, reliefPeriod1.dateFrom)
      )

      val valueTuples = PeriodUtils.getOrderedReturnPeriodValues(liabilityPeriods ++ reliefPeriods, None)
      valueTuples.isEmpty must be (false)
      valueTuples must be (expected)
    }

    "return three values if the value changed mid year, then changed back" in {
      val liabilityPeriod1 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"$periodKey-4-1"),
        new LocalDate(s"$periodKey-8-31"),  AtedConstants.LiabilityReturnType, None)
      val liabilityPeriod2 = FormBundleProperty(BigDecimal(123.45), new LocalDate(s"${periodKey + 1}-2-1"),
        new LocalDate(s"${periodKey + 1}-3-31"), AtedConstants.LiabilityReturnType, None)
      val reliefPeriod1 = FormBundleProperty(BigDecimal(999.45), new LocalDate(s"$periodKey-9-1"),
        new LocalDate(s"${periodKey + 1}-1-31"), AtedConstants.ReliefReturnType, Some("Property rental businesses"))
      val liabilityPeriods = List(liabilityPeriod2, liabilityPeriod1)
      val reliefPeriods = List(reliefPeriod1)

      val expected = List( LineItemValue(liabilityPeriod1.propertyValue, dateOfValuation),
        LineItemValue(reliefPeriod1.propertyValue, reliefPeriod1.dateFrom),
        LineItemValue(liabilityPeriod2.propertyValue, liabilityPeriod2.dateFrom)
      )

      val valueTuples = PeriodUtils.getOrderedReturnPeriodValues(liabilityPeriods ++ reliefPeriods, None)
      valueTuples.isEmpty must be (false)
      valueTuples must be (expected)
    }
  }

  "getPeriodValueMessage" must {
    "correct when we only have one value" in {
      PeriodUtils.getPeriodValueMessage(0, 1) must be ("ated.form-bundle.view.return.value.only")
    }
    "correct when we have the first of multiple values" in {
      PeriodUtils.getPeriodValueMessage(0, 3) must be ("ated.form-bundle.view.return.value.initial")
    }
    "correct when we have another of multiple values" in {
      PeriodUtils.getPeriodValueMessage(1, 3) must be ("ated.form-bundle.view.return.value.changed")
    }
  }

  "getPeriodValueDateMessage" must {
    "correct when we only have one value" in {
      PeriodUtils.getPeriodValueDateMessage(0, 1) must be ("ated.form-bundle.view.return.date.valuation.only")
    }
    "correct when we have the first of multiple values" in {
      PeriodUtils.getPeriodValueDateMessage(0, 3) must be ("ated.form-bundle.view.return.date.valuation.initial")
    }
    "correct when we have another of multiple values" in {
      PeriodUtils.getPeriodValueDateMessage(1, 3) must be ("ated.form-bundle.view.return.date.valuation.changed")
    }
  }

  "getCalculated" must {
    "return an empty list if we have No Calculated value" in {
      PeriodUtils.getCalculatedPeriodValues(None).isEmpty must be (true)
    }

    "return an empty list if we have an empty Calculated Object" in {
      PeriodUtils.getCalculatedPeriodValues(Some(new PropertyDetailsCalculated())).isEmpty must be (true)
    }
  }

  "isBlank" must {
    "return true, if the string is blank" in {
      PeriodUtils.isBlank("") must be (true)
    }
    "return false, if the string is NOT blank" in {
      PeriodUtils.isBlank("hello") must be (false)
    }
  }

  "calculateLowerTaxYearBoundary" must {
    "return 2017, if periodKey is greater than or equal to 2018 or lesser than or equal 2023" in {
      PeriodUtils.calculateLowerTaxYearBoundary(`2018`).getYear.toString must be ("2017")
    }
    "return 2012, if periodKey is lesser than 2017" in {
      PeriodUtils.calculateLowerTaxYearBoundary(`2015`).getYear.toString must be ("2012")
    }
  }
}
