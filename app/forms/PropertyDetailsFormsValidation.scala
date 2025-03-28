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

package forms

import models.LineItem
import java.time.LocalDate
import play.api.data.{Form, FormError}
import utils.PeriodUtils
import utils.PeriodUtils._

import scala.util.{Left, Try}


object PropertyDetailsFormsValidation {

  //scalastyle:off line.size.limit
  def validateAvoidanceSchemeRefNo(isTaxAvoidance: Option[Boolean], avoidanceSchemeNo: Option[String], promoterReference: Option[String]): Seq[Option[FormError]] = {
    def validateAvoidanceScheme(avoidanceSchemeNo: Option[String]): Seq[Option[FormError]] = {
      avoidanceSchemeNo.getOrElse("") match {
        case a if a.isEmpty => Seq(Some(FormError("taxAvoidanceScheme", "ated.property-details-period.taxAvoidanceScheme.error.empty")))
        case a if a.length != 8 => Seq(Some(FormError("taxAvoidanceScheme", "ated.property-details-period.taxAvoidanceScheme.error.wrong-length")))
        case a if Try(a.toInt).isFailure => Seq(Some(FormError("taxAvoidanceScheme", "ated.property-details-period.taxAvoidanceScheme.error.numbers")))
        case _ => Seq(None)
      }
    }

    def validatePromoterReference(promoterReference: Option[String]): Seq[Option[FormError]] = {
      promoterReference.getOrElse("") match {
        case a if a.isEmpty => Seq(Some(FormError("taxAvoidancePromoterReference", "ated.property-details-period.taxAvoidancePromoterReference.error.empty")))
        case a if a.length != 8 => Seq(Some(FormError("taxAvoidancePromoterReference", "ated.property-details-period.taxAvoidancePromoterReference.error.wrong-length")))
        case a if Try(a.toInt).isFailure => Seq(Some(FormError("taxAvoidancePromoterReference", "ated.property-details-period.taxAvoidancePromoterReference.error.numbers")))
        case _ => Seq(None)
      }
    }

    isTaxAvoidance match {
      case Some(isAvoidTax) if isAvoidTax => validateAvoidanceScheme(avoidanceSchemeNo) ++ validatePromoterReference(promoterReference)
      case _ => Seq(None)
    }
  }

  //scalastyle:off line.size.limit
   def validateAvoidanceSchemeRefNoNew(avoidanceSchemeNo: Option[String], promoterReference: Option[String]): Seq[Option[FormError]] = {
     def validateAvoidanceScheme(avoidanceSchemeNo: Option[String]): Seq[Option[FormError]] = {
       avoidanceSchemeNo.getOrElse("") match {
         case a if a.isEmpty => Seq(Some(FormError("taxAvoidanceScheme", "ated.property-details-period.taxAvoidanceScheme.error.empty")))
         case a if a.length != 8 => Seq(Some(FormError("taxAvoidanceScheme", "ated.property-details-period.taxAvoidanceScheme.error.wrong-length")))
         case a if Try(a.toInt).isFailure => Seq(Some(FormError("taxAvoidanceScheme", "ated.property-details-period.taxAvoidanceScheme.error.numbers")))
         case _ => Seq(None)
       }
     }

     def validatePromoterReference(promoterReference: Option[String]): Seq[Option[FormError]] = {
       promoterReference.getOrElse("") match {
         case a if a.isEmpty => Seq(Some(FormError("taxAvoidancePromoterReference", "ated.property-details-period.taxAvoidancePromoterReference.error.empty")))
         case a if a.length != 8 => Seq(Some(FormError("taxAvoidancePromoterReference", "ated.property-details-period.taxAvoidancePromoterReference.error.wrong-length")))
         case a if Try(a.toInt).isFailure => Seq(Some(FormError("taxAvoidancePromoterReference", "ated.property-details-period.taxAvoidancePromoterReference.error.numbers")))
         case _ => Seq(None)
       }
     }

     validateAvoidanceScheme(avoidanceSchemeNo) ++ validatePromoterReference(promoterReference)
   }

  def validateBuildDate(periodKey: Int, f: Form[_], isNewBuild: Option[Boolean]): Seq[Option[FormError]] = {
    if (isNewBuild == Some(true))
      validateDate(periodKey, f, "newBuildDate", mustBeInChargeablePeriod = true, isMandatory = true) ++
        validateDate(periodKey, f, "localAuthRegDate", mustBeInChargeablePeriod = true, isMandatory = true)
    else
      validateDate(periodKey, f, "notNewBuildDate", isMandatory = true)
  }

  def validatedFirstOccupiedDate(periodKey: Int, f: Form[_]): Seq[Option[FormError]] = {
    validateDate(periodKey, f, "dateFirstOccupied", mustBeInChargeablePeriod = true, isMandatory = true)
  }

  def validatedCouncilRegisteredDate(periodKey: Int, f: Form[_]): Seq[Option[FormError]] = {
    validateDate(periodKey, f, "dateCouncilRegistered", mustBeInChargeablePeriod = true, isMandatory = true)
  }

  def validatedWhenAcquiredDate(periodKey: Int, f: Form[_]): Seq[Option[FormError]] = {
    validateDate(periodKey, f, "acquiredDate", isMandatory = true, noDateTooEarly = true)
  }

  def validatedNewBuildDate(periodKey: Int, f: Form[_]): Seq[Option[FormError]] = {
    validateDate(periodKey, f, "newBuildOccupyDate", mustBeInChargeablePeriod = true, isMandatory = true) ++
      validateDate(periodKey, f, "newBuildRegisterDate", mustBeInChargeablePeriod = true, isMandatory = true)
  }

  def checkDate (periodKey: Int, isPropertyRevalued: Option[Boolean], date: Option[LocalDate], field: String): Seq[Option[FormError]] = {
    if (isPropertyRevalued.contains(true)) {
      if (date.isEmpty) {
        Seq(Some(FormError(field, s"ated.property-details-value.$field.error.empty")))
      } else if (date.isDefined && date.exists(_.isAfter(LocalDate.now()))) {
        Seq(Some(FormError(field, s"ated.property-details-value.$field.error.in-future")))
      } else if (date.isDefined && date.exists(a => PeriodUtils.isPeriodTooLate(periodKey, Some(a)))) {
        Seq(Some(FormError(field, s"ated.property-details-value.$field.error.too-late")))
      } else Seq(None)
    } else Seq(None)
  }

  def validateStartEndDates(messageStart: String, periodKey: Int, form: Form[_], datesToAvoidValidation : Seq[String] = Seq.empty): Seq[Option[FormError]] = {
    val startDate = if(!datesToAvoidValidation.contains("startDate")) {
      (formDate2Option("startDate", form): @unchecked) match {
        case Right(a) if dateFallsInCurrentPeriod(periodKey, Some(a)) => Seq(None)
        case Right(a) if isPeriodTooEarly(periodKey, Some(a)) => Seq(Some(FormError("startDate", s"$messageStart.startDate.error.too-early")))
        case Right(a) if isPeriodTooLate(periodKey, Some(a)) => Seq(Some(FormError("startDate", s"$messageStart.startDate.error.too-late")))
      }
    } else Seq()
    val endDate = if(datesToAvoidValidation.isEmpty) {
      ((formDate2Option("startDate", form), formDate2Option("endDate", form)): @unchecked) match {
        case (Right(sd), Right(ed)) if ed.isBefore(sd) && isPeriodTooEarly(periodKey, Some(ed)) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error.before-start-date-and-too-early")))
        case (Right(sd), Right(ed)) if ed.isBefore(sd) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error.before-start-date")))
        case (_, Right(ed)) if dateFallsInCurrentPeriod(periodKey, Some(ed)) => Seq(None)
        case (_, Right(ed)) if isPeriodTooEarly(periodKey, Some(ed)) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error.too-early")))
        case (_, Right(ed)) if isPeriodTooLate(periodKey, Some(ed)) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error.too-late")))
      }
    } else Seq()

    startDate ++ endDate
  }

  def validateDatesInExistingPeriod(messageStart: String, currentPeriods: List[LineItem], form: Form[_]): Seq[Option[FormError]] = {
    def checkDateInBetween(dateToCheck: LocalDate): Boolean = {
      currentPeriods.map(
        period =>
          dateToCheck.isAfter(period.startDate.minusDays(1)) && dateToCheck.isBefore(period.endDate.plusDays(1))
      ).find(_ == true).getOrElse(false)
    }

    def checkDatesEncompassPeriod(startDate: LocalDate, endDate: LocalDate): Boolean = {
      currentPeriods.map(
        period =>
          startDate.isBefore(period.startDate) && endDate.isAfter(period.endDate)
      ).find(_ == true).getOrElse(false)
    }

    val startDate = formDate2Option("startDate", form)
    val endDate = formDate2Option("endDate", form)

    (startDate, endDate) match {
      case (Right(a), _) if checkDateInBetween(a) => Seq(Some(FormError("startDate", s"$messageStart.overlap.error")))
      case (_, Right(a)) if checkDateInBetween(a) => Seq(Some(FormError("endDate", s"$messageStart.overlap.error")))
      case (Right(a), Right(b)) if checkDatesEncompassPeriod(a, b) => Seq(Some(FormError("startDate", s"$messageStart.overlap.error"))) ++ Seq(Some(FormError("endDate", s"$messageStart.overlap.error")))
      case _ => Seq(None)
    }
  }

  def validateDate(periodKey: Int, f: Form[_], dateField: String,
                   noDateTooEarly: Boolean = false,
                   mustBeInChargeablePeriod: Boolean = false,
                   isMandatory: Boolean = false): Seq[Option[FormError]] = {
    val date = formDate2Option(dateField, f)
    val valuationYear: LocalDate = PeriodUtils.calculateLowerTaxYearBoundary(periodKey)

    if (date.exists(a => a.isBefore(valuationYear) && !noDateTooEarly)) {
      Seq(Some(FormError(dateField, s"ated.property-details-value.$dateField.error.too-early")))
    } else if (date.exists(a => a.isAfter(LocalDate.now()))) {
      Seq(Some(FormError(dateField, s"ated.property-details-value.$dateField.error.too-late")))
    } else if (mustBeInChargeablePeriod && date.exists(a => PeriodUtils.isPeriodTooEarly(periodKey, Some(a)) ||
      PeriodUtils.isPeriodTooLate(periodKey, Some(a)))) {
      Seq(Some(FormError(dateField, s"ated.property-details-value.$dateField.error.not-in-period")))
    } else if (isMandatory && date.left.exists(a => a)) {
      Seq(Some(FormError(dateField, s"ated.property-details-value.$dateField.error.empty")))
    } else Seq(None)
  }

  private[forms] def formDate2Option[A](dateField: String, f: Form[A]): Either[Boolean, LocalDate] = {
    (f.data.getOrElse(s"$dateField.day", ""), f.data.getOrElse(s"$dateField.month", ""), f.data.getOrElse(s"$dateField.year", "")) match {
      case (day, month, year) if day != "" && month != "" && year != "" =>
        Right(LocalDate.of(year.trim.toInt, month.trim.toInt, day.trim.toInt))
      case (day, month, year) if day != "" || month != "" || year != "" =>
        Left(false) //FIXME using booleans to denote whether the field is empty or not. Need a better way
      case _ => Left(true)
    }
  }

  private def dateFallsInCurrentPeriod(periodKey: Int, date: Option[LocalDate]): Boolean = {
    date match {
      case Some(a) => !isPeriodTooEarly(periodKey, Some(a)) && !isPeriodTooLate(periodKey, Some(a))
      case None => true
    }
  }

}
