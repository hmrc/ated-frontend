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

import forms.AtedForms._
import play.api.data.validation.Constraint
import uk.gov.hmrc.play.mappers.StopOnFirstFail
import uk.gov.hmrc.play.mappers.StopOnFirstFail.constraint
import utils.AtedUtils

object FormHelper {

  def validateFormAddressLine(mandatoryMsg: String, lengthMsg: String, validMsg: String): Constraint[String] = {
    StopOnFirstFail(
      constraint[String](mandatoryMsg, x => checkBlankFieldLength(x)),
      constraint[String](lengthMsg, x => x.isEmpty || (x.nonEmpty && x.length <= addressLineLength)),
      constraint[String](validMsg, x => x.trim.matches(AddressRegex)))
  }

  def validateFormOptionalAddressLine(lengthMsg: String, validMsg: String): Constraint[Option[String]] = {
    StopOnFirstFail(
      constraint[Option[String]](lengthMsg, x => checkFieldLengthIfPopulated(x, addressLineLength)),
      constraint[Option[String]](validMsg, x => x.isEmpty || x.fold(false)(_.trim.matches(AddressRegex))))
  }

  def validateBusinessname(mandatoryMsg: String, lengthMsg: String, validMsg: String): Constraint[String] = {
    StopOnFirstFail(
      constraint[String](mandatoryMsg, x => checkBlankFieldLength(x)),
      constraint[String](lengthMsg, x => x.isEmpty || (x.nonEmpty && x.length <= businessNameLength)),
      constraint[String](validMsg, x => x.trim.matches(businessNameRegex)))
  }

  def validateFormPostCode(lengthMsg: String, validMsg: String): Constraint[Option[String]] = {
    StopOnFirstFail(
      constraint[Option[String]](lengthMsg, x => checkFieldLengthIfPopulated(x, postcodeLength)),
      constraint[Option[String]](validMsg, x => validatePostCodeFormat(AtedUtils.formatPostCode(x))))
  }

}
