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

package models

import play.api.libs.json.{Json, OFormat}

case class SortCode(firstElement: String, secondElement: String, thirdElement: String) {
  override def toString: String = s"$firstElement - $secondElement - $thirdElement"
}


object SortCode {

  implicit val formats: OFormat[SortCode] = Json.format[SortCode]
  private val FIRST_ELEMENT_START = 0
  private val SECOND_ELEMENT_START = 2
  private val THIRD_ELEMENT_START = 4
  private val SORT_CODE_LENGTH = 6

  def fromString(sixDigits: String): SortCode = {
    require(sixDigits.length == SORT_CODE_LENGTH, s"Invalid SortCode, must be $SORT_CODE_LENGTH characters in length")
    apply(sixDigits.substring(FIRST_ELEMENT_START, SECOND_ELEMENT_START),
      sixDigits.substring(SECOND_ELEMENT_START, THIRD_ELEMENT_START),
      sixDigits.substring(THIRD_ELEMENT_START, SORT_CODE_LENGTH))
  }
}

case class BicSwiftCode(swiftCode: String) {
  private val strippedSwiftCode: String = swiftCode.replaceAll(" ", "")

  private def bankCode: String = {
    val BANK_CODE_START = 0
    val BANK_CODE_END = 4
    strippedSwiftCode.substring(BANK_CODE_START, BANK_CODE_END)
  }

  def countryCode: String = {
    val COUNTRY_CODE_START = 4
    val COUNTRY_CODE_END = 6
    strippedSwiftCode.substring(COUNTRY_CODE_START, COUNTRY_CODE_END)
  }

  private def locationCode: String = {
    val LOCATION_CODE_START = 6
    val LOCATION_CODE_END = 8
    strippedSwiftCode.substring(LOCATION_CODE_START, LOCATION_CODE_END)
  }
  private def branchCode: String = {
    val BRANCH_CODE_START = 8
    val BRANCH_CODE_END = 11
    if (strippedSwiftCode.length >= BRANCH_CODE_END) {
      strippedSwiftCode.substring(BRANCH_CODE_START, BRANCH_CODE_END)
    } else {
      ""
    }
  }

  override def toString: String = {
    s"$bankCode $countryCode $locationCode $branchCode".trim
  }
}

object BicSwiftCode extends (String => BicSwiftCode){
  implicit val formats: OFormat[BicSwiftCode] = Json.format[BicSwiftCode]

  def isValid(swiftCode: String): Boolean = {
    val stripped = swiftCode.replaceAll(" ", "")
    val SWIFT_CODE_LENGTH_1 = 8
    val SWIFT_CODE_LENGTH_2 = 11
    stripped.length == SWIFT_CODE_LENGTH_1 || stripped.length == SWIFT_CODE_LENGTH_2
  }
}

case class Iban(iban: String) {
  override def toString: String = iban
}
object Iban extends (String => Iban){
  implicit val formats: OFormat[Iban] = Json.format[Iban]

  def isValid(iban: String): Boolean = {
    val stripped = iban.replaceAll(" ", "")
    val MIN_IBAN_LENGTH = 1
    val MAX_IBAN_LENGTH = 34
    stripped.length >= MIN_IBAN_LENGTH && stripped.length <= MAX_IBAN_LENGTH
  }
}

case class HasBankDetails(hasBankDetails: Option[Boolean] = None)

object HasBankDetails {
  implicit val format: OFormat[HasBankDetails] = Json.format[HasBankDetails]
}

case class HasUkBankAccount(hasUkBankAccount: Option[Boolean] = Some(false))

object HasUkBankAccount {
  implicit val format: OFormat[HasUkBankAccount] = Json.format[HasUkBankAccount]
}


case class BankDetails(hasUKBankAccount: Option[Boolean] = None,
                       accountName: Option[String] = None,
                       accountNumber: Option[String] = None,
                       sortCode: Option[SortCode] = None,
                       buildingNumber: Option[String] = None,
                       bicSwiftCode: Option[BicSwiftCode] = None,
                       iban: Option[Iban] = None)

object BankDetails {
  implicit val format: OFormat[BankDetails] = Json.format[BankDetails]
}


case class BankDetailsModel(hasBankDetails: Boolean,
                            bankDetails: Option[BankDetails] = None)

object BankDetailsModel {
  implicit val format: OFormat[BankDetailsModel] = Json.format[BankDetailsModel]
}
