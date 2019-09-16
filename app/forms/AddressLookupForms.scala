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

package forms

import models._
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

object AddressLookupForms {

  val addressLookupForm = Form(
    mapping(
      "postcode" -> text
        .verifying(Messages("ated.error.address.postalcode.format", Messages("ated.address.postcode.field")),
          x => AtedForms.validatePostCodeFormat(Some(x.toUpperCase))),
      "houseName" -> optional(text)
    )(AddressLookup.apply)(AddressLookup.unapply)
  )

  val addressSelectedForm = Form(mapping(
      "selected" -> optional(text).verifying(Messages("ated.address-lookup.error.non-selected.field"), _.isDefined)
    )(AddressSelected.apply)(AddressSelected.unapply))

}
