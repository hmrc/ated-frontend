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

package forms

import models._
import play.api.data.{Form, FormError, Mapping}
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.AtedUtils
import uk.gov.hmrc.play.mappers.DateTuple._

import scala.annotation.tailrec
import scala.util.Try
import forms.AtedForms.validatePostCodeFormat

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
  val emailRegex =
    """^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)
      |(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$""".r
  val supportingInfoRegex = """^[A-Za-z0-9\s\.\,\']*$"""
  val titleRegex = """^[A-Za-z0-9\s\.\,\']*$"""
  val supportingInfo = 200
  val PostcodeLength = 10

  val propertyDetailsAddressForm = Form(
    mapping(
      "line_1" -> text
        .verifying(Messages("ated.error.mandatory", Messages("ated.address.line-1")), x => AtedForms.checkBlankFieldLength(x))
        .verifying(Messages("ated.error.address.line-1", Messages("ated.address.line-1")),
          x => x.isEmpty || (x.nonEmpty && x.length <= addressLineLength))
        .verifying(Messages("ated.error.address.line-1.format", Messages("ated.address.line-1")), x => AtedForms.validateAddressLine(Some(x))),
      "line_2" -> text
        .verifying(Messages("ated.error.mandatory", Messages("ated.address.line-2")), x => AtedForms.checkBlankFieldLength(x))
        .verifying(Messages("ated.error.address.line-2", Messages("ated.address.line-2")),
          x => x.isEmpty || (x.nonEmpty && x.length <= addressLineLength))
        .verifying(Messages("ated.error.address.line-2.format", Messages("ated.address.line-2")), x => AtedForms.validateAddressLine(Some(x))),
      "line_3" -> optional(text)
        .verifying(Messages("ated.error.address.line-3", Messages("ated.address.line-3")),
          x => AtedForms.checkFieldLengthIfPopulated(x, addressLineLength))
        .verifying(Messages("ated.error.address.line-3.format", Messages("ated.address.line-3")), x => AtedForms.validateAddressLine(x)),
      "line_4" -> optional(text)
        .verifying(Messages("ated.error.address.line-4", Messages("ated.address.line-4")),
          x => AtedForms.checkFieldLengthIfPopulated(x, addressLineLength))
        .verifying(Messages("ated.error.address.line-4.format", Messages("ated.address.line-4")), x => AtedForms.validateAddressLine(x)),
      "postcode" -> optional(text)
        .verifying(Messages("ated.error.address.postalcode.format", Messages("ated.address.postcode.field"), PostcodeLength),
          x => validatePostCodeFormat(AtedUtils.formatPostCode(x)))
    )(PropertyDetailsAddress.apply)(PropertyDetailsAddress.unapply)
  )

  val propertyDetailsTitleForm = Form(
    mapping(
      "titleNumber" -> text
        .verifying(Messages("ated.error.titleNumber", Messages("ated.error.titleNumber"), titleNumberLength),
          x => x.isEmpty || (x.nonEmpty && x.replaceAll(" ", "").length <= titleNumberLength) )
        .verifying(Messages("ated.error.titleNumber.invalid", Messages("ated.error.titleNumber")), x => x matches titleRegex)
    )(PropertyDetailsTitle.apply)(PropertyDetailsTitle.unapply)
  )

  val hasValueChangedForm = Form(
    mapping(
      "hasValueChanged" -> optional(boolean).verifying(Messages("ated.change-property-value.hasValueChanged.error.non-selected"), a => a.isDefined)
    )(HasValueChanged.apply)(HasValueChanged.unapply)
  )

  val valueValidation: Mapping[Option[BigDecimal]] = propertyDetailsValueValidation

  val propertyDetailsAcquisitionForm = Form(
    mapping("anAcquisition" -> optional(boolean).verifying(Messages("ated.property-details-value.anAcquisition.error-field-name"), x => x.isDefined)
    )(PropertyDetailsAcquisition.apply)(PropertyDetailsAcquisition.unapply))

  val propertyDetailsRevaluedForm = Form(
    mapping(
      "isPropertyRevalued" -> optional(boolean).verifying(Messages("ated.property-details-value.isPropertyRevalued.error.non-selected"), x => x.isDefined),
      "revaluedValue" -> valueValidation,
      "revaluedDate" -> dateTuple,
      "partAcqDispDate" -> dateTuple
    )(PropertyDetailsRevalued.apply)(PropertyDetailsRevalued.unapply))

  val propertyDetailsOwnedBeforeForm = Form(
    mapping(
      "isOwnedBefore2012" -> optional(boolean).verifying(Messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected"), x => x.isDefined),
      "ownedBefore2012Value" -> valueValidation
    )(PropertyDetailsOwnedBefore.apply)(PropertyDetailsOwnedBefore.unapply))

  val propertyDetailsProfessionallyValuedForm = Form(
    mapping(
      "isValuedByAgent" -> optional(boolean).verifying(Messages("ated.property-details-value.isValuedByAgent.error.non-selected"), x => x.isDefined)
    )(PropertyDetailsProfessionallyValued.apply)(PropertyDetailsProfessionallyValued.unapply))

  val propertyDetailsNewBuildForm = Form(
    mapping(
      "isNewBuild" -> optional(boolean).verifying(Messages("ated.property-details-value.isNewBuild.error.non-selected"), x => x.isDefined),
      "newBuildValue" -> valueValidation,
      "newBuildDate" -> dateTuple,
      "localAuthRegDate" -> dateTuple,
      "notNewBuildValue" -> valueValidation,
      "notNewBuildDate" -> dateTuple
    )(PropertyDetailsNewBuild.apply)(PropertyDetailsNewBuild.unapply)
  )

  val isFullTaxPeriodForm = Form(
    mapping(
      "isFullPeriod" -> optional(boolean).verifying(Messages("ated.property-details-period.isFullPeriod.error-field-name"), x => x.isDefined)
    )(PropertyDetailsFullTaxPeriod.apply)(PropertyDetailsFullTaxPeriod.unapply)
  )

  val periodsInAndOutReliefForm = Form(
    mapping(
      "isInRelief" -> optional(boolean).verifying(Messages("ated.property-details-period.isInRelief.error-field-name"), x => x.isDefined)
    )(PropertyDetailsInRelief.apply)(PropertyDetailsInRelief.unapply)
  )
  val periodDatesLiableForm = Form(
    mapping(
      "startDate" -> mandatoryDateTuple("ated.property-details-period.datesLiable.startDate.error.empty"),
      "endDate" -> mandatoryDateTuple("ated.property-details-period.datesLiable.endDate.error.empty")
    )(PropertyDetailsDatesLiable.apply)(PropertyDetailsDatesLiable.unapply)
  )


  lazy val mandatoryRadio = optional(text)
    .verifying("ated.property-details-period.chooseRelief.error.non-selected", _.isDefined)
    .transform({ s: Option[String] => s.getOrElse("") }, { v: String => Some(v) })

  val periodChooseReliefForm = Form(
    mapping(
      "reliefDescription" -> mandatoryRadio
    )(PeriodChooseRelief.apply)(PeriodChooseRelief.unapply)
  )

  val periodInReliefDatesForm = Form(
    mapping(
      "startDate" -> mandatoryDateTuple("ated.property-details-period.datesInRelief.startDate.error.empty"),
      "endDate" -> mandatoryDateTuple("ated.property-details-period.datesInRelief.endDate.error.empty"),
      "description" -> optional(text)
    )(PropertyDetailsDatesInRelief.apply)(PropertyDetailsDatesInRelief.unapply)
  )

  val propertyDetailsTaxAvoidanceForm = Form(
    mapping(
      "isTaxAvoidance" -> optional(boolean).verifying(Messages("ated.property-details-period.isTaxAvoidance.error-field-name"), x => x.isDefined),
      "taxAvoidanceScheme" -> optional(text),
      "taxAvoidancePromoterReference" -> optional(text)
    )(PropertyDetailsTaxAvoidance.apply)(PropertyDetailsTaxAvoidance.unapply)
  )

  val propertyDetailsSupportingInfoForm = Form(
    mapping(
      "supportingInfo" -> text
        .verifying(Messages("ated.property-details-period-error.supportingInfo", Messages("ated.property-details-period.supportingInfo"), supportingInfo), x => x.isEmpty || (x.nonEmpty && x.length <= supportingInfo))
        .verifying(Messages("ated.property-details-period-error.supportingInfo.invalid", Messages("ated.property-details-period.supportingInfo")), x => x matches supportingInfoRegex)
    )(PropertyDetailsSupportingInfo.apply)(PropertyDetailsSupportingInfo.unapply)
  )

  def propertyDetailsValueValidation = {
    import PropertyValueField._

    optional(text)
      .verifying(Messages("ated.property-details-value.incorrect-format"), propertyValue => {
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
        case a => None
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
      val formErrors = (PropertyDetailsFormsValidation.checkPartAcqDispDate(periodKey, f.get.isPropertyRevalued,  f.get.partAcqDispDate)
        ++ validateValue(f.get.isPropertyRevalued == Some(true), "revaluedValue", f.get.revaluedValue, f)
        ++ PropertyDetailsFormsValidation.checkRevaluedDate(periodKey, f.get.isPropertyRevalued, f.get.revaluedDate)
        ).flatten
      addErrorsToForm(f, formErrors)
    } else f
  }

  def validatePropertyDetailsTaxAvoidance(f: Form[PropertyDetailsTaxAvoidance]): Form[PropertyDetailsTaxAvoidance] = {
    if (!f.hasErrors) {
      val formErrors = (PropertyDetailsFormsValidation.validateAvoidanceSchemeRefNo(f.get.isTaxAvoidance,
        f.get.taxAvoidanceScheme,
        f.get.taxAvoidancePromoterReference)).flatten
      addErrorsToForm(f, formErrors)
    } else f
  }

  def validatePropertyDetailsOwnedBefore(f: Form[PropertyDetailsOwnedBefore]): Form[PropertyDetailsOwnedBefore] = {
    if (!f.hasErrors) {
      val formErrors = (validateValue(f.get.isOwnedBefore2012 == Some(true), "ownedBefore2012Value", f.get.ownedBefore2012Value, f)).flatten
      addErrorsToForm(f, formErrors)
    } else f
  }

  def validatePropertyDetailsNewBuild(periodKey: Int, f: Form[PropertyDetailsNewBuild]): Form[PropertyDetailsNewBuild] = {
    if (!f.hasErrors) {
      val formErrors = (
          PropertyDetailsFormsValidation.validateBuildDate(periodKey, f, f.get.isNewBuild) ++
          validateValue(f.get.isNewBuild == Some(true), "newBuildValue", f.get.newBuildValue, f) ++
          validateValue(f.get.isNewBuild == Some(false), "notNewBuildValue", f.get.notNewBuildValue, f)
        ).flatten
      addErrorsToForm(f, formErrors)
    } else f
  }

  def validatePropertyDetailsDatesLiable(periodKey: Int, f: Form[PropertyDetailsDatesLiable], periodsCheck: Boolean, currentPeriods: List[LineItem] = Nil): Form[PropertyDetailsDatesLiable] = {
    val basicErrorForm = if (!f.hasErrors) {
      val formErrors = (PropertyDetailsFormsValidation.validateStartEndDates("ated.property-details-period.datesLiable", periodKey, f)).flatten
      addErrorsToForm(f, formErrors)
    } else f

    if (!basicErrorForm.hasErrors && periodsCheck) {
      val formErrors = (PropertyDetailsFormsValidation.validateDatesInExistingPeriod("ated.property-details-period.datesLiable", currentPeriods, f)).flatten
      addErrorsToForm(basicErrorForm, formErrors)
    } else basicErrorForm
  }

  def validatePropertyDetailsDatesInRelief(periodKey: Int, f: Form[PropertyDetailsDatesInRelief], currentPeriods: List[LineItem]): Form[PropertyDetailsDatesInRelief] = {
    val basicErrorForm = if (!f.hasErrors) {
      val formErrors = (PropertyDetailsFormsValidation.validateStartEndDates("ated.property-details-period.datesInRelief", periodKey, f)).flatten
      addErrorsToForm(f, formErrors)
    } else f

    if (!basicErrorForm.hasErrors) {
      val formErrors = (PropertyDetailsFormsValidation.validateDatesInExistingPeriod("ated.property-details-period.datesInRelief", currentPeriods, f)).flatten
      addErrorsToForm(basicErrorForm, formErrors)
    } else basicErrorForm
  }

  private def validateValue(requiresValidation: Boolean, fieldName: String, fieldValue: Option[BigDecimal], f: Form[_]): Seq[Option[FormError]] = {
    if (requiresValidation) {
      if (f.data.get(fieldName).isDefined && !f.data.get(fieldName).contains("")) {
        if (fieldValue.exists(a => a.toDouble >= maximumPropertyValue)) {
          Seq(Some(FormError(fieldName, Messages(s"ated.property-details-value.$fieldName.error.too-high"))))
        } else if (fieldValue.exists(a => a.toDouble < minimumPropertyValue)) {
          Seq(Some(FormError(fieldName, Messages(s"ated.property-details-value.$fieldName.error.too-low"))))
        } else Seq(None)
      } else Seq(Some(FormError(fieldName, Messages(s"ated.property-details-value.$fieldName.error.empty"))))
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


}
