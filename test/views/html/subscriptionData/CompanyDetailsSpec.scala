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
import models._
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}
import views.html.subcriptionData.companyDetails

class CompanyDetailsSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val authMock: Enrolments ~ Some[AffinityGroup] ~ Some[String] = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
  setAuthMocks(authMock)

  val injectedViewInstance: companyDetails = app.injector.instanceOf[views.html.subcriptionData.companyDetails]

  "Company Details view" must {
    behave like pageWithTitle(messages("ated.company-details.title"))
    behave like pageWithHeader(messages("ated.company-details.header"))
    behave like pageWithPreHeading(messages("ated.company-details.preheader"))
    behave like pageWithBackLink
  }

  "Company Details page" must {
    "display company details of the user" when {
      "display name label correctly" in {
        doc must haveElementWithIdAndText(messages("ated.company-details.name"), "company-name-header")
      }

      "display ated reference number" in {
        doc must haveElementWithIdAndText(messages("ated.company-details.ated-reference-number"), "ated-reference-number")
      }

      "display ated reference value" in {
        doc must haveElementWithIdAndText("XN1200000100001", "ated-reference-number-val")
      }

      "display registered address" in {
        doc must haveElementWithIdAndText(messages("ated.company-details.registered-address"), "registered-address-label")
      }

      "display correspondence address" in {
        doc must haveElementWithIdAndText(messages("ated.company-details.correspondence-address"), "correspondence-address-label")
      }

      "display address line one" in {
        doc must haveElementWithIdAndText("some street", "line_1")
      }

      "display address line two" in {
        doc must haveElementWithIdAndText("some area", "line_2")
      }

      "display address line three" in {
        doc must haveElementWithIdAndText("some county", "line_3")
      }

      "display postCode" in {
        doc must haveElementWithIdAndText("ne981zz", "postcode")
      }

      "display Country Code" in {
        doc must haveElementWithIdAndText("United Kingdom", "countryCode")
      }

      "display ATED contact details" in {
        doc must haveElementWithIdAndText(messages("ated.company-details.contact-address"), "contact-details-label")
      }


      "display First Name" in {
        doc must haveElementWithIdAndText("name1", "firstName")
      }

      "display Last Name" in {
        doc must haveElementWithIdAndText("name2", "lastName")
      }

      "display ATED email address" in {
        doc must haveElementWithIdAndText(messages("ated.company-details.contact-preference.label"), "contact-pref-label")
        doc must haveElementWithIdAndText("ated@b.c", "contact-pref-val")
      }

      "display edit link for correspondence address" in {
        doc must haveLinkWithUrlWithID("correspondence-edit", "/ated/correspondence-address")
      }

      "display edit link for contact details" in {
        doc must haveLinkWithUrlWithID("contactdetails-edit", "/ated/edit-contact")
      }

      "display edit link for contact email" in {
        doc must haveLinkWithUrlWithID("contactemail-edit", "/ated/edit-contact-email")
      }

      "display Back to your ATED summary link" in {
        doc must haveLinkWithUrlWithID("back", "/ated/account-summary")
      }

    }

    "display Agent name and edit link" in {
      doc must haveElementWithIdAndText(messages("ated.company-details.appointed_agent"), "appointed-agent-label")
      doc must haveElementWithIdAndText("agent", "appointed-agent-val")
      doc must haveLinkWithUrlWithID("appointed-agent-link", "/changeAgentLink")
    }

    "display Agent email address and edit link" in {
      doc must haveElementWithIdAndText(messages("ated.company-details.agent-email-address"), "email-label")
      doc must haveElementWithIdAndText("agent@b.c", "email-val")
      doc must haveLinkWithUrlWithID("email-link", "/changeEmailLink")
    }
  }


  val addressDetails: AddressDetails = AddressDetails(addressType = "", addressLine1 = "some street", addressLine2 = "some area",
    addressLine3 = Some("some county"), postalCode = Some("ne981zz"), countryCode = "GB")

  val contactDetails: ContactDetails = ContactDetails(emailAddress = Some("ated@b.c"))
  val correspondence: Address = Address(Some("name1"), Some("name2"), addressDetails = addressDetails, contactDetails = Some(contactDetails))
  val businessPartnerDetails: RegisteredDetails = RegisteredDetails(isEditable = false, "testName",
    RegisteredAddressDetails(addressLine1 = "bpline1",
      addressLine2 = "bpline2",
      addressLine3 = Some("bpline3"),
      addressLine4 = Some("bpline4"),
      postalCode = Some("postCode"),
      countryCode = "GB"))

  val businessPartnerDetailsEditable: RegisteredDetails = RegisteredDetails(isEditable = true, "testName",
    RegisteredAddressDetails(addressLine1 = "bpline1",
      addressLine2 = "bpline2",
      addressLine3 = Some("bpline3"),
      addressLine4 = Some("bpline4"),
      postalCode = Some("postCode"),
      countryCode = "GB"))

  val clientMandateDetails : Option[ClientMandateDetails] = Some(ClientMandateDetails(
    agentName = "agent", changeAgentLink = "/changeAgentLink", email = "agent@b.c", changeEmailLink = "/changeEmailLink"
  ))

  override def view: Html = injectedViewInstance(Some(correspondence),
    Some(businessPartnerDetails), emailConsent = true, clientMandateDetails, None, Html(""), Some("http://backLink"))
}
