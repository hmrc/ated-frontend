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

package views.html.subscriptionData

import config.ApplicationConfig
import forms.AtedForms.editContactDetailsEmailForm
import models._
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class EditContactEmailSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {
  implicit lazy val authContxt: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance = app.injector.instanceOf[views.html.subcriptionData.editContactEmail]
"Edit contact email view" must {

  "have correct page title" in {
    doc.title mustBe messages("ated.contact-details-edit-email.title") + " - GOV.UK"
  }

  "have correct heading and caption" in {
    doc.select("h1").text must include("This section is: Manage your ATED service Edit your ATED email address")
  }

  "have a backLink" in {
    val backLink = new CssSelector("a.govuk-back-link")
    doc must backLink
  }
    }

  "Edit contact email page" must {

    "display info text correctly" in {
      doc must haveElementWithIdAndText(messages("ated.contact-details-email-lede"), "lede")
    }

    "display email risk help text correctly" in {
      doc.getElementsByClass("govuk-details__summary-text").text mustBe messages("ated.contact-details.email.risk.help.text")
    }

    "display email consent Yes correctly" in {
      doc.getElementsByAttributeValue("for", "emailConsent").text mustBe "Yes"
    }

    "display email consent No correctly" in {
      doc.getElementsByAttributeValue("for", "emailConsent-2").text mustBe "No"
    }

    "display correct value in the email field" in {
      doc must haveValueElement("emailAddress", "test@test.com")
    }

    "display submit button" in {
      doc.getElementsByClass("govuk-button").text mustBe messages("ated.save-changes")
    }

    "display correct submit form url" in {
      doc must haveFormWithSubmitUrl("/ated/edit-contact-email")
    }

  }

  val prePopulatedData = EditContactDetailsEmail(emailAddress = "test@test.com",
    emailConsent = true
  )
  override def view: Html = injectedViewInstance(editContactDetailsEmailForm.fill(prePopulatedData), Html(""), Some("http://backLink"))

}
