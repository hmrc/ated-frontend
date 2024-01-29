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

case class ReliefReturnResponse(reliefDescription: String, formBundleNumber: String)

object ReliefReturnResponse {
  implicit val formats: OFormat[ReliefReturnResponse] = Json.format[ReliefReturnResponse]
}

case class LiabilityReturnResponse(
                                    mode: String,
                                    propertyKey: String,
                                    liabilityAmount: BigDecimal,
                                    paymentReference: Option[String],
                                    formBundleNumber: String
                                    )

object LiabilityReturnResponse {
  implicit val formats: OFormat[LiabilityReturnResponse] = Json.format[LiabilityReturnResponse]
}


case class SubmitReturnsResponse(
                                  processingDate: String,
                                  reliefReturnResponse: Option[Seq[ReliefReturnResponse]] = None,
                                  liabilityReturnResponse: Option[Seq[LiabilityReturnResponse]] = None
                                  )

object SubmitReturnsResponse {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ" //DateTime
  implicit val jodaDateTimeReads: Reads[DateTime] = JodaReads.jodaDateReads(dateFormat)
  implicit val jodaDateTimeWrites: Writes[DateTime] = JodaWrites.jodaDateWrites(dateFormat)
  implicit val formats: OFormat[SubmitReturnsResponse] = Json.format[SubmitReturnsResponse]
}

case class AlreadySubmittedReturnsResponse(
                                  reliefReturnResponse: Option[Seq[ReliefReturnResponse]] = None,
                                  liabilityReturnResponse: Option[Seq[LiabilityReturnResponse]] = None
                                )

object AlreadySubmittedReturnsResponse {
  implicit val formats: OFormat[AlreadySubmittedReturnsResponse] = Json.format[AlreadySubmittedReturnsResponse]
}
