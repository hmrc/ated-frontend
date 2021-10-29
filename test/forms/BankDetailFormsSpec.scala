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

import models.{BicSwiftCode, Iban, SortCode}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class BankDetailFormsSpec extends PlaySpec with GuiceOneServerPerSuite {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())

  val validUkData: Map[String, String] = Map("hasUKBankAccount" -> "true",
      "accountName" -> "Account Name",
      "accountNumber" -> "12345678",
      "sortCode.firstElement" -> "11",
      "sortCode.secondElement" -> "22",
      "sortCode.thirdElement" -> "33"
    )

  val validNonUkData: Map[String, String] = Map("hasUKBankAccount" -> "false",
    "bicSwiftCode" -> "123654789654",
    "iban" -> "GADGSDGADSGF"
  )

  val maxLengthUkData: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "Account Name"*20,
    "accountNumber" -> "12345678"*10,
    "sortCode.firstElement" -> "11"*10,
    "sortCode.secondElement" -> "22"*10,
    "sortCode.thirdElement" -> "33"*10
  )

  val maxLengthNonUkData: Map[String, String] = Map("hasUKBankAccount" -> "false",
    "accountName" -> "Account Name"*20,
    "bicSwiftCode" -> "123654789654"*20,
    "iban" -> "GADGSDGADSGF"*10
  )

  val emptyUkData: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "",
    "accountNumber" -> "",
    "sortCode.firstElement" -> "",
    "sortCode.secondElement" -> "",
    "sortCode.thirdElement" -> ""
  )

  val emptyNonUkData: Map[String, String] = Map("hasUKBankAccount" -> "false",
    "accountName" -> "",
    "bicSwiftCode" -> "",
    "iban" -> ""
  )

  "bankDetailsForm" must {
    "pass through validation" when {
      "supplied with valid data for uk accounts" in {
        BankDetailForms.bankDetailsForm.bind(validUkData).fold(
          formWithErrors => {
            formWithErrors.errors.isEmpty mustBe true

          },
          success => {
            success.accountName mustBe Some("Account Name")
            success.accountNumber mustBe Some("12345678")
            success.sortCode mustBe Some(SortCode("11","22","33"))
          }
        )
      }

      "supplied with valid data for non uk accounts" in {
        BankDetailForms.bankDetailsForm.bind(validNonUkData).fold(
          formWithErrors => {
            formWithErrors.errors.isEmpty mustBe true

          },
          success => {
            success.bicSwiftCode mustBe Some(BicSwiftCode("123654789654"))
            success.iban mustBe Some(Iban("GADGSDGADSGF"))
          }
        )
      }
    }

    "fail validation" when {

      "supplied with empty form" in {
        BankDetailForms.bankDetailsForm.bind(Map.empty[String, String]).fold (
          formWithErrors => {
            formWithErrors.errors.length mustBe 1
            formWithErrors.errors.head.message mustBe "ated.bank-details.error-key.hasUKBankAccount.empty"
          },
          _ => {
            fail("Form should give an error")
          }
        )
      }

      "supplied with uk account 'empty form'" in {
        BankDetailForms.validateBankDetails(BankDetailForms.bankDetailsForm.bind(emptyUkData)).fold (
          formWithErrors => {
            formWithErrors.errors.length mustBe 3
            formWithErrors.errors.head.message mustBe "ated.bank-details.error-key.accountName.empty"
            formWithErrors.errors(1).message mustBe "ated.bank-details.error-key.accountNumber.empty"
            formWithErrors.errors.last.message mustBe "ated.bank-details.error-key.sortCode.empty"
          },
          _ => {
            fail("Form should give an error")
          }
        )
      }

      "supplied with non uk account 'empty form'" in {
        BankDetailForms.validateBankDetails(BankDetailForms.bankDetailsForm.bind(emptyNonUkData)).fold (
          formWithErrors => {
            formWithErrors.errors.length mustBe 3
            formWithErrors.errors.head.message mustBe "ated.bank-details.error-key.accountName.empty"
            formWithErrors.errors(1).message mustBe "ated.bank-details.error-key.iban.empty"
            formWithErrors.errors.last.message mustBe "ated.bank-details.error-key.bicSwiftCode.empty"

          },
          _ => {
            fail("Form should give an error")
          }
        )
      }

      "supplied with uk account form data which exceeds max length" in {
        BankDetailForms.validateBankDetails(BankDetailForms.bankDetailsForm.bind(maxLengthUkData)).fold (
          formWithErrors => {
            formWithErrors.errors.length mustBe 3
            formWithErrors.errors.head.message mustBe "ated.bank-details.error-key.accountName.max-len"
            formWithErrors.errors(1).message mustBe "ated.bank-details.error-key.accountNumber.max-len"
            formWithErrors.errors.last.message mustBe "ated.bank-details.error-key.sortCode.invalid"
          },
          _ => {
            fail("Form should give an error")
          }
        )
      }

      "supplied with non uk form data which exceeds  max length" in {
        BankDetailForms.validateBankDetails(BankDetailForms.bankDetailsForm.bind(maxLengthNonUkData)).fold (
          formWithErrors => {
            formWithErrors.errors.length mustBe 3
            formWithErrors.errors.head.message mustBe "ated.bank-details.error-key.accountName.max-len"
            formWithErrors.errors(1).message mustBe "ated.bank-details.error-key.iban.max-len"
            formWithErrors.errors.last.message mustBe "ated.bank-details.error-key.bicSwiftCode.invalid"

          },
          _ => {
            fail("Form should give an error")
          }
        )

      }
    }
  }

}
