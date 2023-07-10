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

import models._
import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, FormBinding, FormError, Mapping}
import play.api.mvc.{AnyContent, Request}
import utils.PeriodUtils._

import java.io
import java.time.YearMonth
import scala.annotation.tailrec
import scala.collection.immutable
import scala.util.Try
import scala.util.matching.Regex

sealed trait DateError
case object EmptyDate extends DateError
case object InvalidDate extends DateError

object ReliefForms {

  val numRegex = """[0-9]{8}"""

  val avoidanceSchemeConstraint: Constraint[IsTaxAvoidance] = Constraint("isAvoidanceScheme")({
    model =>
      if (model.isAvoidanceScheme.isDefined ) {
        Valid
      } else {
        Invalid("ated.claim-relief.avoidance-scheme.selected", "isAvoidanceScheme")
      }
  })

  val reliefSelectedConstraint: Constraint[Reliefs] = Constraint("rentalBusiness")({
    model =>
      if (model.rentalBusiness || model.openToPublic
        || model.propertyDeveloper || model.propertyTrading || model.lending || model.employeeOccupation
        || model.farmHouses || model.socialHousing || model.equityRelease) {
        Valid
      } else {
        Invalid("ated.choose-reliefs.error", "rentalBusiness")
      }
  })

  val reliefsForm: Form[Reliefs] = Form(mapping(
    "periodKey" -> number,
    "rentalBusiness" -> boolean,
    "rentalBusinessDate" -> dateTuple(),
    "openToPublic" -> boolean,
    "openToPublicDate" -> dateTuple(),
    "propertyDeveloper" -> boolean,
    "propertyDeveloperDate" -> dateTuple(),
    "propertyTrading" -> boolean,
    "propertyTradingDate" -> dateTuple(),
    "lending" -> boolean,
    "lendingDate" -> dateTuple(),
    "employeeOccupation" -> boolean,
    "employeeOccupationDate" -> dateTuple(),
    "farmHouses" -> boolean,
    "farmHousesDate" -> dateTuple(),
    "socialHousing" -> boolean,
    "socialHousingDate" -> dateTuple(),
    "equityRelease" -> boolean,
    "equityReleaseDate" -> dateTuple(),
    "isAvoidanceScheme" -> optional(boolean)
  )
  (Reliefs.apply)(Reliefs.unapply)
    .verifying(reliefSelectedConstraint)
  )

    val fields = Seq(
      ("rentalBusiness", "rentalBusinessDate"),
      ("openToPublic", "openToPublicDate"),
      ("propertyDeveloper", "propertyDeveloperDate"),
      ("propertyTrading", "propertyTradingDate"),
      ("lending", "lendingDate"),
      ("employeeOccupation", "employeeOccupationDate"),
      ("farmHouses", "farmHousesDate"),
      ("socialHousing", "socialHousingDate"),
      ("equityRelease", "equityReleaseDate")
    )

  //scalastyle:off cyclomatic.complexity
  def validateForm(f: Form[Reliefs]): Form[Reliefs] = {
    val formErrors = {
      fields.map { x =>
        val reliefBool = f.data.get(x._1)
        val periodKey = f.data.get("periodKey").get.toInt
        val reliefDate: Either[DateError, LocalDate] = {
          (f.data.get(s"${x._2}.day"), f.data.get(s"${x._2}.month"), f.data.get(s"${x._2}.year")) match {
            case (Some(d), Some(m), Some(y)) if(d.isEmpty && m.isEmpty && y.isEmpty) => Left(EmptyDate)
            case (Some(d), Some(m), Some(y)) => try {
              Right(new LocalDate(y.trim.toInt, m.trim.toInt, d.trim.toInt))
            } catch {
              case _ : Throwable => Left(InvalidDate)
            }
            case (None, None, None) => Left(EmptyDate)
            case _ => Left(InvalidDate)
          }
        }
        reliefBool match {
          case Some("true") => {
            reliefDate match {
              // Keeping the empty and invalid cases separate as a reminder that we should differentiate these error messages
              case Left(EmptyDate) => Seq(FormError(s"${x._2}", s"ated.choose-reliefs.error.date.mandatory.${x._2}"))
              case Left(InvalidDate) => Seq(FormError(s"${x._2}", s"ated.choose-reliefs.error.date.mandatory.${x._2}"))
              case Right(date) if (isPeriodTooEarly(periodKey, Some(date)) || isPeriodTooLate(periodKey, Some(date))) => Seq(FormError(s"${x._2}", s"ated.choose-reliefs.error.date.chargePeriod.${x._2}"))
              case _ => Nil
            }
          }
          case _ => Nil
        }
      }
    }
    addErrorsToForm(f, formErrors.flatten)
  }

  val isTaxAvoidanceForm: Form[IsTaxAvoidance] = Form(mapping(

    "isAvoidanceScheme" -> optional(boolean)
  )
  (IsTaxAvoidance.apply)(IsTaxAvoidance.unapply)
    .verifying(avoidanceSchemeConstraint)
  )

  val taxAvoidanceForm: Form[TaxAvoidance] = Form(mapping(
    "rentalBusinessScheme" -> optional(text),
    "rentalBusinessSchemePromoter" -> optional(text),
    "openToPublicScheme" -> optional(text),
    "openToPublicSchemePromoter" -> optional(text),
    "propertyDeveloperScheme" -> optional(text),
    "propertyDeveloperSchemePromoter" -> optional(text),
    "propertyTradingScheme" -> optional(text),
    "propertyTradingSchemePromoter" -> optional(text),
    "lendingScheme" -> optional(text),
    "lendingSchemePromoter" -> optional(text),
    "employeeOccupationScheme" -> optional(text),
    "employeeOccupationSchemePromoter" -> optional(text),
    "farmHousesScheme" -> optional(text),
    "farmHousesSchemePromoter" -> optional(text),
    "socialHousingScheme" -> optional(text),
    "socialHousingSchemePromoter" -> optional(text),
    "equityReleaseScheme" -> optional(text),
    "equityReleaseSchemePromoter" -> optional(text)
  )
  (TaxAvoidance.apply)(TaxAvoidance.unapply)
  )

  //scalastyle:off cyclomatic.complexity
  def validateTaxAvoidance(f: Form[TaxAvoidance], periodKey: Int): Form[TaxAvoidance] = {
    def validateAvoidanceScheme(avoidanceFieldName: String): Seq[Option[FormError]] = {
      val messageKeySuffix =  if (periodKey >= 2020 && avoidanceFieldName == "socialHousingScheme") "providerSocialOrHousingScheme" else avoidanceFieldName
      val avoidanceSchemeNo = f.data.get(avoidanceFieldName)
      avoidanceSchemeNo.getOrElse("") match {
        case a if a.isEmpty => Seq(Some(FormError(avoidanceFieldName, s"ated.avoidance-scheme-error.general.empty.$messageKeySuffix")))
        case a if Try(a.toInt).isFailure => Seq(Some(FormError(avoidanceFieldName, s"ated.avoidance-scheme-error.general.numeric-error.$messageKeySuffix")))
        case a if a.length != 8 => Seq(Some(FormError(avoidanceFieldName, s"ated.avoidance-scheme-error.general.wrong-length.$messageKeySuffix")))
        case _ => Seq(None)
      }
    }
    def validatePromoterReference(promoterFieldName: String): Seq[Option[FormError]] = {
      val messageKeySuffix =  if (periodKey >= 2020 && promoterFieldName == "socialHousingSchemePromoter") "providerSocialOrHousingSchemePromoter" else promoterFieldName
      val promoterReference = f.data.get(promoterFieldName)
      promoterReference.getOrElse("") match {
        case a if a.isEmpty => Seq(Some(FormError(promoterFieldName, s"ated.avoidance-scheme-error.general.empty.$messageKeySuffix")))
        case a if Try(a.toInt).isFailure => Seq(Some(FormError(promoterFieldName, s"ated.avoidance-scheme-error.general.numeric-error.$messageKeySuffix")))
        case a if a.length != 8 => Seq(Some(FormError(promoterFieldName, s"ated.avoidance-scheme-error.general.wrong-length.$messageKeySuffix")))
        case _ => Seq(None)
      }
    }

    def validateAvoidance(avoidanceFieldName: String, promoterFieldName : String): Seq[Option[FormError]] = {
      val avoidanceValue = f.data.get(avoidanceFieldName)
      val promoterValue = f.data.get(promoterFieldName)

      if (!avoidanceValue.getOrElse("").trim.isEmpty || !promoterValue.getOrElse("").trim.isEmpty) {
        validateAvoidanceScheme(avoidanceFieldName) ++ validatePromoterReference(promoterFieldName)
      } else {
        Seq(None)
      }
    }

    if (!f.hasErrors) {
      validateTA(f.value.getOrElse(TaxAvoidance())) match {
        case true =>
          val errors =
            validateAvoidance("rentalBusinessScheme", "rentalBusinessSchemePromoter") ++
              validateAvoidance("openToPublicScheme", "openToPublicSchemePromoter") ++
              validateAvoidance("propertyDeveloperScheme", "propertyDeveloperSchemePromoter") ++
              validateAvoidance("propertyTradingScheme", "propertyTradingSchemePromoter") ++
              validateAvoidance("lendingScheme", "lendingSchemePromoter") ++
              validateAvoidance("employeeOccupationScheme", "employeeOccupationSchemePromoter") ++
              validateAvoidance("farmHousesScheme", "farmHousesSchemePromoter") ++
            validateAvoidance("socialHousingScheme", "socialHousingSchemePromoter") ++
            validateAvoidance("equityReleaseScheme", "equityReleaseSchemePromoter")

          addErrorsToForm(f, errors.flatten)
        case false => f.withError("", "ated.avoidance-schemes.scheme.empty") // message parameter doesn't matter as we get a message using the error key
      }
    } else f
  }

  private def validateTA(ta: TaxAvoidance): Boolean = {
    List(
      ta.employeeOccupationScheme, ta.employeeOccupationSchemePromoter,
      ta.farmHousesScheme, ta.farmHousesSchemePromoter,
      ta.lendingScheme, ta.lendingSchemePromoter,
      ta.openToPublicScheme, ta.openToPublicSchemePromoter,
      ta.propertyDeveloperScheme, ta.propertyDeveloperSchemePromoter,
      ta.propertyTradingScheme, ta.propertyTradingSchemePromoter,
      ta.rentalBusinessScheme, ta.rentalBusinessSchemePromoter,
      ta.socialHousingScheme, ta.socialHousingSchemePromoter,
      ta.equityReleaseScheme, ta.equityReleaseSchemePromoter
    ).flatten.nonEmpty
  }

  def addErrorsToForm[A](form: Form[A], formErrors: Seq[FormError]): Form[A] = {
    @tailrec
    def y(f: Form[A], fe: Seq[FormError]): Form[A] = {
      if (fe.isEmpty) f
      else y(f.withError(fe.head), fe.tail)
    }
    y(form, formErrors)
  }

  private def dateTuple(): Mapping[Option[LocalDate]] =
    tuple(
      "year"  -> optional(text),
      "month" -> optional(text),
      "day"   -> optional(text)
    ).transform(
      {
        case (Some(y), Some(m), Some(d)) =>
          try Some(new LocalDate(y.trim.toInt, m.trim.toInt, d.trim.toInt))
          catch {
            case e: Exception => None
          }
        case (a, b, c)                   => None
      },
      (date: Option[LocalDate]) =>
        date match {
          case Some(d) => (Some(d.getYear.toString), Some(d.getMonthOfYear.toString), Some(d.getDayOfMonth.toString))
          case _       => (None, None, None)
        }
    )

  def processRequestToDateFormData(dateFields: List[String], data: Map[String, Seq[String]]): Seq[(String, (Option[String], Option[String], Option[String]))] = {
    val map = data.foldLeft(Map.empty[String, String]) {
      case (s, (key, values)) =>
        if (key.endsWith("[]")) {
          val k = key.dropRight(2)
          s ++ values.zipWithIndex.map { case (v, i) => s"$k[$i]" -> v }
        } else {
          s + (key -> values.headOption.getOrElse(""))
        }
    }


    dateFields.map { field =>
      val dateFieldKeyDay = field + "." + "day"
      val dateFieldKeyMonth = field + "." + "month"
      val dateFieldKeyYear = field + "." + "year"

      val dateMappings = List(dateFieldKeyDay, dateFieldKeyMonth, dateFieldKeyYear).foldLeft[Map[String, String]](Map.empty[String, String])((agg, cur) =>
        map.get(cur) match {
          case Some(res) => agg + (cur -> res)
          case _ => agg
        }
      )

      field -> (dateMappings.get(dateFieldKeyDay), dateMappings.get(dateFieldKeyMonth), dateMappings.get(dateFieldKeyYear))
    }
  }

  val baseDateFormMapping: Mapping[Option[LocalDate]] = tuple(
    "year" -> optional(text),
    "month" -> optional(text),
    "day" -> optional(text)
  ).transform[Option[LocalDate]](
    {
      case (Some(y), Some(m), Some(d)) =>
        try Some(new LocalDate(y.trim.toInt, m.trim.toInt, d.trim.toInt))
        catch {
          case _: Exception => None
        }
      case (_, _, _) => None
    },
    {
      case Some(d) => (Some(d.getYear.toString), Some(d.getMonthOfYear.toString), Some(d.getDayOfMonth.toString))
      case _ => (None, None, None)
    }
  )

  def stripDuplicateDateFieldErrors[A](fieldValidation: Seq[(String, Either[List[String], LocalDate])],
                                       formWithError: Form[A]): Form[A] = {
    val dateFormErrors: Seq[String] = fieldValidation.flatMap { entry =>
      entry._2 match {
        case Left(_) => Some(entry._1)
        case _ => None
      }
    }
    val originalFormErrors: Seq[FormError] = formWithError.errors
    val editedFormErrors: Seq[FormError] = originalFormErrors.flatMap { formError =>
      if (dateFormErrors.contains(formError.key)) {
        None
      } else {
        Some(formError)
      }
    }

    formWithError.copy(errors = editedFormErrors)
  }

  def manageDateFormRequest(
                              dateFields: List[String],
                              emptyError: String,
                              dayError: String,
                              monthError: String,
                              yearError: String,
                              realDateError: String,
                              dateRangeError: Option[String] = None,
                              dateNotInFutureOf: Option[LocalDate] = None,
                              dateNotInPastOf: Option[LocalDate] = None
                           )(implicit request: Request[AnyContent], formBinding: FormBinding): (Seq[(String, Either[List[String], LocalDate])], Seq[FormError]) = {
    val data: Seq[(String, (Option[String], Option[String], Option[String]))] =
      processRequestToDateFormData(dateFields, formBinding.apply(request))

    val accufieldData: Seq[(String, Option[FormError], Either[List[String], LocalDate])] = data.map { fieldData =>
      val dateInputValidation: Either[List[String], LocalDate] = DateInputValidation().validateDate(fieldData._2)

      val validationErrors: Option[String] = dateInputValidation match {
        case Left(errors) => errors match {
          case Nil => Some(emptyError)
          case list if list.contains("day") => Some(dayError)
          case list if list.contains("month") => Some(monthError)
          case list if list.contains("year") => Some(yearError)
          case list if list.contains("invalid") => Some(realDateError)
        }
        case Right(date) =>
          (dateNotInPastOf, dateNotInFutureOf) match {
            case (Some(pastDate), _) => if (date.compareTo(pastDate) >= 1) {
              None
            } else {
              dateRangeError
            }
            case (_, Some(futureDate)) => if (date.compareTo(futureDate) <= 1) {
              None
            } else {
              dateRangeError
            }
            case _ => None
          }
      }

      val formWithErrors: Option[FormError] = validationErrors match {
        case Some(content) => Some(FormError(fieldData._1, content))
        case _ => None
      }

      (fieldData._1, formWithErrors, dateInputValidation)
    }

    val formErrors: Seq[FormError] = accufieldData.flatMap { data =>
      data._2
    }

    (accufieldData.map { fieldData =>
      (fieldData._1, fieldData._3)
    }, formErrors)
  }

  case class DateInputValidation(futureBound: Boolean = false) {

    val mapping: Mapping[(Option[String], Option[String], Option[String])] = tuple(
      "year" -> optional(text),
      "month" -> optional(text),
      "day" -> optional(text)
    )

    def getMapping(): Mapping[Option[LocalDate]] = {


      mapping.transform(
        {
          case (y, m, d) => validateDate((d, m, y)) match {
            case Left(_) => None
            case Right(value) => Some(value)
          }
        },
        {
          case Some(d) => (Some(d.getYear.toString), Some(d.getMonthOfYear.toString), Some(d.getDayOfMonth.toString))
          case _ => (None, None, None)
        }
      )
    }

    val maxDay = 31
    val maxMonth = 12
    val maxYear = 9999
    def asPositiveInt(value: String, max: Int = maxYear): Option[Int] = {
      matchedInt(value, "\\d{1,10}".r).flatMap(x => if (x < 0 || x > max) None else Some(x))
    }

    private def matchedInt(value: String, regex: Regex): Option[Int] =
      regex.findFirstIn(value.filterNot(_.equals(' '))).flatMap(a => Try(a.toInt).toOption)

    val knownLeapYear = 2000

    def validateDate(input: (Option[String], Option[String], Option[String])): Either[List[String], LocalDate] = {
      (input._1.flatMap(asPositiveInt(_, maxDay)), input._2.flatMap(asPositiveInt(_, maxMonth)), input._3.flatMap(asPositiveInt(_))) match {
        case (Some(dy), Some(mn), Some(yr)) if YearMonth.of(yr, mn).isValidDay(dy) =>
          Try(new LocalDate(yr, mn, dy)).toEither.left.map(_ => Nil)
        case (Some(_), Some(_), Some(_)) => Left(List("invalid"))
        case (Some(dy), Some(mn), None) if YearMonth.of(knownLeapYear, mn).isValidDay(dy) =>
          Left(List("year"))
        case (None, None, None) => Left(Nil)
        case (dy, mn, yr) =>
          Left(List(
            dy.fold[Option[String]](Some("day"))(_ => None),
            mn.fold[Option[String]](Some("month"))(_ => None),
            yr.fold[Option[String]](Some("year"))(_ => None)
          ).flatten)
      }
    }
  }

  case class DateTupleCustomErrorImpl(invalidDateErrorKey: String) extends DateTupleCustomError
  trait DateTupleCustomError {


    val invalidDateErrorKey: String
    val dateTuple: Mapping[Option[LocalDate]] = dateTuple()

    def mandatoryDateTuple(error: String): Mapping[LocalDate] =
      dateTuple.verifying(error, data => data.isDefined).transform(o => o.get, v => Option(v))

    def dateTuple(validate: Boolean = true): Mapping[Option[LocalDate]] = {
      val x = tuple(
        "year"  -> optional(text),
        "month" -> optional(text),
        "day"   -> optional(text)
      ).verifying(
        invalidDateErrorKey,
        data =>
          (data._1, data._2, data._3) match {
            case (None, None, None)                   => true
            case (yearOption, monthOption, dayOption) =>
              try {
                val y = yearOption.getOrElse(throw new Exception("Year missing")).trim
                if (y.length != 4) {
                  throw new Exception("Year must be 4 digits")
                }
                new LocalDate(
                  y.toInt,
                  monthOption.getOrElse(throw new Exception("Month missing")).trim.toInt,
                  dayOption.getOrElse(throw new Exception("Day missing")).trim.toInt
                )
                true
              } catch {
                case _: Throwable =>
                  if (validate) {
                    false
                  } else {
                    true
                  }
              }
          }
      )

        x.transform(
        {
          case (Some(y), Some(m), Some(d)) =>
            try Some(new LocalDate(y.trim.toInt, m.trim.toInt, d.trim.toInt))
            catch {
              case _: Exception => None
            }
          case (_, _, _) => None
        },
        {
          case Some(d) => (Some(d.getYear.toString), Some(d.getMonthOfYear.toString), Some(d.getDayOfMonth.toString))
          case _ => (None, None, None)
        }
      )
    }
  }
}
