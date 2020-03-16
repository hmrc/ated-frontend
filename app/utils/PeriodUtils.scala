/*
 * Copyright 2020 HM Revenue & Customs
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
import models._
import org.joda.time.LocalDate
import utils.AtedConstants._


object PeriodUtils {
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toDate.getTime)

  val lowestBound = 2012

  def calculatePeakStartYear(date: LocalDate = new LocalDate(), month: Int = 3)(implicit appConfig: ApplicationConfig): Int = {
    if(date.getMonthOfYear <= month && date.getDayOfMonth < appConfig.atedPeakStartDay.toInt) {
      date.minusYears(1).getYear
    } else {
      date.getYear
    }
  }

  def periodStartDate(periodKey: Int): LocalDate = new LocalDate(s"$periodKey-${AtedConstants.PeriodStartMonth}-${AtedConstants.PeriodStartDay}")

  def periodEndDate(periodKey: Int): LocalDate = periodStartDate(periodKey).plusYears(1).minusDays(1)

  def isPeriodTooEarly(periodKey: Int, periodDate: Option[LocalDate]): Boolean = periodDate match {
    case Some(x) => x.isBefore(PeriodUtils.periodStartDate(periodKey))
    case _ => false
  }

  def isPeriodTooEarlyBefore2012(periodDate: Option[LocalDate]): Boolean = periodDate match {
    case Some(x) => x.isBefore(new LocalDate("2012-04-01"))
    case _ => false
  }

  def isPeriodTooLate(periodKey: Int, periodDate: Option[LocalDate]): Boolean = periodDate match {
    case Some(x) => x.isAfter(PeriodUtils.periodEndDate(periodKey))
    case _ => false
  }

  def isAfterPresentDay(periodDate: Option[LocalDate]): Boolean = periodDate match {
    case Some(x) => x.isAfter(new LocalDate())
    case _ => false
  }


  lazy val liabilityReturnTypeDesc = "ated.property-details-period.liability.return-type"
  lazy val disposeReturnTypeDesc = "ated.property-details-period.dispose.return-type"

  def getDisplayPeriods(propertyDetails: Option[PropertyDetailsPeriod]): Seq[LineItem] = {

    val liabilityPeriods = propertyDetails.map(_.liabilityPeriods).getOrElse(Nil)
    val reliefPeriods = propertyDetails.map(_.reliefPeriods).getOrElse(Nil)
    sortAndConvertLineItemsForDisplay(liabilityPeriods ++ reliefPeriods)
  }

  def getDisplayFormBundleProperties(lineItems: Seq[FormBundleProperty]): Seq[LineItem] = {
    def mergeValueChanges(lineItems: Seq[FormBundleProperty]) = {
      val startingVal = List[FormBundleProperty]()
      lineItems.foldLeft(startingVal){
        (r,newPeriod) =>
          r.headOption match {
            case Some(old) if (old.reliefDescription == newPeriod.reliefDescription && old.`type` == newPeriod.`type`) => {
              val mergedPeriod = old.copy(dateTo = newPeriod.dateTo)
              mergedPeriod :: r.tail
            }
            case _ => newPeriod :: r
          }
      }
    }
    implicit val lineItemOrdering: Ordering[FormBundleProperty] = Ordering.by(_.dateFrom)
    val filteredFormBundle = mergeValueChanges(lineItems.sorted)
    sortAndConvertLineItemsForDisplay(filteredFormBundle.map(item => LineItem(item.`type`, item.dateFrom, item.dateTo, item.reliefDescription)))
  }

  private def sortAndConvertLineItemsForDisplay(lineItems: Seq[LineItem]) = {
    implicit val lineItemOrdering: Ordering[LineItem] = Ordering.by(_.startDate)

    lineItems.map{
      lineItem =>
        lineItem.lineItemType.toLowerCase match {
          case LiabilityReturnType => lineItem.copy( description =  Some(liabilityReturnTypeDesc))
          case DisposeReturnType => lineItem.copy( description =  Some(disposeReturnTypeDesc))
          case _ => lineItem.copy( description = lineItem.description.map(ReliefsUtils.convertETMPReliefNameForSingleRelief(_)))
        }
    }.sorted
  }


  // from 1st of march add the next period so they can start to submit draft periods
  def getPeriods(startDate: LocalDate, endDate: LocalDate, applicationConfig: ApplicationConfig): List[(String,String)] = {
    val startYear = if (startDate.getMonthOfYear >= 4) startDate.getYear else startDate.getYear-1
    val endYear = if ((endDate.getMonthOfYear >= 3 && endDate.getDayOfMonth >= applicationConfig.atedPeakStartDay.toInt) || endDate.getMonthOfYear > 3) endDate.getYear else endDate.getYear-1
    (startYear to endYear toList).reverse.map(x => s"$x" -> s"$x to ${x + 1}")
  }

  def getCalculatedPeriodValues(calculated : Option[PropertyDetailsCalculated]): Seq[LineItemValue] = {
    def convert(items: Seq[CalculatedPeriod]) : Seq[FormBundleProperty]= {
      items.map(item => FormBundleProperty(item.value, item.startDate, item.endDate, item.lineItemType, item.description))
    }
    (calculated, calculated.flatMap(_.acquistionDateToUse)) match {
      case (Some(x), Some(valuationDate)) => getOrderedReturnPeriodValues(convert(x.liabilityPeriods) ++ convert(x.reliefPeriods), None)
      case _ => Nil
    }
  }

  def getOrderedReturnPeriodValues(lineItems: Seq[FormBundleProperty], dateOfAcquisition : Option[LocalDate] = None): Seq[LineItemValue] = {
    implicit val lineItemOrdering: Ordering[FormBundleProperty] = Ordering.by(_.dateFrom)

    def filterReturnPeriodValues(lineItems: Seq[FormBundleProperty]) = {
      val startingVal = List[LineItemValue]()
      val filtered = lineItems.foldLeft(startingVal){
        (r,c) =>
          r.headOption match {
            case None => LineItemValue(c.propertyValue, c.dateFrom) :: r
            case Some(x) if (x.propertyValue != c.propertyValue) => LineItemValue(c.propertyValue, c.dateFrom) :: r
            case _ => r
          }
      }
      filtered.reverse
    }

    def updateStartDateOfFirstItem(first: LineItemValue): LineItemValue = {
      val minValuationDate = List(first.dateOfChange).min

      val maxDateBeforeFirstPeriod = dateOfAcquisition match {
        case Some(acquisition) => {
          val minAcquisitionDate = List(first.dateOfChange, acquisition).min
          List(minValuationDate, minAcquisitionDate).max
        }
        case None => minValuationDate
      }
      first.copy(dateOfChange = maxDateBeforeFirstPeriod)
    }


    val orderedResults = filterReturnPeriodValues(lineItems.sorted)
    orderedResults match {
      case head :: Nil => Seq(updateStartDateOfFirstItem(head))
      case head :: tail => updateStartDateOfFirstItem(head) :: tail
      case Nil => Nil
    }
  }

  def getPeriodValueMessage(index: Int, size: Int): String = {
    (index, size) match {
      case (_, 1) => "ated.form-bundle.view.return.value.only"
      case (0, _) => "ated.form-bundle.view.return.value.initial"
      case _ =>  "ated.form-bundle.view.return.value.changed"
    }
  }

  def getPeriodValueDateMessage(index: Int, size: Int): String = {
    (index, size) match {
      case (_, 1) => "ated.form-bundle.view.return.date.valuation.only"
      case (0, _) => "ated.form-bundle.view.return.date.valuation.initial"
      case _ =>  "ated.form-bundle.view.return.date.valuation.changed"
    }
  }

  def isListEmpty[T](list: Seq[T]): Boolean = list == Nil || list.isEmpty

  def isBlank(str: String): Boolean = str.isEmpty

  def calculateLowerTaxYearBoundary(periodKey: Int): LocalDate = {
    val year = if (periodKey <= lowestBound) lowestBound else {
      lowestBound + (5 * ((periodKey - lowestBound - 1) / 5).floor.toInt)
    }
    LocalDate.parse(s"$year-4-1")
  }
}
