/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.AtedForms.editContactDetailsForm
import models._
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class EditContactDetailsSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance = app.injector.instanceOf[views.html.subcriptionData.editContactDetails]
"Edit contact details view" must {
      behave like pageWithTitle(messages("ated.contact-details.title"))
      behave like pageWithHeader(messages("ated.contact-details.header"))
      behave like pageWithPreHeading(messages("ated.contact-details-edit.subheader"))
      behave like pageWithBackLink
    }

  "Edit contact details page" must {

    "display info text correctly" in {
      doc must haveElementWithIdAndText(messages("ated.contact-details.subheader"), "contact-details-subheader")
    }
    "display first name label correctly" in {
      doc must haveElementWithIdAndText(messages("ated.contact-details.firstName"), "firstName_field")
    }
    "display correct value in the first name field" in {
      doc must haveValueElement("firstName", "Y")
    }
    "display last name label correctly" in {
      doc must haveElementWithIdAndText(messages("ated.contact-details.lastName"), "lastName_field")
    }
    "display correct value in the last name field" in {
      doc must haveValueElement("lastName", "Z")
    }
    "display telephone label correctly" in {
      doc must haveElementWithIdAndText(messages("ated.contact-details.phoneNumber"), "phoneNumber_field")
    }
    "display correct value in the phone number field" in {
      doc must haveValueElement("phoneNumber", "0191")
    }

    "display submit button" in {
      doc must haveSubmitButton(messages("ated.save-changes"))
    }

    "display correct submit form url" in {
      doc must haveFormWithSubmitUrl("/ated/contact-address")
    }
  }

  val prePopulatedData = EditContactDetails(firstName = "Y",
    lastName = "Z",
    phoneNumber = "0191"
  )
  override def view: Html = injectedViewInstance(editContactDetailsForm.fill(prePopulatedData), Html(""), Some("http://backLink"))

}
