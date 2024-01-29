/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.DateTime
import play.api.libs.json.{JodaReads, JodaWrites, Json, OFormat, Reads, Writes}

case class EditLiabilityReturnsResponse(mode: String,
                                        oldFormBundleNumber: String,
                                        formBundleNumber: Option[String],
                                        liabilityAmount: BigDecimal,
                                        amountDueOrRefund: BigDecimal,
                                        paymentReference: Option[String])

object EditLiabilityReturnsResponse {
  implicit val formats: OFormat[EditLiabilityReturnsResponse] = Json.format[EditLiabilityReturnsResponse]
}

case class EditLiabilityReturnsResponseModel(processingDate: DateTime,
                                             liabilityReturnResponse: Seq[EditLiabilityReturnsResponse],
                                             accountBalance: BigDecimal)

object EditLiabilityReturnsResponseModel {
  implicit val yourJodaDateWrites: Writes[DateTime] = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss'Z'") // DateTime
  implicit val yourJodaDateReads: Reads[DateTime] = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'") // DateTime
  implicit val formats: OFormat[EditLiabilityReturnsResponseModel] = Json.format[EditLiabilityReturnsResponseModel]
}
