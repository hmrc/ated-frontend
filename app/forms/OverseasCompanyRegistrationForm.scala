/*
 * Copyright 2017 HM Revenue & Customs
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

import models.OverseasCompanyRegistration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object OverseasCompanyRegistrationForm {

  private val length60 = 60
  private val length40 = 40

  val overseasCompanyRegistrationForm = Form(
    mapping(
      "businessUniqueId" -> optional(text)
        .verifying(Messages("ated.non-uk-reg.businessUniqueId.length", length60), x => x.isEmpty || (x.nonEmpty && x.get.length <= length60)),
      "issuingInstitution" -> optional(text)
        .verifying(Messages("ated.non-uk-reg.issuingInstitution.length", length40), x => x.isEmpty || (x.nonEmpty && x.get.length <= length40)),
      "countryCode" -> optional(text)
    )(OverseasCompanyRegistration.apply)(OverseasCompanyRegistration.unapply)
  )
}