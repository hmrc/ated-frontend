/*
 * Copyright 2021 HM Revenue & Customs
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
import org.joda.time.{DateTime, LocalDate}
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationResult}
import play.api.data.{Form, FormError, Mapping}
import uk.gov.hmrc.play.mappers.DateTuple._
import utils.PeriodUtils

import scala.annotation.tailrec
import scala.util.Try


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
        Invalid("ated.choose-reliefs.error", "reliefs")
      }
  })

  val rentalBusinessDateConstraint: Constraint[Reliefs] = Constraint("rentalBusinessDate")({
    model => validatePeriodStartDate(model.periodKey, model.rentalBusiness, model.rentalBusinessDate, "rentalBusinessDate")
  })
  val employeeOccupationDateConstraint: Constraint[Reliefs] = Constraint("employeeOccupationDate")({
    model => validatePeriodStartDate(model.periodKey, model.employeeOccupation, model.employeeOccupationDate, "employeeOccupationDate")
  })
  val farmHousesDateConstraint: Constraint[Reliefs] = Constraint("farmHousesDate")({
    model => validatePeriodStartDate(model.periodKey, model.farmHouses, model.farmHousesDate, "farmHousesDate")
  })
  val lendingDateConstraint: Constraint[Reliefs] = Constraint("lendingDate")({
    model => validatePeriodStartDate(model.periodKey, model.lending, model.lendingDate, "lendingDate")
  })
  val openToPublicDateConstraint: Constraint[Reliefs] = Constraint("openToPublicDate")({
    model => validatePeriodStartDate(model.periodKey, model.openToPublic, model.openToPublicDate, "openToPublicDate")
  })
  val propertyDeveloperDateConstraint: Constraint[Reliefs] = Constraint("propertyDeveloperDate")({
    model => validatePeriodStartDate(model.periodKey, model.propertyDeveloper, model.propertyDeveloperDate, "propertyDeveloperDate")
  })
  val propertyTradingDateConstraint: Constraint[Reliefs] = Constraint("propertyTradingDate")({
    model => validatePeriodStartDate(model.periodKey, model.propertyTrading, model.propertyTradingDate, "propertyTradingDate")
  })
  val socialHousingDateConstraint: Constraint[Reliefs] = Constraint("socialHousingDate")({
    model => validatePeriodStartDate(model.periodKey, model.socialHousing, model.socialHousingDate, "socialHousingDate")
  })
  val equityReleaseConstraint: Constraint[Reliefs] = Constraint("equityReleaseDate")({
    model => validatePeriodStartDate(model.periodKey, model.equityRelease, model.equityReleaseDate, "equityReleaseDate")
  })

  def validatePeriodStartDate(periodKey: Int,
                              reliefSelected: Boolean,
                              startDate: Option[LocalDate],
                              dateFieldName: String) : ValidationResult = {
    import PeriodUtils._

    reliefSelected match {
      case true if startDate.isEmpty =>
        Invalid(s"ated.choose-reliefs.error.date.mandatory.$dateFieldName",
          s"$dateFieldName")
      case true if isPeriodTooEarly(periodKey, startDate) =>
        Invalid(s"ated.choose-reliefs.error.date.chargePeriod.$dateFieldName",
          s"$dateFieldName")
      case true if isPeriodTooLate(periodKey, startDate) =>
        Invalid(s"ated.choose-reliefs.error.date.chargePeriod.$dateFieldName",
          s"$dateFieldName")
      case _ => Valid
    }
  }

  val reliefsForm = Form(mapping(
    "periodKey" -> number,
    "rentalBusiness" -> boolean,
    "rentalBusinessDate" -> DateTupleCustomErrorImpl("ated.choose-reliefs.error.date.mandatory.rentalBusinessDate").dateTuple,
    "openToPublic" -> boolean,
    "openToPublicDate" -> DateTupleCustomErrorImpl("ated.choose-reliefs.error.date.mandatory.openToPublicDate").dateTuple,
    "propertyDeveloper" -> boolean,
    "propertyDeveloperDate" -> DateTupleCustomErrorImpl("ated.choose-reliefs.error.date.mandatory.propertyDeveloperDate").dateTuple,
    "propertyTrading" -> boolean,
    "propertyTradingDate" -> DateTupleCustomErrorImpl("ated.choose-reliefs.error.date.mandatory.propertyTradingDate").dateTuple,
    "lending" -> boolean,
    "lendingDate" -> DateTupleCustomErrorImpl("ated.choose-reliefs.error.date.mandatory.lendingDate").dateTuple,
    "employeeOccupation" -> boolean,
    "employeeOccupationDate" -> DateTupleCustomErrorImpl("ated.choose-reliefs.error.date.mandatory.employeeOccupationDate").dateTuple,
    "farmHouses" -> boolean,
    "farmHousesDate" -> DateTupleCustomErrorImpl("ated.choose-reliefs.error.date.mandatory.farmHousesDate").dateTuple,
    "socialHousing" -> boolean,
    "socialHousingDate" -> DateTupleCustomErrorImpl("ated.choose-reliefs.error.date.mandatory.socialHousingDate").dateTuple,
    "equityRelease" -> boolean,
    "equityReleaseDate" -> DateTupleCustomErrorImpl("ated.choose-reliefs.error.date.mandatory.equityReleaseDate").dateTuple,
    "isAvoidanceScheme" -> optional(boolean)
  )
  (Reliefs.apply)(Reliefs.unapply)
    .verifying(reliefSelectedConstraint)
    .verifying(rentalBusinessDateConstraint)
    .verifying(employeeOccupationDateConstraint)
    .verifying(farmHousesDateConstraint)
    .verifying(lendingDateConstraint)
    .verifying(openToPublicDateConstraint)
    .verifying(propertyDeveloperDateConstraint)
    .verifying(propertyTradingDateConstraint)
    .verifying(socialHousingDateConstraint)
    .verifying(equityReleaseConstraint)

  )


  val isTaxAvoidanceForm = Form(mapping(

    "isAvoidanceScheme" -> optional(boolean)
  )
  (IsTaxAvoidance.apply)(IsTaxAvoidance.unapply)
    .verifying(avoidanceSchemeConstraint)
  )

  val taxAvoidanceForm = Form(mapping(
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
  def validateTaxAvoidance(f: Form[TaxAvoidance]): Form[TaxAvoidance] = {
    def validateAvoidanceScheme(avoidanceFieldName: String): Seq[Option[FormError]] = {
      val avoidanceSchemeNo = f.data.get(avoidanceFieldName)
      avoidanceSchemeNo.getOrElse("") match {
        case a if a.isEmpty => Seq(Some(FormError(avoidanceFieldName, "ated.avoidance-schemes.scheme.empty")))
        case a if a.length != 8 => Seq(Some(FormError(avoidanceFieldName, "ated.avoidance-schemes.scheme.wrong-length")))
        case a if Try(a.toInt).isFailure => Seq(Some(FormError(avoidanceFieldName, "ated.avoidance-schemes.scheme.numeric-error")))
        case _ => Seq(None)
      }
    }
    def validatePromoterReference(promoterFieldName: String): Seq[Option[FormError]] = {
      val promoterReference = f.data.get(promoterFieldName)
      promoterReference.getOrElse("") match {
        case a if a.isEmpty => Seq(Some(FormError(promoterFieldName, "ated.avoidance-schemes.promoter.empty")))
        case a if a.length != 8 => Seq(Some(FormError(promoterFieldName, "ated.avoidance-schemes.promoter.wrong-length")))
        case a if Try(a.toInt).isFailure => Seq(Some(FormError(promoterFieldName, "ated.avoidance-schemes.promoter.numeric-error")))
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
            validateAvoidance("employeeOccupationScheme", "employeeOccupationSchemePromoter") ++
            validateAvoidance("farmHousesScheme", "farmHousesSchemePromoter") ++
            validateAvoidance("lendingScheme", "lendingSchemePromoter") ++
            validateAvoidance("openToPublicScheme", "openToPublicSchemePromoter") ++
            validateAvoidance("propertyDeveloperScheme", "propertyDeveloperSchemePromoter") ++
            validateAvoidance("propertyTradingScheme", "propertyTradingSchemePromoter") ++
            validateAvoidance("socialHousingScheme", "socialHousingSchemePromoter") ++
            validateAvoidance("equityReleaseScheme", "equityReleaseSchemePromoter")

          addErrorsToForm(f, errors.flatten)
        case false => f.withError("empty", "") // message parameter doesn't matter as we get a message using the error key
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

  private def addErrorsToForm[A](form: Form[A], formErrors: Seq[FormError]): Form[A] = {
    @tailrec
    def y(f: Form[A], fe: Seq[FormError]): Form[A] = {
      if (fe.isEmpty) f
      else y(f.withError(fe.head), fe.tail)
    }
    y(form, formErrors)
  }

  case class DateTupleCustomErrorImpl(invalidDateErrorKey: String) extends DateTupleCustomError
  trait DateTupleCustomError {

    import uk.gov.hmrc.play.mappers.DateFields._

    val invalidDateErrorKey: String
    val dateTuple: Mapping[Option[LocalDate]] = dateTuple(validate = true)

    def mandatoryDateTuple(error: String): Mapping[LocalDate] =
      dateTuple.verifying(error, data => data.isDefined).transform(o => o.get, v => if (v == null) None else Some(v))

    def dateTuple(validate: Boolean = true) =
      tuple(
        year  -> optional(text),
        month -> optional(text),
        day   -> optional(text)
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
      ).transform(
        {
          case (Some(y), Some(m), Some(d)) =>
            try Some(new LocalDate(y.trim.toInt, m.trim.toInt, d.trim.toInt))
            catch {
              case e: Exception =>
                if (validate) {
                  throw e
                } else {
                  None
                }
            }
          case (a, b, c)                   => None
        },
        (date: Option[LocalDate]) =>
          date match {
            case Some(d) => (Some(d.getYear.toString), Some(d.getMonthOfYear.toString), Some(d.getDayOfMonth.toString))
            case _       => (None, None, None)
          }
      )

    def validDateTuple: Mapping[DateTime] = {

      def verifyDigits(triple: (String, String, String)) =
        triple._1.forall(_.isDigit) && triple._2.forall(_.isDigit) && triple._3.forall(_.isDigit)

      tuple(
        "year"  -> optional(text),
        "month" -> optional(text),
        "day"   -> optional(text)
      ).verifying("error.enter_a_date", x => x._1.isDefined && x._2.isDefined && x._3.isDefined)
        .transform[(String, String, String)](
          x => (x._1.get.trim, x._2.get.trim, x._3.get.trim),
          x => (Some(x._1), Some(x._2), Some(x._3))
        )
        .verifying("error.enter_numbers", verifyDigits _)
        .verifying(
          "error.enter_valid_date",
          x => !verifyDigits(x) || Try(new DateTime(x._1.toInt, x._2.toInt, x._3.toInt, 0, 0)).isSuccess
        )
        .transform[DateTime](
          x => new DateTime(x._1.toInt, x._2.toInt, x._3.toInt, 0, 0).withTimeAtStartOfDay,
          x => (x.getYear.toString, x.getMonthOfYear.toString, x.getDayOfMonth.toString)
        )
    }

  }

}
