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

package models

import play.api.data.Form
import play.api.data.Forms._

case class LeaveFeedback(summaryInfo: String, moreInfo: String, experienceLevel: Option[Int])

object LeaveFeedback {

  val maxStringLength = 1200
  val maxOptionSize = 4
  val lengthZero = 0

  val form = Form(mapping(
    "summaryInfo" -> text
      .verifying("ated.leave-feedback.error.mandatory", x => x.length > lengthZero),
    "moreInfo" -> text
      .verifying("ated.leave-feedback.error.mandatory", x => x.length > lengthZero),
    "experienceLevel" -> optional(number(0, maxOptionSize))
      .verifying("ated.leave-feedback.experience-error", x => x.isDefined)
  )(LeaveFeedback.apply)(LeaveFeedback.unapply))

}
