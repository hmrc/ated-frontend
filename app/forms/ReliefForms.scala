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

package forms

import models._
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationResult}
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
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
        Invalid(Messages("ated.claim-relief.avoidance-scheme.selected"), "isAvoidanceScheme")
      }
  })

  val reliefSelectedConstraint: Constraint[Reliefs] = Constraint("rentalBusiness")({
    model =>
      if (model.rentalBusiness || model.openToPublic
        || model.propertyDeveloper || model.propertyTrading || model.lending || model.employeeOccupation
        || model.farmHouses || model.socialHousing || model.equityRelease) {
        Valid
      } else {
        Invalid(Messages("ated.choose-reliefs.error"), "reliefs")
      }
  })

  val rentalBusinessDateConstraint: Constraint[Reliefs] = Constraint("rentalBusinessDate")({
    model => validatePeriodStartDate(model.periodKey, model.rentalBusiness, model.rentalBusinessDate, "rentalBusiness", "rentalBusinessDate")
  })
  val employeeOccupationDateConstraint: Constraint[Reliefs] = Constraint("employeeOccupationDate")({
    model => validatePeriodStartDate(model.periodKey, model.employeeOccupation, model.employeeOccupationDate, "employeeOccupation", "employeeOccupationDate")
  })
  val farmHousesDateConstraint: Constraint[Reliefs] = Constraint("farmHousesDate")({
    model => validatePeriodStartDate(model.periodKey, model.farmHouses, model.farmHousesDate, "farmHouses", "farmHousesDate")
  })
  val lendingDateConstraint: Constraint[Reliefs] = Constraint("lendingDate")({
    model => validatePeriodStartDate(model.periodKey, model.lending, model.lendingDate, "lending", "lendingDate")
  })
  val openToPublicDateConstraint: Constraint[Reliefs] = Constraint("openToPublicDate")({
    model => validatePeriodStartDate(model.periodKey, model.openToPublic, model.openToPublicDate, "openToPublic", "openToPublicDate")
  })
  val propertyDeveloperDateConstraint: Constraint[Reliefs] = Constraint("propertyDeveloperDate")({
    model => validatePeriodStartDate(model.periodKey, model.propertyDeveloper, model.propertyDeveloperDate, "propertyDeveloper", "propertyDeveloperDate")
  })
  val propertyTradingDateConstraint: Constraint[Reliefs] = Constraint("propertyTradingDate")({
    model => validatePeriodStartDate(model.periodKey, model.propertyTrading, model.propertyTradingDate, "propertyTrading", "propertyTradingDate")
  })
  val socialHousingDateConstraint: Constraint[Reliefs] = Constraint("socialHousingDate")({
    model => validatePeriodStartDate(model.periodKey, model.socialHousing, model.socialHousingDate, "socialHousing", "socialHousingDate")
  })
  val equityReleaseConstraint: Constraint[Reliefs] = Constraint("equityReleaseDate")({
    model => validatePeriodStartDate(model.periodKey, model.equityRelease, model.equityReleaseDate, "equityRelease", "equityReleaseDate")
  })
  def validatePeriodStartDate(periodKey: Int,
                              reliefSelected: Boolean,
                              startDate: Option[LocalDate],
                              reliefSelectedFieldName: String,
                              dateFieldName: String) : ValidationResult = {
    import PeriodUtils._

    reliefSelected match {
      case true if startDate.isEmpty => {
        Invalid(Messages("ated.choose-reliefs.error.date.mandatory",
          Messages(s"ated.choose-reliefs.$reliefSelectedFieldName").toLowerCase), dateFieldName)
      }
      case true if isPeriodTooEarly(periodKey, startDate) => {
        Invalid(Messages("ated.choose-reliefs.error.date.tooEarly",
          Messages(s"ated.choose-reliefs.$reliefSelectedFieldName").toLowerCase), dateFieldName)
      }
      case true if isPeriodTooLate(periodKey, startDate) => {
        Invalid(Messages("ated.choose-reliefs.error.date.tooLate",
          Messages(s"ated.choose-reliefs.$reliefSelectedFieldName").toLowerCase), dateFieldName)
      }
      case _ => Valid
    }
  }

  val reliefsForm = Form(mapping(
    "periodKey" -> number,
    "rentalBusiness" -> boolean,
    "rentalBusinessDate" -> dateTuple,
    "openToPublic" -> boolean,
    "openToPublicDate" -> dateTuple,
    "propertyDeveloper" -> boolean,
    "propertyDeveloperDate" -> dateTuple,
    "propertyTrading" -> boolean,
    "propertyTradingDate" -> dateTuple,
    "lending" -> boolean,
    "lendingDate" -> dateTuple,
    "employeeOccupation" -> boolean,
    "employeeOccupationDate" -> dateTuple,
    "farmHouses" -> boolean,
    "farmHousesDate" -> dateTuple,
    "socialHousing" -> boolean,
    "socialHousingDate" -> dateTuple,
    "equityRelease" -> boolean,
    "equityReleaseDate" -> dateTuple,
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
        case a if a.isEmpty => Seq(Some(FormError(avoidanceFieldName, Messages("ated.avoidance-schemes.scheme.empty"))))
        case a if a.length != 8 => Seq(Some(FormError(avoidanceFieldName, Messages("ated.avoidance-schemes.scheme.wrong-length"))))
        case a if Try(a.toInt).isFailure => Seq(Some(FormError(avoidanceFieldName, Messages("ated.avoidance-schemes.scheme.numeric-error"))))
        case _ => Seq(None)
      }
    }
    def validatePromoterReference(promoterFieldName: String): Seq[Option[FormError]] = {
      val promoterReference = f.data.get(promoterFieldName)
      promoterReference.getOrElse("") match {
        case a if a.isEmpty => Seq(Some(FormError(promoterFieldName, Messages("ated.avoidance-schemes.promoter.empty"))))
        case a if a.length != 8 => Seq(Some(FormError(promoterFieldName, Messages("ated.avoidance-schemes.promoter.wrong-length"))))
        case a if Try(a.toInt).isFailure => Seq(Some(FormError(promoterFieldName, Messages("ated.avoidance-schemes.promoter.numeric-error"))))
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

}
