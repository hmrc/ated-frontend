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
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Mapping, Form, FormError}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.annotation.tailrec
import scala.util.Try

object BankDetailForms {

  val TWO = 2

  val sortCodeTuple: Mapping[Option[SortCode]] = sortCodeTupleOpt

  //scalastyle:off cyclomatic.complexity
  def sortCodeTupleOpt = {
    import SortCodeFields._

    tuple("firstElement" -> optional(text), "secondElement" -> optional(text), "thirdElement" -> optional(text))
    .transform[Option[SortCode]](
      {
        case (Some(e1), Some(e2), Some(e3)) if isValid(e1) && isValid(e2) & isValid(e3) => Some(SortCode(e1, e2, e3))
        case (a, b, c) => None
      }, {
        case Some(a) => (Some(a.firstElement), Some(a.secondElement), Some(a.thirdElement))
        case _ => (None, None, None)
      }
    )
  }

  object SortCodeFields {
    def isValid(value: String): Boolean = value.length == TWO && Try(value.toInt).isSuccess
  }

  implicit val bicSwiftFormat = new Formatter[BicSwiftCode] {
    def bind(key: String, data: Map[String, String]):Either[Seq[FormError], BicSwiftCode] = {
      val bicSwiftValue = data.get(key).getOrElse("")
      Right(BicSwiftCode(bicSwiftValue))
    }

    def unbind(key: String, value: BicSwiftCode) = Map(key -> value.toString)
  }

  implicit val ibanFormat = new Formatter[Iban] {
    def bind(key: String, data: Map[String, String]):Either[Seq[FormError], Iban] = {
      val ibanValue = data.get(key).getOrElse("")
      Right(Iban(ibanValue))
    }

    def unbind(key: String, value: Iban) = Map(key -> value.toString)
  }


  val hasBankDetailsForm = Form(mapping(
    "hasBankDetails" -> optional(boolean).verifying(Messages("ated.bank-details.error-key.hasBankDetails.empty"), a => a.isDefined)
  )(HasBankDetails.apply)(HasBankDetails.unapply))

  val bankDetailsForm = Form(mapping(
    "hasUKBankAccount" -> optional(boolean).verifying(Messages("ated.bank-details.error-key.hasUKBankAccount.empty"), a => a.isDefined),
    "accountName" -> optional(text),
    "accountNumber" -> optional(text),
    "sortCode" -> sortCodeTuple,
    "bicSwiftCode" -> optional(of[BicSwiftCode]),
    "iban" -> optional(of[Iban])
  )(BankDetails.apply)(BankDetails.unapply))

  //scalastyle:off cyclomatic.complexity
  def validateBankDetails(bankDetails: Form[BankDetails]): Form[BankDetails] = {
    val hasUKBankAccount = bankDetails.data.get("hasUKBankAccount").map(_.toBoolean)

    def validate: Seq[Option[FormError]] = {
      hasUKBankAccount match {
        case Some(false) => validateAccountName ++ validateIBAN ++ validateBicSwiftCode
        case Some(true) => validateAccountName ++ validateAccountNumber ++ validateSortCode
        case _ => Seq(Some(FormError("hasUKBankAccount", Messages("ated.bank-details.error-key.hasUKBankAccount.empty"))))
      }
    }

    def validateAccountName: Seq[Option[FormError]] = {
      val accountName = bankDetails.data.get("accountName").map(_.trim)
      if (accountName.getOrElse("").length == 0) Seq(Some(FormError("accountName", Messages("ated.bank-details.error-key.accountName.empty"))))
      else if (accountName.nonEmpty && accountName.getOrElse("").length > 60) {
        Seq(Some(FormError("accountName", Messages("ated.bank-details.error-key.accountName.max-len"))))
      }
      else Seq()
    }

    def validateAccountNumber: Seq[Option[FormError]] = {
      val accountNumber = bankDetails.data.get("accountNumber").map(_.trim)
      if (accountNumber.getOrElse("").length == 0) {
        Seq(Some(FormError("accountNumber", Messages("ated.bank-details.error-key.accountNumber.empty"))))
      }
      else if (accountNumber.nonEmpty && accountNumber.getOrElse("").length > 18) {
        Seq(Some(FormError("accountNumber", Messages("ated.bank-details.error-key.accountNumber.max-len"))))
      }
      else if (accountNumber.nonEmpty && !accountNumber.get.matches("""^[0-9]+$""")) {
        Seq(Some(FormError("accountNumber", Messages("ated.bank-details.error-key.accountNumber.invalid"))))
      }
      else Seq()
    }

    def validateSortCode: Seq[Option[FormError]] = {
      val sortCodeElement1 = bankDetails.data.get("sortCode.firstElement").map(_.trim)
      val sortCodeElement2 = bankDetails.data.get("sortCode.secondElement").map(_.trim)
      val sortCodeElement3 = bankDetails.data.get("sortCode.thirdElement").map(_.trim)
      (sortCodeElement1, sortCodeElement2, sortCodeElement3) match {
        case (Some(a), Some(b), Some(c)) if a.length > 0 && b.length > 0 && c.length > 0 =>
          if (SortCodeFields.isValid(a) && SortCodeFields.isValid(b) & SortCodeFields.isValid(c)) Seq()
          else Seq(Some(FormError("sortCode", "ated.bank-details.error-key.sortCode.invalid")))
        case (_, _, _) => Seq(Some(FormError("sortCode", "ated.bank-details.error-key.sortCode.empty")))
      }
    }

    def validateIBAN: Seq[Option[FormError]] = {
      val iban = bankDetails.data.get("iban").map(_.trim).getOrElse("").replaceAll(" ", "")
      if (iban.length == 0) Seq(Some(FormError("iban", Messages("ated.bank-details.error-key.iban.empty"))))
      else if (iban.length > 34) Seq(Some(FormError("iban", Messages("ated.bank-details.error-key.iban.max-len"))))
      else if (!Iban.isValid(iban)) Seq(Some(FormError("iban", Messages("ated.bank-details.error-key.iban.invalid"))))
      else Seq()
    }

    def validateBicSwiftCode: Seq[Option[FormError]] = {
      val bicSwiftCode = bankDetails.data.get("bicSwiftCode").map(_.trim).getOrElse("").replaceAll(" ", "")
      if (bicSwiftCode.length == 0) Seq(Some(FormError("bicSwiftCode", Messages("ated.bank-details.error-key.bicSwiftCode.empty"))))
      else if (!BicSwiftCode.isValid(bicSwiftCode)) Seq(Some(FormError("bicSwiftCode", Messages("ated.bank-details.error-key.bicSwiftCode.invalid"))))
      else Seq()
    }


    if (!bankDetails.hasErrors) {
      val formErrors = validate.flatten
      addErrorsToForm(bankDetails, formErrors)
    } else {
      bankDetails
    }
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
