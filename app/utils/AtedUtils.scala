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

package utils

import models._
import org.joda.time.LocalDate
import play.api.mvc.{AnyContent, Request}
import utils.AtedConstants._
import scala.language.postfixOps


object AtedUtils {
  //scalastyle:off magic.number
  def isValidARN(arn: String): Boolean = {
    patternCheckARN(arn) match {
      case true =>
        val checkCharacter = arn.toUpperCase.charAt(0)
        val weights = List(9, 10, 11, 12, 13, 8, 7, 6, 5, 4)
        val equivalentValue = List(33, 50, 46)
        val equivalentValues = equivalentValue ++ arn.toList.filter(_ isDigit).map(_ asDigit)
        val SumOfWeightedValues = (for ((w1, d1) <- equivalentValues zip weights) yield w1 * d1).sum
        val remainder = SumOfWeightedValues % 23
        val mapOfRemainders = Map(0 -> "A", 1 -> "B", 2 -> "C", 3 -> "D", 4 -> "E", 5 -> "F", 6 -> "G", 7 -> "H", 8 -> "X", 9 -> "J", 10 -> "K", 11 -> "L",
          12 -> "M", 13 -> "N", 14 -> "Y", 15 -> "P", 16 -> "Q", 17 -> "R", 18 -> "S", 19 -> "T", 20 -> "Z", 21 -> "V", 22 -> "W")
        mapOfRemainders.get(remainder).contains(checkCharacter.toString)
      case false => false
    }
  }

  def patternCheckARN(arn: String): Boolean = {
    val pattern = """^[A-Za-z][Aa][Rr][Nn]\d{7}"""
    arn.matches(pattern)
  }


  def printHeaderMsg(returnType: String): String = {
    returnType match {
      case "F" => FurtherReturnDec
      case "A" => AmendedReturnDec
      case "C" => ChangeDetailsDec
    }
  }

  def printSubmitMsg(returnType: String): String = {
    returnType match {
      case "F" => FurtherReturnSub
      case "A" => AmendedReturnSub
      case "C" => ChangedReturnSub
    }
  }

  def formatPostCode(postCode: Option[String]): Option[String] = {
    postCode.map(formatMandatoryPostCode(_))
  }

  def formatMandatoryPostCode(postCode: String): String = {
    val trimmedPostcode = postCode.replaceAll(" ", "").toUpperCase()
    val postCodeSplit = trimmedPostcode splitAt (trimmedPostcode.length - 3)
    postCodeSplit._1 + " " + postCodeSplit._2
  }


  val EDIT_SUBMITTED = "editSubmitted"
  val EDIT_PREV_RETURN = "editPrevReturn"

  def getEditSubmittedMode(propertyDetails: PropertyDetails, isFromPrevReturn: Option[Boolean] = None): Option[String] = {
    isFromPrevReturn match {
      case Some(true) => Some(EDIT_PREV_RETURN)
      case _ => propertyDetails.formBundleReturn.map(x => EDIT_SUBMITTED)
    }
  }

  def isEditSubmitted(propertyDetails: PropertyDetails): Boolean = propertyDetails.formBundleReturn.isDefined

  def isEditSubmittedMode(mode: Option[String]): Boolean = mode == Some(EDIT_SUBMITTED) || mode == Some(EDIT_PREV_RETURN)

  def isEditReturn(mode: Option[String]): Boolean = mode.contains(EDIT_SUBMITTED)

  def isPrevReturn(mode: Option[String]): Boolean = mode.contains(EDIT_PREV_RETURN)

  def getSummaryBackLink(id: String, mode: Option[String]): Option[String] =
    if (isEditReturn(mode)) {
      Some(controllers.editLiability.routes.EditLiabilitySummaryController.view(id).url)
    } else {
      Some(controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(id).url)
    }

  def getPropertyDetailsPreHeader(mode: Option[String] = None): String = {
    mode match {
      case Some(EDIT_SUBMITTED) => "ated.property-details.pre-header-change"
      case _ => "ated.property-details.pre-header"
    }
  }

  def canSubmit(periodKey: Int, currentDate: LocalDate): Boolean = {
    val currentYear = if (currentDate.getMonthOfYear >= 4) currentDate.getYear else currentDate.getYear - 1
    periodKey <= currentYear
  }

  def maskSortCode(inpSortCode: String): String = {
    val sortCode = inpSortCode.trim
    if (sortCode.length > 0 && sortCode.length >= 6)
      "XX - XX - " + sortCode.substring(sortCode.length - 2, sortCode.length)
    else ""
  }

  def maskBankDetails(accountNumber: String, pos: Int): String = {
    val accNum = accountNumber.trim
    if (accNum.length > 0 && accNum.length >= 6)
      accNum.substring(0, accNum.length - pos).replaceAll(".", "X") + accNum.substring(accNum.length - pos, accNum.length)
    else ""
  }

  // $COVERAGE-OFF$
  def createDraftId: String = {
    java.util.UUID.randomUUID.toString.take(8).toUpperCase()
  }

  // $COVERAGE-ON$

  def createLabel(str: String): String = {
    str match {
      case "sortCode.firstElement" => "First two numbers"
      case "sortCode.secondElement" => "Second two numbers"
      case "sortCode.thirdElement" => "Third two numbers"
      case _ => ""
    }
  }

  def printNotProvidedIfEmpty(str: String): String = if (str == "") "ated.property-details-summary.field-blank" else str

  def addParamsToRequest(params: Map[String, Seq[String]])(implicit request: Request[AnyContent]): Option[Map[String, Seq[String]]] = {
    request.body.asFormUrlEncoded.map(c => c ++ params)
  }

  def getEarliestDate(date1: LocalDate, date2: LocalDate) = if (date1.isBefore(date2)) date1 else date2

  def intToMonth(monthAsInt: Int): String = {
    monthAsInt match {
      case 1 =>  "January"
      case 2 =>  "February"
      case 3 =>  "March"
      case 4 =>  "April"
      case 5 =>  "May"
      case 6 =>  "June"
      case 7 =>  "July"
      case 8 =>  "August"
      case 9 =>  "September"
      case 10 => "October"
      case 11 => "November"
      case 12 => "December"
    }
  }
}
