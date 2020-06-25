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

package forms

import models.LineItem
import org.joda.time.LocalDate
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

  def validateBuildDate(periodKey: Int, f: Form[_], isNewBuild: Option[Boolean]): Seq[Option[FormError]] = {
    if (isNewBuild == Some(true))
      validateDate(periodKey, f, "newBuildDate", mustBeInChargeablePeriod = true, isMandatory = true) ++
        validateDate(periodKey, f, "localAuthRegDate", mustBeInChargeablePeriod = true, isMandatory = true)
    else
      validateDate(periodKey, f, "notNewBuildDate", isMandatory = true)
  }

  def validatedNewBuildDate(periodKey: Int, f: Form[_]): Seq[Option[FormError]] = {
    validateDate(periodKey, f, "newBuildOccupyDate", mustBeInChargeablePeriod = true, isMandatory = true) ++
      validateDate(periodKey, f, "newBuildRegisterDate", mustBeInChargeablePeriod = true, isMandatory = true)
  }

  def checkRevaluedDate(periodKey: Int, isPropertyRevalued: Option[Boolean], revaluedDate: Option[LocalDate]): Seq[Option[FormError]] = {
    if (isPropertyRevalued.contains(true)) {
      if (revaluedDate.isEmpty) {
        Seq(Some(FormError("revaluedDate", "ated.property-details-value.revaluedDate.error.empty")))
      } else if (revaluedDate.isDefined && revaluedDate.exists(_.isAfter(new LocalDate()))) {
        Seq(Some(FormError("revaluedDate", "ated.property-details-value.revaluedDate.error.in-future")))
      } else if (revaluedDate.isDefined && revaluedDate.exists(a => PeriodUtils.isPeriodTooLate(periodKey, Some(a)))) {
        Seq(Some(FormError("revaluedDate", "ated.property-details-value.revaluedDate.error.too-late")))
      } else Seq(None)
    } else Seq(None)
  }

  def checkPartAcqDispDate(periodKey: Int, isPropertyRevalued: Option[Boolean], partAcqDispDate: Option[LocalDate]): Seq[Option[FormError]] = {
    if (isPropertyRevalued.contains(true)) {
      if (partAcqDispDate.isEmpty) {
        Seq(Some(FormError("partAcqDispDate", "ated.property-details-value.partAcqDispDate.error.empty")))
      } else if (partAcqDispDate.isDefined && partAcqDispDate.exists(_.isAfter(new LocalDate()))) {
        Seq(Some(FormError("partAcqDispDate", "ated.property-details-value.partAcqDispDate.error.in-future")))
      } else if (partAcqDispDate.isDefined && partAcqDispDate.exists(a => PeriodUtils.isPeriodTooLate(periodKey, Some(a)))) {
        Seq(Some(FormError("partAcqDispDate", "ated.property-details-value.partAcqDispDate.error.too-late")))
      } else Seq(None)
    } else Seq(None)
  }

  def validateStartEndDates(messageStart: String, periodKey: Int, form: Form[_]): Seq[Option[FormError]] = {
    val startDate = formDate2Option("startDate", form) match {
      case Right(a) if dateFallsInCurrentPeriod(periodKey, Some(a)) => Seq(None)
      case Right(a) if isPeriodTooEarly(periodKey, Some(a)) => Seq(Some(FormError("startDate", s"$messageStart.startDate.error.too-early")))
      case Right(a) if isPeriodTooLate(periodKey, Some(a)) => Seq(Some(FormError("startDate", s"$messageStart.startDate.error.too-late")))
      case Right(a) => Seq(Some(FormError("startDate", s"$messageStart.startDate.error")))
      case Left(false) => Seq(Some(FormError("startDate", s"$messageStart.startDate.error.incomplete")))
      case Left(true) => Seq(Some(FormError("startDate", s"$messageStart.startDate.error.empty")))
    }
    val endDate = (formDate2Option("startDate", form), formDate2Option("endDate", form)) match {
      case (Right(sd), Right(ed)) if ed.isBefore(sd) && isPeriodTooEarly(periodKey, Some(ed)) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error.before-start-date-and-too-early")))
      case (Right(sd), Right(ed)) if ed.isBefore(sd) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error.before-start-date")))
      case (_, Right(ed)) if dateFallsInCurrentPeriod(periodKey, Some(ed)) => Seq(None)
      case (_, Right(ed)) if isPeriodTooEarly(periodKey, Some(ed)) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error.too-early")))
      case (_, Right(ed)) if isPeriodTooLate(periodKey, Some(ed)) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error.too-late")))
      case (_, Right(ed)) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error")))
      case (_, Left(false)) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error.incomplete")))
      case (_, Left(true)) => Seq(Some(FormError("endDate", s"$messageStart.endDate.error.empty")))
    }

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
    val valuationYear = PeriodUtils.calculateLowerTaxYearBoundary(periodKey)

    if (date.right.exists(a => new LocalDate(a).isBefore(new LocalDate(s"$valuationYear")) && !noDateTooEarly)) {
      Seq(Some(FormError(dateField, s"ated.property-details-value.$dateField.error.too-early")))
    } else if (date.right.exists(a => new LocalDate(a).isAfter(new LocalDate()))) {
      Seq(Some(FormError(dateField, s"ated.property-details-value.$dateField.error.too-late")))
    } else if (mustBeInChargeablePeriod && date.right.exists(a => PeriodUtils.isPeriodTooEarly(periodKey, Some(a)) ||
      PeriodUtils.isPeriodTooLate(periodKey, Some(a)))) {
      Seq(Some(FormError(dateField, s"ated.property-details-value.$dateField.error.not-in-period")))
    } else if (isMandatory && date.left.exists(a => a)) {
      Seq(Some(FormError(dateField, s"ated.property-details-value.$dateField.error.empty")))
    } else Seq(None)
  }

  private[forms] def formDate2Option[A](dateField: String, f: Form[A]): Either[Boolean, LocalDate] = {
    (f.data.getOrElse(s"$dateField.day", ""), f.data.getOrElse(s"$dateField.month", ""), f.data.getOrElse(s"$dateField.year", "")) match {
      case (day, month, year) if day != "" && month != "" && year != "" =>
        Right(new LocalDate(s"${year.trim.toInt}-${month.trim.toInt}-${day.trim.toInt}"))
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
