/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import utils.AtedConstants._


object PeriodUtils {
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toDate.getTime)

  def calculatePeriod(date: LocalDate = new LocalDate(), month:Int = 4): Int = {
    if (date.getMonthOfYear < month) date.minusYears(1).getYear
    else date.getYear
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


  lazy val liabilityReturnTypeDesc = Messages("ated.property-details-period.liability.return-type")
  lazy val disposeReturnTypeDesc = Messages("ated.property-details-period.dispose.return-type")

  def getDisplayPeriods(propertyDetails: Option[PropertyDetailsPeriod]) = {

    val liabilityPeriods = propertyDetails.map(_.liabilityPeriods).getOrElse(Nil)
    val reliefPeriods = propertyDetails.map(_.reliefPeriods).getOrElse(Nil)
    sortAndConvertLineItemsForDisplay(liabilityPeriods ++ reliefPeriods)
  }

  def getDisplayFormBundleProperties(lineItems: Seq[FormBundleProperty]) = {
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
  def getPeriods(startDate: LocalDate, endDate: LocalDate): List[(String,String)] = {
    val startYear = if (startDate.getMonthOfYear >= 4) startDate.getYear else startDate.getYear-1
    val endYear = if (endDate.getMonthOfYear >= 3) endDate.getYear else endDate.getYear-1

    (startYear to endYear toList).reverse.map(x => s"$x" -> s"$x to ${x + 1}")
  }

  def getCalculatedPeriodValues(calculated : Option[PropertyDetailsCalculated]) = {
    def convert(items: Seq[CalculatedPeriod]) : Seq[FormBundleProperty]= {
      items.map(item => FormBundleProperty(item.value, item.startDate, item.endDate, item.lineItemType, item.description))
    }
    (calculated, calculated.flatMap(_.acquistionDateToUse)) match {
      case (Some(x), Some(valuationDate)) => getOrderedReturnPeriodValues(convert(x.liabilityPeriods) ++ convert(x.reliefPeriods), None)
      case _ => Nil
    }
  }

  def getOrderedReturnPeriodValues(lineItems: Seq[FormBundleProperty], dateOfAcquisition : Option[LocalDate] = None) = {
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

  def getPeriodValueMessage(index: Int, size: Int) = {
    (index, size) match {
      case (_, 1) => Messages("ated.form-bundle.view.return.value.only")
      case (0, _) => Messages("ated.form-bundle.view.return.value.initial")
      case _ =>  Messages("ated.form-bundle.view.return.value.changed")
    }
  }

  def getPeriodValueDateMessage(index: Int, size: Int) = {
    (index, size) match {
      case (_, 1) => Messages("ated.form-bundle.view.return.date.valuation.only")
      case (0, _) => Messages("ated.form-bundle.view.return.date.valuation.initial")
      case _ =>  Messages("ated.form-bundle.view.return.date.valuation.changed")
    }
  }

  def isListEmpty[T](list: Seq[T]): Boolean = list == Nil || list.isEmpty

  def isBlank(str: String): Boolean = str.isEmpty

/* function needs to be updated after every five years according to busines logic*/
  def getValuationYear(periodKey : Int) = {
    periodKey match {
      case p if periodKey >= 2018 && periodKey <= 2023 => "2017"
      case p if periodKey <= 2017 => "2012"
      case _ => throw new RuntimeException("Incorrect period")
    }
  }
}
