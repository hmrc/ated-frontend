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

import forms.BankDetailForms.{bankDetailsForm, hasUkBankAccountForm}
import models.{BicSwiftCode, HasUkBankAccount, Iban, SortCode}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class BankDetailFormsSpec extends PlaySpec with GuiceOneServerPerSuite {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())

  val validUkData: Map[String, String] = Map("hasUKBankAccount" -> "true",
      "accountName" -> "Account Name",
      "accountNumber" -> "12345678",
      "sortCode" -> "112233"
    )

  val validUkDataWithSpaceAtEndOfSortCode: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "Account Name",
    "accountNumber" -> "12345678",
    "sortCode" -> "112233    "
  )

  val validUkDataWithSpaceAtStartOfSortCode: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "Account Name",
    "accountNumber" -> "12345678",
    "sortCode" -> "    112233"
  )

  val validUKDataWithSpaceInMiddleOfSortCode: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "Account Name",
    "accountNumber" -> "12345678",
    "sortCode" -> "112 233"
  )

  val validUkDataWithSpaceInMiddleOfSortCodeWithDashes: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "Account Name",
    "accountNumber" -> "12345678",
    "sortCode" -> "11-22 33"
  )


  val validUkDataWithSpaceAtEndAN: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "Account Name",
    "accountNumber" -> "12345678   ",
    "sortCode" -> "112233"
  )


  val validAccountNumberWithSpaces: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "Account Name",
    "accountNumber" -> " 123 45678 ",
    "sortCode" -> "112233"
  )


  val validUkDataWithSpaceAtStartAN: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "Account Name",
    "accountNumber" -> "   12345678",
    "sortCode" -> "112233"
  )

  val nonValidUkData: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "Account Name",
    "accountNumber" -> "aaaaaa",
    "sortCode" -> "aaaaaa"
  )

  val validNonUkData: Map[String, String] = Map("hasUKBankAccount" -> "false",
    "accountName" -> "Account Name",
    "bicSwiftCode" -> "12345678998",
    "iban" -> "GADGSDGADSGF"
  )
  val nonValidNonUkData: Map[String, String] = Map("hasUKBankAccount" -> "false",
    "accountName" -> "Account Name",
    "bicSwiftCode" -> "aaaaaa",
    "iban" -> "aaaaaa"
  )

  val maxLengthUkData: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "Account Name"*20,
    "accountNumber" -> "12345678"*10,
    "sortCode" -> "112233"*100
  )

  val maxLengthNonUkData: Map[String, String] = Map("hasUKBankAccount" -> "false",
    "accountName" -> "Account Name"*20,
    "bicSwiftCode" -> "123654789654"*20,
    "iban" -> "GADGSDGADSGF"*10
  )

  val emptyUkData: Map[String, String] = Map("hasUKBankAccount" -> "true",
    "accountName" -> "",
    "accountNumber" -> "",
    "sortCode" -> ""
  )

  val emptyNonUkData: Map[String, String] = Map("hasUKBankAccount" -> "false",
    "accountName" -> "",
    "bicSwiftCode" -> "",
    "iban" -> ""
  )

  "bankDetailsForm" must {

    "bind successfully when 'hasUkBankAccount' is true" in {
      val data: Map[String, String] = Map("hasUkBankAccount" -> "true")
      val boundForm: Form[HasUkBankAccount] = hasUkBankAccountForm.bind(data)
      boundForm.hasErrors mustBe false
      boundForm.value mustBe Some(HasUkBankAccount(Some(true)))
    }

    "show an error when 'hasUkBankAccount' is missing" in {
      val data: Map[String, String] = Map.empty
      val boundForm: Form[HasUkBankAccount] = hasUkBankAccountForm.bind(data)
      boundForm.hasErrors mustBe true
      boundForm.errors.size mustBe 1
      boundForm.error("hasUkBankAccount").map(_.message) mustBe Some("ated.bank-details.error-key.hasUkBankAccount.empty")
    }

    "pass through validation" when {
      "supplied with valid data for uk accounts" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(validUkData)).fold(
          formWithErrors => {
            fail(s"form should not have errors. Errors: ${formWithErrors.errors}")

          },
          success => {
            success.accountName mustBe Some("Account Name")
            success.accountNumber mustBe Some("12345678")
            success.sortCode mustBe Some(SortCode("11","22","33"))
          }
        )
      }

      "supplied with valid data for uk accounts with spaces in account number" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(validAccountNumberWithSpaces)).fold(
          formWithErrors => {
            fail(s"form should not have errors. Errors: ${formWithErrors.errors}")

          },
          success => {
            success.accountName mustBe Some("Account Name")
            success.accountNumber mustBe Some("12345678")
            success.sortCode mustBe Some(SortCode("11","22","33"))
          }
        )
      }

      "supplied with sort code with blank spaces at end for uk accounts" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(validUkDataWithSpaceAtEndOfSortCode)).fold(
          formWithErrors => {
            fail(s"form should not have errors. Errors: ${formWithErrors.errors}")

          },
          success => {
            success.accountName mustBe Some("Account Name")
            success.accountNumber mustBe Some("12345678")
            success.sortCode mustBe Some(SortCode("11","22","33"))
          }
        )
      }

      "supplied with sort code with blank spaces at start for uk accounts" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(validUkDataWithSpaceAtStartOfSortCode)).fold(
          formWithErrors => {
            fail(s"form should not have errors. Errors: ${formWithErrors.errors}")

          },
          success => {
            success.accountName mustBe Some("Account Name")
            success.accountNumber mustBe Some("12345678")
            success.sortCode mustBe Some(SortCode("11","22","33"))
          }
        )
      }

      "supplied with sort code with blank spaces in middle for uk accounts" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(validUKDataWithSpaceInMiddleOfSortCode)).fold(
          formWithErrors => {
            fail(s"form should not have errors. Errors: ${formWithErrors.errors}")

          },
          success => {
            success.accountName mustBe Some("Account Name")
            success.accountNumber mustBe Some("12345678")
            success.sortCode mustBe Some(SortCode("11","22","33"))
          }
        )
      }

      "supplied with sort code with blank spaces and dashes for uk accounts" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(validUkDataWithSpaceInMiddleOfSortCodeWithDashes)).fold(
          formWithErrors => {
            fail(s"form should not have errors. Errors: ${formWithErrors.errors}")

          },
          success => {
            success.accountName mustBe Some("Account Name")
            success.accountNumber mustBe Some("12345678")
            success.sortCode mustBe Some(SortCode("11","22","33"))
          }
        )
      }


      "supplied with AN with blank spaces at end for uk accounts" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(validUkDataWithSpaceAtEndAN)).fold(
          formWithErrors => {
            fail(s"form should not have errors. Errors: ${formWithErrors.errors}")

          },
          success => {
            success.accountName mustBe Some("Account Name")
            success.accountNumber mustBe Some("12345678")
            success.sortCode mustBe Some(SortCode("11","22","33"))
          }
        )
      }

      "supplied with AN with blank spaces at start for uk accounts" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(validUkDataWithSpaceAtStartAN)).fold(
          formWithErrors => {
            fail(s"form should not have errors. Errors: ${formWithErrors.errors}")

          },
          success => {
            success.accountName mustBe Some("Account Name")
            success.accountNumber mustBe Some("12345678")
            success.sortCode mustBe Some(SortCode("11","22","33"))
          }
        )
      }



      "supplied with valid data for non uk accounts" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(validNonUkData)).fold(
          formWithErrors => {
            fail(s"form should not have errors. Errors: ${formWithErrors.errors}")

          },
          success => {
            success.bicSwiftCode mustBe Some(BicSwiftCode("12345678998"))
            success.iban mustBe Some(Iban("GADGSDGADSGF"))
          }
        )
      }
    }

    "fail validation" when {

      "supplied with empty form" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(Map.empty[String, String])).fold (
          formWithErrors => {
            formWithErrors.errors.length mustBe 2
            formWithErrors.errors.head.message mustBe "ated.bank-details.error-key.accountName.empty"
            formWithErrors.errors.last.message mustBe "ated.bank-details.error-key.hasUKBankAccount.empty"
          },
          _ => {
            fail("Form should give an error")
          }
        )
      }

      "supplied with uk account 'empty form'" in {
        BankDetailForms.validateBankDetails("", bankDetailsForm.bind(emptyUkData)).fold (
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
        BankDetailForms.validateBankDetails("", BankDetailForms.bankDetailsForm.bind(emptyNonUkData)).fold (
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
        BankDetailForms.validateBankDetails("", BankDetailForms.bankDetailsForm.bind(maxLengthUkData)).fold (
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
        BankDetailForms.validateBankDetails("", BankDetailForms.bankDetailsForm.bind(maxLengthNonUkData)).fold (
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

      "supplied with uk account form non valid data" in {
        BankDetailForms.validateBankDetails("", BankDetailForms.bankDetailsForm.bind(nonValidUkData)).fold (
          formWithErrors => {
            formWithErrors.errors.length mustBe 2
            formWithErrors.errors.head.message mustBe "ated.bank-details.error-key.accountNumber.invalid"
            formWithErrors.errors.last.message mustBe "ated.bank-details.error-key.sortCode.invalid"
          },
          _ => {
            fail("Form should give an error")
          }
        )
      }

      "supplied with non uk account form non valid data" in {
        BankDetailForms.validateBankDetails("", BankDetailForms.bankDetailsForm.bind(nonValidNonUkData)).fold (
          formWithErrors => {
            formWithErrors.errors.length mustBe 1
            formWithErrors.errors.head.message mustBe "ated.bank-details.error-key.bicSwiftCode.invalid"
          },
          _ => {
            fail("Form should give an error")
          }
        )
      }

    }
  }

}
