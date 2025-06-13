/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, FormError, Mapping}

import scala.annotation.tailrec
import scala.util.Try

object BankDetailForms {

  val SIX = 6

  val sortCodeTuple: Mapping[Option[SortCode]] = sortCodeTupleOpt

  val sortCodeRegEx = """^[0-9]{2}\s?\-?\–?[0-9]{2}\s?\-?\–?[0-9]{2}$"""

  def sanitiseSortCode(sortCode: String): String = sortCode.replaceAll("""[\s\-–]""", "")

  def sortCodeTupleOpt: Mapping[Option[SortCode]] = {

    optional(text)
      .transform[Option[SortCode]](
        {
          case Some(text) if text.matches(sortCodeRegEx) => Some(SortCode.fromString(sanitiseSortCode(text)))
          case _ => None
        }, {
          case Some(a) => Some(sanitiseSortCode(a.toString))
          case _ => None
        }
      )
  }

  object SortCodeFields {
    def isValid(value: String): Boolean = value.length == SIX && Try(value.toInt).isSuccess
  }

  implicit val bicSwiftFormat: Formatter[BicSwiftCode] = new Formatter[BicSwiftCode] {
    def bind(key: String, data: Map[String, String]):Either[Seq[FormError], BicSwiftCode] = {
      val bicSwiftValue = data.get(key).getOrElse("")
      Right(BicSwiftCode(bicSwiftValue))
    }

    def unbind(key: String, value: BicSwiftCode): Map[String, String] = Map(key -> value.toString)
  }

  implicit val ibanFormat: Formatter[Iban] = new Formatter[Iban] {
    def bind(key: String, data: Map[String, String]):Either[Seq[FormError], Iban] = {
      val ibanValue = data.get(key).getOrElse("")
      Right(Iban(ibanValue))
    }

    def unbind(key: String, value: Iban): Map[String, String] = Map(key -> value.toString)
  }

  val hasBankDetailsForm: Form[HasBankDetails] = Form(mapping(
    "hasBankDetails" -> optional(boolean).verifying("ated.bank-details.error-key.hasBankDetails.empty", a => a.isDefined)
  )(HasBankDetails.apply)(HasBankDetails.unapply))

  val hasUkBankDetailsForm: Form[HasUkBankDetails] = Form(mapping(
    "hasUkBankDetails" -> optional(boolean).verifying("ated.bank-details.error-key.hasUkBankDetails.empty", a => a.isDefined)
  )(HasUkBankDetails.apply)(HasUkBankDetails.unapply))

   lazy val accountNameConstraint: Constraint[Option[String]] = Constraint("accountName.validation") ({ data =>
    val accountName = data.map(_.trim)
    val errors  = {
      if (accountName.getOrElse("").isEmpty) Seq(ValidationError("ated.bank-details.error-key.accountName.empty"))
      else if (accountName.nonEmpty && accountName.getOrElse("").length > 60) {
        Seq(ValidationError("ated.bank-details.error-key.accountName.max-len"))
      }
      else Nil
    }
    if (errors.isEmpty) Valid else Invalid(errors)
  })

  val bankDetailsForm: Form[BankDetails] = Form(mapping(
    "hasUKBankAccount" -> optional(boolean),
    "accountName" -> optional(text).verifying(accountNameConstraint),
    "accountNumber" -> optional(text),
    "sortCode" -> sortCodeTuple,
    "buildingNumber" -> optional(text),
    "bicSwiftCode" -> optional(of[BicSwiftCode]),
    "iban" -> optional(of[Iban])
  )(BankDetails.apply)(BankDetails.unapply))

  def validateBankDetails(controllerId: String, bankDetails: Form[BankDetails]): Form[BankDetails] = {
    val hasUKBankAccount = bankDetails.data.get("hasUKBankAccount").map(_.toBoolean)

    def validate: Seq[Option[FormError]] = {
      (hasUKBankAccount, controllerId) match {
        case (Some(false), _) => validateIBAN ++ validateBicSwiftCode
        case (Some(true), _) => validateAccountNumber ++ validateSortCode
        case (None, "DisposeLiabilityUkBankDetailsController") => validateAccountNumber ++ validateSortCode
        case (None, "DisposeLiabilityNonUkBankDetailsController") => validateIBAN ++ validateBicSwiftCode
        case (None, _) => Seq(Some(FormError("hasUKBankAccount", "ated.bank-details.error-key.hasUKBankAccount.empty")))
      }
    }

    def validateAccountNumber: Seq[Option[FormError]] = {
      val accountNumber = bankDetails.data.get("accountNumber").map(_.trim)
      if (accountNumber.getOrElse("").length == 0) {
        Seq(Some(FormError("accountNumber", "ated.bank-details.error-key.accountNumber.empty")))
      }
      else if (accountNumber.nonEmpty && accountNumber.getOrElse("").length > 18) {
        Seq(Some(FormError("accountNumber", "ated.bank-details.error-key.accountNumber.max-len")))
      }
      else if (accountNumber.nonEmpty && !accountNumber.get.matches("""^[0-9]+$""")) {
        Seq(Some(FormError("accountNumber", "ated.bank-details.error-key.accountNumber.invalid")))
      }
      else Seq()
    }

    def validateSortCode: Seq[Option[FormError]] = {
      val sortCode = bankDetails.data.get("sortCode").map(x => sanitiseSortCode(x))

      sortCode match {
        case Some(a) if a.length > 0 =>
          if (SortCodeFields.isValid(a)) Seq()
          else Seq(Some(FormError("sortCode", "ated.bank-details.error-key.sortCode.invalid")))
        case _ => Seq(Some(FormError("sortCode", "ated.bank-details.error-key.sortCode.empty")))
      }
    }

    def validateIBAN: Seq[Option[FormError]] = {
      val iban = bankDetails.data.get("iban").map(_.trim).getOrElse("").replaceAll(" ", "")
      if (iban.length == 0) Seq(Some(FormError("iban", "ated.bank-details.error-key.iban.empty")))
      else if (iban.length > 34) Seq(Some(FormError("iban", "ated.bank-details.error-key.iban.max-len")))
      else if (!Iban.isValid(iban)) Seq(Some(FormError("iban", "ated.bank-details.error-key.iban.invalid")))
      else Seq()
    }

    def validateBicSwiftCode: Seq[Option[FormError]] = {
      val bicSwiftCode = bankDetails.data.get("bicSwiftCode").map(_.trim).getOrElse("").replaceAll(" ", "")
      if (bicSwiftCode.length == 0) Seq(Some(FormError("bicSwiftCode", "ated.bank-details.error-key.bicSwiftCode.empty")))
      else if (!BicSwiftCode.isValid(bicSwiftCode)) Seq(Some(FormError("bicSwiftCode", "ated.bank-details.error-key.bicSwiftCode.invalid")))
      else Seq()
    }

    addErrorsToForm(bankDetails, validate.flatten)
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
