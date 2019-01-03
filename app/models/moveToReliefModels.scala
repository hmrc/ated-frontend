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

package models

import play.api.libs.json.Json

case class MoveToRelief(reliefType: Option[String] = None)

object MoveToRelief {
  implicit val formats = Json.format[MoveToRelief]
}

case class MoveToReliefCalculated(liabilityAmount: BigDecimal, amountDueOrRefund: BigDecimal)

object MoveToReliefCalculated {
  implicit val formats = Json.format[MoveToReliefCalculated]
}

case class MoveToReliefData(id: String,
                            formBundleReturn: FormBundleReturn,
                            moveToRelief: Option[MoveToRelief] = None,
                            periodDetails: Option[PropertyDetailsPeriod] = None,
                            calculated: Option[MoveToReliefCalculated] = None,
                            bankDetails: Option[BankDetailsModel] = None)

object MoveToReliefData {
  implicit val formats = Json.format[MoveToReliefData]
}
