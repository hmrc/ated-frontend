/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.viewhelper

import utils.AtedConstants

object EditLiability {

  def createHeadermessages(returnType: String, prefix: String): String = returnType match {
    case AtedConstants.Further => s"$prefix.further"
    case AtedConstants.Amend => s"$prefix.amend"
    case  _ => s"$prefix.change"
  }

  def returnTypeFromAmount(amount: BigDecimal): String = amount.signum match {
    case 0 => AtedConstants.Change
    case 1 => AtedConstants.Further
    case -1 => AtedConstants.Amend
  }

}
