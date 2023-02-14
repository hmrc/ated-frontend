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
import play.api.data.{Form, FormError, Mapping}
import utils.PeriodUtils._

import scala.annotation.tailrec
import scala.util.Try

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

  private def addErrorsToForm[A](form: Form[A], formErrors: Seq[FormError]): Form[A] = {
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

}
