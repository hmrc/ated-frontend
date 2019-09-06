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

import forms.AtedForms.editContactDetailsEmailForm
import models._
import org.scalatest.mockito.MockitoSugar
import play.twirl.api.Html
import utils.MockAuthUtil
import utils.viewHelpers.AtedViewSpec

class EditContactEmailSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {
  implicit lazy val authContext = organisationStandardRetrievals


  "Edit contact email view" must {
      behave like pageWithTitle(messages("ated.contact-details-edit-email.title"))
      behave like pageWithHeader(messages("ated.contact-details-edit-email.header"))
      behave like pageWithPreHeading(messages("ated.contact-details-edit-email.subheader"))
      behave like pageWithBackLink
    }

  "Edit contact email page" must {

    "display info text correctly" in {
      doc must haveElementWithIdAndText(messages("ated.contact-details-email-lede"), "lede")
    }

    "display email risk help text correctly" in {
      doc must haveElementWithIdAndText(messages("ated.contact-details.email.risk.help.text"), "email-risk-question")
    }

    "display email consent Yes correctly" in {
      doc must haveElementWithIdAndText(messages("Yes"), "emailConsent-true_field")
    }

    "display email consent No correctly" in {
      doc must haveElementWithIdAndText(messages("No"), "emailConsent-false_field")
    }

    "display correct value in the email field" in {
      doc must haveValueElement("emailAddress", "test@test.com")
    }

    "display submit button" in {
      doc must haveSubmitButton(messages("ated.save-changes"))
    }

    "display correct submit form url" in {
      doc must haveFormWithSubmitUrl("/ated/edit-contact-email")
    }

  }

  val prePopulatedData = EditContactDetailsEmail(emailAddress = "test@test.com",
    emailConsent = true
  )
  override def view: Html = views.html.subcriptionData.editContactEmail(editContactDetailsEmailForm.fill(prePopulatedData), Some("http://backLink"))

}
