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

import forms.AtedForms.validatePostCodeFormat
import forms.PropertyDetailsForms.PropertyValueField.isValid
import forms.mappings.DateTupleCustomError
import models._
import java.time.LocalDate
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, FormError, Mapping}
import utils.{AtedUtils, PeriodUtils}

import scala.annotation.tailrec
import scala.util.Try
import scala.util.matching.Regex

object PropertyDetailsForms {

  val ZERO = 0
  val ELEVEN = 11
  val SIXTY = 60
  val numRegex = """[0-9]{8}"""
  val addressLineLength = 35
  val emailLength = 241
  val lengthZero = 0
  val nameLength = 35
  val phoneLength = 30
  val faxLength = 30
  val businessNameLength = 105
  val titleNumberLength = 40
  val lanLength = 4
  val minimumPropertyValue = 500001L
  val maximumPropertyValue = 9999999999999L
  val emailRegex: Regex =
    """^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)
      |(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$""".r
  val supportingInfoRegex = """^[A-Za-z0-9\s\.\,\']*$"""
  val titleRegex = """^[A-Za-z0-9\s\.\,\']*$"""
  val supportingInfo = 200
  val PostcodeLength = 10

  val propertyDetailsAddressForm: Form[PropertyDetailsAddress] = Form(
    mapping(
      "line_1" -> text
        .verifying("ated.address.line-1", x => AtedForms.checkBlankFieldLength(x))
        .verifying("ated.error.address.line-1", x => x.isEmpty || (x.nonEmpty && x.length <= addressLineLength))
        .verifying("ated.error.address.line-1.format", x => AtedForms.validateAddressLine(Some(x))),
      "line_2" -> text
        .verifying("ated.address.line-2", x => AtedForms.checkBlankFieldLength(x))
        .verifying("ated.error.address.line-2", x => x.isEmpty || (x.nonEmpty && x.length <= addressLineLength))
        .verifying("ated.error.address.line-2.format", x => AtedForms.validateAddressLine(Some(x))),
      "line_3" -> optional(text)
        .verifying("ated.address.line-3", x => AtedForms.checkBlankFieldLength(x.toString))
        .verifying("ated.error.address.line-3", x => AtedForms.checkFieldLengthIfPopulated(x, addressLineLength))
        .verifying("ated.error.address.line-3.format", x => AtedForms.validateAddressLine(x)),
      "line_4" -> optional(text)
        .verifying("ated.address.line-4", x => AtedForms.checkBlankFieldLength(x.toString))
        .verifying("ated.error.address.line-4", x => AtedForms.checkFieldLengthIfPopulated(x, addressLineLength))
        .verifying("ated.error.address.line-4.format", x => AtedForms.validateAddressLine(x)),
      "postcode" -> optional(text)
        .verifying("ated.error.address.postalcode.format", x => validatePostCodeFormat(AtedUtils.formatPostCode(x)))
    )(PropertyDetailsAddress.apply)(PropertyDetailsAddress.unapply)
  )

  val propertyDetailsTitleForm: Form[PropertyDetailsTitle] = Form(
    mapping(
      "titleNumber" -> text
        .verifying("ated.error.titleNumber",
          x => x.isEmpty || (x.nonEmpty && x.replaceAll(" ", "").length <= titleNumberLength))
        .verifying("ated.error.titleNumber.invalid", x => x matches titleRegex)
    )(PropertyDetailsTitle.apply)(PropertyDetailsTitle.unapply)
  )

  val hasValueChangedForm: Form[HasValueChanged] = Form(
    mapping(
      "hasValueChanged" -> optional(boolean).verifying("ated.change-property-value.hasValueChanged.error.non-selected", a => a.isDefined)
    )(HasValueChanged.apply)(HasValueChanged.unapply)
  )

  val valueValidation: Mapping[Option[BigDecimal]] = propertyDetailsValueValidation

  val propertyDetailsAcquisitionForm: Form[PropertyDetailsAcquisition] = Form(
    mapping("anAcquisition" -> optional(boolean).verifying("ated.property-details-value.anAcquisition.error-field-name", x => x.isDefined)
    )(PropertyDetailsAcquisition.apply)(PropertyDetailsAcquisition.unapply))

  val propertyDetailsHasBeenRevaluedForm: Form[HasBeenRevalued] = Form(
    mapping(
      "isPropertyRevalued" -> optional(boolean).verifying("ated.property-details-value.isPropertyRevalued.error.non-selected", x => x.isDefined),
    )(HasBeenRevalued.apply)(HasBeenRevalued.unapply)
  )

  val propertyDetailsRevaluedForm: Form[PropertyDetailsRevalued] = Form(
    mapping(
      "isPropertyRevalued" -> optional(boolean).verifying("ated.property-details-value.isPropertyRevalued.error.non-selected", x => x.isDefined),
      "revaluedValue" -> valueValidation,
      "revaluedDate" -> DateTupleCustomError("ated.error.date.invalid").dateTupleOptional(),
      "partAcqDispDate" -> DateTupleCustomError("ated.error.date.invalid").dateTupleOptional()
    )(PropertyDetailsRevalued.apply)(PropertyDetailsRevalued.unapply))

  val propertyDetailsDateOfChangeForm: Form[DateOfChange] = Form (
    mapping(
      "dateOfChange" -> DateTupleCustomError("ated.error.date.invalid").dateTupleOptional()
    )(DateOfChange.apply)(DateOfChange.unapply)
  )

  val propertyDetailsNewValuationForm: Form[PropertyDetailsNewValuation] = Form(
    mapping(
      "revaluedValue" -> valueValidation.verifying(revaluedValueConstraint)
    )(PropertyDetailsNewValuation.apply)(PropertyDetailsNewValuation.unapply)
  )

  def OwnedBeforeYearConstraint(periodKey: Int): Constraint[Option[Boolean]] = Constraint({ model =>
    model match {
      case Some(_) => Valid
      case _ => Invalid("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", PeriodUtils.calculateLowerTaxYearBoundary(periodKey).getYear.toString)
    }
  })

  private def revaluedValueConstraint(): Constraint[Option[BigDecimal]] = Constraint({ model =>
    model match {
      case Some(v) => {
        if(v.toDouble >= maximumPropertyValue){
          Invalid("ated.property-details-value.revaluedValue.error.too-high")
        } else if(v.toDouble < minimumPropertyValue){
          Invalid("ated.property-details-value.revaluedValue.error.too-low")
        } else {
          Valid
        }
      }
      case _ => Invalid("ated.property-details-value.revaluedValue.error.empty")
    }
  })

  def propertyDetailsOwnedBeforeForm(periodKey: Int): Form[PropertyDetailsOwnedBefore] = Form(
    mapping(
      "isOwnedBeforePolicyYear" -> optional(boolean).verifying(OwnedBeforeYearConstraint(periodKey)),
      "ownedBeforePolicyYearValue" -> valueValidation
    )(PropertyDetailsOwnedBefore.apply)(PropertyDetailsOwnedBefore.unapply))

  val propertyDetailsProfessionallyValuedForm: Form[PropertyDetailsProfessionallyValued] = Form(
    mapping(
      "isValuedByAgent" -> optional(boolean).verifying("ated.property-details-value.isValuedByAgent.error.non-selected", x => x.isDefined)
    )(PropertyDetailsProfessionallyValued.apply)(PropertyDetailsProfessionallyValued.unapply))

  val propertyDetailsNewBuildForm: Form[PropertyDetailsNewBuild] = Form(
    mapping(
      "isNewBuild" -> optional(boolean).verifying("ated.property-details-value.isNewBuild.error.non-selected", x => x.isDefined)
    )(PropertyDetailsNewBuild.apply)(PropertyDetailsNewBuild.unapply)
  )

  val dateFirstOccupiedKnownForm: Form[DateFirstOccupiedKnown] = Form(
    mapping(
      "isDateFirstOccupiedKnown" -> optional(boolean).verifying("ated.property-details.first-occupied-known.non-selected", x => x.isDefined)
    )(DateFirstOccupiedKnown.apply)(DateFirstOccupiedKnown.unapply)
  )

  val dateCouncilRegisteredKnownForm: Form[DateCouncilRegisteredKnown] = Form(
    mapping(
      "isDateCouncilRegisteredKnown" -> optional(boolean).verifying("ated.property-details.council-registered-known.non-selected", x => x.isDefined)
    )(DateCouncilRegisteredKnown.apply)(DateCouncilRegisteredKnown.unapply)
  )

  val dateFirstOccupiedForm: Form[DateFirstOccupied] = Form(
    mapping(
      "dateFirstOccupied" -> DateTupleCustomError("ated.property-details.first-occupied-date.invalidInputType").dateTupleOptional()
        )(DateFirstOccupied.apply)(DateFirstOccupied.unapply)
  )

  val dateCouncilRegisteredForm: Form[DateCouncilRegistered] = Form(
    mapping(
      "dateCouncilRegistered" -> DateTupleCustomError("ated.property-details.council-registered-date.invalidInputType").dateTupleOptional()
    )(DateCouncilRegistered.apply)(DateCouncilRegistered.unapply)
  )

  val propertyDetailsNewBuildValueForm: Form[PropertyDetailsNewBuildValue] = Form(
    mapping(
      "newBuildValue" -> valueValidation.verifying("ated.property-details-value-error.newBuildValue.emptyValue", model => model.isDefined)
    )(PropertyDetailsNewBuildValue.apply)(PropertyDetailsNewBuildValue.unapply)
  )

  val propertyDetailsValueAcquiredForm: Form[PropertyDetailsValueOnAcquisition] = Form(
    mapping(
      "acquiredValue" -> valueValidation.verifying("ated.property-details-value-error.valueAcquired.emptyValue", model => model.isDefined )
    )(PropertyDetailsValueOnAcquisition.apply)(PropertyDetailsValueOnAcquisition.unapply)
  )

  val propertyDetailsWhenAcquiredDatesForm: Form[PropertyDetailsWhenAcquiredDates] = Form(
    mapping(
      "acquiredDate" -> DateTupleCustomError("ated.property-details.whenAcquired.invalidInputType").dateTupleOptional()
    )(PropertyDetailsWhenAcquiredDates.apply)(PropertyDetailsWhenAcquiredDates.unapply)
  )

  val isFullTaxPeriodForm: Form[PropertyDetailsFullTaxPeriod] = Form(
    mapping(
      "isFullPeriod" -> optional(boolean).verifying("ated.property-details-period.isFullPeriod.error-field-name", x => x.isDefined)
    )(PropertyDetailsFullTaxPeriod.apply)(PropertyDetailsFullTaxPeriod.unapply)
  )

  val periodsInAndOutReliefForm: Form[PropertyDetailsInRelief] = Form(
    mapping(
      "isInRelief" -> optional(boolean).verifying("ated.property-details-period.isInRelief.error-field-name", x => x.isDefined)
    )(PropertyDetailsInRelief.apply)(PropertyDetailsInRelief.unapply)
  )
  val periodDatesLiableForm: Form[PropertyDetailsDatesLiable] = Form(
    mapping(
      "startDate" -> DateTupleCustomError("error.invalid.date.format").dateTupleOptional(),
      "endDate" -> DateTupleCustomError("error.invalid.date.format").dateTupleOptional()
    )(PropertyDetailsDatesLiable.apply)(PropertyDetailsDatesLiable.unapply)
  )

  lazy val mandatoryRadio: Mapping[String] = optional(text)
    .verifying("ated.property-details-period.chooseRelief.error.non-selected", _.isDefined)
    .transform({ s: Option[String] => s.getOrElse("") }, { v: String => Some(v) })

  val periodChooseReliefForm: Form[PeriodChooseRelief] = Form(
    mapping(
      "reliefDescription" -> mandatoryRadio
    )(PeriodChooseRelief.apply)(PeriodChooseRelief.unapply)
  )

  val periodInReliefDatesForm: Form[PropertyDetailsDatesInRelief] = Form(
    mapping(
      "startDate" -> DateTupleCustomError("error.invalid.date.format").dateTupleOptional(),
      "endDate" -> DateTupleCustomError("error.invalid.date.format").dateTupleOptional(),
      "description" -> optional(text)
    )(PropertyDetailsDatesInRelief.apply)(PropertyDetailsDatesInRelief.unapply)
  )

  val propertyDetailsTaxAvoidanceForm: Form[PropertyDetailsTaxAvoidance] = Form(
    mapping(
      "isTaxAvoidance" -> optional(boolean).verifying("ated.property-details-period.isTaxAvoidance.error-field-name", x => x.isDefined),
      "taxAvoidanceScheme" -> optional(text),
      "taxAvoidancePromoterReference" -> optional(text)
    )(PropertyDetailsTaxAvoidance.apply)(PropertyDetailsTaxAvoidance.unapply)
  )

  val propertyDetailsSupportingInfoForm: Form[PropertyDetailsSupportingInfo] = Form(
    mapping(
      "supportingInfo" -> text
        .verifying("ated.property-details-period-error.supportingInfo", x => x.isEmpty || (x.nonEmpty && x.length <= supportingInfo))
        .verifying("ated.property-details-period-error.supportingInfo.invalid", x => x matches supportingInfoRegex)

    )(PropertyDetailsSupportingInfo.apply)(PropertyDetailsSupportingInfo.unapply)
  )

  def propertyDetailsValueValidation: Mapping[Option[BigDecimal]] = {
    import PropertyValueField._

    optional(text)
      .verifying("ated.property-details-value.incorrect-format", propertyValue => {
        propertyValue match {
          case Some(x) =>
            val noDecimals = x.split("\\.").head
            val strippedAndTrimmed = noDecimals.trim.replaceAll(" ", "").replace(",", "").replace(".", "")
            strippedAndTrimmed.matches("£?[0-9]*") && isValid(strippedAndTrimmed.replace("£", ""))
          case None => true
        }
      })
      .transform[Option[BigDecimal]](
        {
          case Some(a) =>
            val noDecimals = a.split("\\.").head
            val strippedAndTrimmed = noDecimals.trim.replaceAll(" ", "").replace(",", "").replace(".", "")
            if (strippedAndTrimmed.matches("£?[0-9]*") && isValid(strippedAndTrimmed.replace("£", "")))
              Some(BigDecimal(strippedAndTrimmed.replace("£", "")))
            else None
          case _ => None
        }, {
          case Some(a) => Some(a.toString)
          case _ => None
        }
      )
  }

  object PropertyValueField {
    def isValid(value: String): Boolean = Try(value.toLong).isSuccess
  }

  //scalastyle:off cyclomatic.complexity
  def validatePropertyDetailsRevalued(periodKey: Int, f: Form[PropertyDetailsRevalued]): Form[PropertyDetailsRevalued] = {
    if (!f.hasErrors) {
      val formErrors = (PropertyDetailsFormsValidation.checkPartAcqDispDate(periodKey, f.get.isPropertyRevalued, f.get.partAcqDispDate)
        ++ PropertyDetailsFormsValidation.checkRevaluedDate(periodKey, f.get.isPropertyRevalued, f.get.revaluedDate)
        ).flatten
      addErrorsToForm(f, formErrors)
    } else f
  }

  def validatePropertyDetailsTaxAvoidance(f: Form[PropertyDetailsTaxAvoidance]): Form[PropertyDetailsTaxAvoidance] = {
    if (!f.hasErrors) {
      val formErrors = PropertyDetailsFormsValidation.validateAvoidanceSchemeRefNo(f.get.isTaxAvoidance,
        f.get.taxAvoidanceScheme,
        f.get.taxAvoidancePromoterReference).flatten
      addErrorsToForm(f, formErrors)
    } else f
  }

  def validatePropertyDetailsOwnedBefore(f: Form[PropertyDetailsOwnedBefore]): Form[PropertyDetailsOwnedBefore] = {
    if (!f.hasErrors) {
      val formErrors = (validateValue(f.get.isOwnedBeforePolicyYear.contains(true), "ownedBeforePolicyYearValue", f.get.ownedBeforePolicyYearValue, f)).flatten
      addErrorsToForm(f, formErrors)
    } else f
  }

  def validatePropertyDetailsNewBuild(periodKey: Int, f: Form[PropertyDetailsNewBuild]): Form[PropertyDetailsNewBuild] = {
    if (!f.hasErrors) {
      val formErrors = PropertyDetailsFormsValidation.validateBuildDate(periodKey, f, f.get.isNewBuild).flatten
      addErrorsToForm(f, formErrors)
    } else f
  }

  def validateNewBuildFirstOccupiedDate(periodKey: Int, f: Form[DateFirstOccupied], dateFields: Seq[(String, String)]): Form[DateFirstOccupied] = {

    val dateValidationErrors =
      if (!f.hasErrors) {
        dateFields.map { x =>
          DateTupleCustomError.validateDateFields(f.data.get(s"${x._1}.day"), f.data.get(s"${x._1}.month"), f.data.get(s"${x._1}.year"),
            Seq((x._1, x._2)), dateForFutureValidation = Some(LocalDate.now()))
        }
      } else {
        Seq()
      }

    val preValidatedForm = addErrorsToForm(f, dateValidationErrors.flatten)

    if (!preValidatedForm.hasErrors) {
      val formErrors = PropertyDetailsFormsValidation.validatedFirstOccupiedDate(periodKey, f).flatten
      addErrorsToForm(f, formErrors)
    } else preValidatedForm

  }

  def validateWhenAcquiredDate(periodKey: Int, f: Form[PropertyDetailsWhenAcquiredDates], dateFields: Seq[(String, String)]): Form[PropertyDetailsWhenAcquiredDates] = {

    val dateValidationErrors =
      if (!f.hasErrors) {
        dateFields.map { x =>
          DateTupleCustomError.validateDateFields(f.data.get(s"${x._1}.day"), f.data.get(s"${x._1}.month"), f.data.get(s"${x._1}.year"),
            Seq((x._1, x._2)))
        }
      } else {
        Seq()
      }

    val preValidatedForm = addErrorsToForm(f, dateValidationErrors.flatten)

    if (!preValidatedForm.hasErrors) {
      val formErrors = PropertyDetailsFormsValidation.validatedWhenAcquiredDate(periodKey, f).flatten
      addErrorsToForm(f, formErrors)
    } else preValidatedForm

  }

  def validateDateOfChange(periodKey: Int, f: Form[DateOfChange], dateFields: (String, String)): Form[DateOfChange] = {

    val dateValidationErrors =
      if (!f.hasErrors) {
        DateTupleCustomError.validateDateFields(f.data.get(s"${dateFields._1}.day"), f.data.get(s"${dateFields._1}.month"), f.data.get(s"${dateFields._1}.year"),
          Seq((dateFields._1, dateFields._2)))
      } else {
        Seq()
      }

    val preValidatedForm = addErrorsToForm(f, dateValidationErrors)

    if (!preValidatedForm.hasErrors) {
      val formErrors = PropertyDetailsFormsValidation.checkDate(periodKey, Some(true), f.get.dateOfChange, dateFields._1).flatten
      addErrorsToForm(f, formErrors)
    } else preValidatedForm

  }

  def validateNewBuildCouncilRegisteredDate(periodKey: Int, f: Form[DateCouncilRegistered], dateFields: Seq[(String, String)]): Form[DateCouncilRegistered] = {
    val dateValidationErrors =
      if (!f.hasErrors) {
        dateFields.map { x =>
          DateTupleCustomError.validateDateFields(f.data.get(s"${x._1}.day"), f.data.get(s"${x._1}.month"), f.data.get(s"${x._1}.year"),
            Seq((x._1, x._2)), dateForFutureValidation = Some(LocalDate.now()))
        }
      } else {
        Seq()
      }

    val preValidatedForm = addErrorsToForm(f, dateValidationErrors.flatten)

    if (!preValidatedForm.hasErrors) {
      val formErrors = PropertyDetailsFormsValidation.validatedCouncilRegisteredDate(periodKey, f).flatten
      addErrorsToForm(f, formErrors)
    }
    else preValidatedForm
  }

  def validatePropertyDetailsNewBuildDates(periodKey: Int, f: Form[PropertyDetailsNewBuildDates]): Form[PropertyDetailsNewBuildDates] = {
    if (!f.hasErrors) {
      val formErrors = PropertyDetailsFormsValidation.validatedNewBuildDate(periodKey, f).flatten
      addErrorsToForm(f, formErrors)
    } else f
  }

  def validatePropertyDetailsDatesLiable(periodKey: Int, f: Form[PropertyDetailsDatesLiable], periodsCheck: Boolean, currentPeriods: List[LineItem] = Nil, dateFields: Seq[(String, String)]): Form[PropertyDetailsDatesLiable] = {
    val dateValidationErrors =
      if (!f.hasErrors) {
        dateFields.map { x =>
          DateTupleCustomError.validateDateFields(f.data.get(s"${x._1}.day"), f.data.get(s"${x._1}.month"), f.data.get(s"${x._1}.year"),
            Seq((x._1, x._2)))
        }
      } else {
        Seq()
      }

    val preValidatedForm = addErrorsToForm(f, dateValidationErrors.flatten)

    val datesToAvoidValidation = dateValidationErrors.flatten.toList.map(x => x.key match {
      case dateKey if dateKey.startsWith("startDate") => "startDate"
      case dateKey if dateKey.startsWith("endDate") => "endDate"
      case _ => ""
    })

    val basicErrorForm = if (!preValidatedForm.hasErrors || (preValidatedForm.hasErrors && datesToAvoidValidation.nonEmpty)) {
      val formErrors = (PropertyDetailsFormsValidation.validateStartEndDates("ated.property-details-period.datesLiable", periodKey, f, datesToAvoidValidation)).flatten
      addErrorsToForm(f, formErrors ++ preValidatedForm.errors)
    } else preValidatedForm

    if (!basicErrorForm.hasErrors && periodsCheck) {
      val formErrors = (PropertyDetailsFormsValidation.validateDatesInExistingPeriod("ated.property-details-period.datesLiable", currentPeriods, f)).flatten
      addErrorsToForm(basicErrorForm, formErrors)
    } else basicErrorForm
  }

  def validatePropertyDetailsDatesInRelief(periodKey: Int, f: Form[PropertyDetailsDatesInRelief], currentPeriods: List[LineItem],
                                           dateFields: Seq[(String, String)]): Form[PropertyDetailsDatesInRelief] = {

    val dateValidationErrors =
      if (!f.hasErrors) {
        dateFields.map { x =>
          DateTupleCustomError.validateDateFields(f.data.get(s"${x._1}.day"), f.data.get(s"${x._1}.month"), f.data.get(s"${x._1}.year"),
            Seq((x._1, x._2)))
        }
      } else {
        Seq()
      }

    val preValidatedForm = addErrorsToForm(f, dateValidationErrors.flatten)

    val basicErrorForm = if (!preValidatedForm.hasErrors) {
      val formErrors = (PropertyDetailsFormsValidation.validateStartEndDates("ated.property-details-period.datesInRelief", periodKey, f)).flatten
      addErrorsToForm(f, formErrors)
    } else preValidatedForm

    if (!basicErrorForm.hasErrors) {
      val formErrors = (PropertyDetailsFormsValidation.validateDatesInExistingPeriod("ated.property-details-period.datesInRelief", currentPeriods, f)).flatten
      addErrorsToForm(basicErrorForm, formErrors)
    } else basicErrorForm
  }

  private def validateValue(requiresValidation: Boolean, fieldName: String, fieldValue: Option[BigDecimal], f: Form[_]): Seq[Option[FormError]] = {
    if (requiresValidation) {
      if (f.data.contains(fieldName) && !f.data.get(fieldName).contains("")) {
        if (fieldValue.exists(a => a.toDouble >= maximumPropertyValue)) {
          Seq(Some(FormError(fieldName, s"ated.property-details-value.$fieldName.error.too-high")))
        } else if (fieldValue.exists(a => a.toDouble < minimumPropertyValue)) {
          Seq(Some(FormError(fieldName, s"ated.property-details-value.$fieldName.error.too-low")))
        } else Seq(None)
      } else Seq(Some(FormError(fieldName, s"ated.property-details-value.$fieldName.error.empty")))
    } else Seq(None)
  }

  private def addErrorsToForm[A](form: Form[A], formErrors: Seq[FormError]): Form[A] = {
    @tailrec
    def y(f: Form[A], fe: Seq[FormError]): Form[A] = {
      if (fe.isEmpty) f
      else y(f.withError(fe.head), fe.tail)
    }

    y(form, formErrors)
  }

  //scalastyle:off cyclomatic.complexity
  def validatePropertyDetailsRevaluedForm(periodKey : Int, f: Form[PropertyDetailsRevalued], dateFields : Seq[(String, String)] ): Form[PropertyDetailsRevalued] = {
    if (!f.hasErrors) {
      val formErrors =
        if (f.get.isPropertyRevalued.contains(true)) {
          dateFields.map { x =>
            DateTupleCustomError.validateDateFields(f.data.get(s"${x._1}.day"), f.data.get(s"${x._1}.month"), f.data.get(s"${x._1}.year"),
              Seq((x._1, x._2)))
          }
        } else {
          Seq()
        }
      val validationValueErrors = validateValue(f.get.isPropertyRevalued.contains(true), "revaluedValue", f.get.revaluedValue, f)
      if (f.get.isPropertyRevalued.contains(true)) {
        validatePropertyDetailsRevalued(periodKey, addErrorsToForm(f, formErrors(0) ++ validationValueErrors.flatten ++ formErrors(1)))
      } else {
        validatePropertyDetailsRevalued(periodKey, addErrorsToForm(f, validationValueErrors.flatten))
      }
    } else {
      f
    }
  }




}
