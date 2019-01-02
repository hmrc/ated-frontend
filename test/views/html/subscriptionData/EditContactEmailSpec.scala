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

package views.html.subscriptionData

import forms.AtedForms.editContactDetailsForm
import models._
import play.twirl.api.Html
import java.util.UUID

import builders.AuthBuilder._
import utils.viewHelpers.AtedViewSpec
import forms.AtedForms.editContactDetailsEmailForm

class EditContactEmailSpec extends AtedViewSpec {

    "Edit contact email view" must {
      behave like pageWithTitle(messages("ated.contact-details-edit-email.title"))
      behave like pageWithHeader(messages("ated.contact-details-edit-email.header"))
      behave like pageWithPreHeading(messages("ated.contact-details-edit-email.subheader"))
      behave like pageWithBackLink
    }


  override def view: Html = views.html.subcriptionData.editContactEmail(editContactDetailsEmailForm, Some("http://backLink"))

}
