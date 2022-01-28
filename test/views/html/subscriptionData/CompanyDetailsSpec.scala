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

package views.html.subscriptionData

import config.ApplicationConfig
import models._
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}

class CompanyDetailsSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val authMock: Enrolments ~ Some[AffinityGroup] ~ Some[String] = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
  setAuthMocks(authMock)

  val injectedViewInstance = app.injector.instanceOf[views.html.subcriptionData.companyDetails]

  "Company Details view" must {
    "have correct page title" in {
      doc.title mustBe messages("ated.company-details.title") + " - GOV.UK"
    }

    "have correct heading and caption" in {
      doc.select("h1").text must include("This section is: Manage your ATED service Your ATED details")
    }

    "have a backLink" in {
      val backLink = new CssSelector("a.govuk-back-link")
      doc must backLink
    }
  }

  "Company Details page" must {
    "display company details of the user" when {
      "have rows for all details" in {
        val rows = doc.select("dl.govuk-summary-list dt")

        rows.get(0).text mustBe messages("ated.company-details.name")
        rows.get(1).text mustBe messages("ated.company-details.ated-reference-number")
        rows.get(2).text mustBe messages("ated.company-details.registered-address")
        rows.get(3).text mustBe messages("ated.company-details.correspondence-address")
        rows.get(4).text mustBe messages("ated.company-details.contact-address")
        rows.get(5).text mustBe messages("ated.company-details.contact-preference.label")
      }



      "display First Name" in {
        doc must haveElementWithIdAndText("name1", "firstName")
      }

      "display Last Name" in {
        doc must haveElementWithIdAndText("name2", "lastName")
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
        doc.select("a.govuk-button").attr("href") mustBe "/ated/account-summary"
      }

    }

  }


  val addressDetails = AddressDetails(addressType = "", addressLine1 = "some street", addressLine2 = "some area", addressLine3 = Some("some county"), postalCode = Some("ne981zz"), countryCode = "GB")
  val contactDetails = ContactDetails(emailAddress = Some("a@b.c"))
  val correspondence = Address(Some("name1"), Some("name2"), addressDetails = addressDetails, contactDetails = Some(contactDetails))
  val businessPartnerDetails = RegisteredDetails(isEditable = false, "testName",
    RegisteredAddressDetails(addressLine1 = "bpline1",
      addressLine2 = "bpline2",
      addressLine3 = Some("bpline3"),
      addressLine4 = Some("bpline4"),
      postalCode = Some("postCode"),
      countryCode = "GB"))

  val businessPartnerDetailsEditable = RegisteredDetails(isEditable = true, "testName",
    RegisteredAddressDetails(addressLine1 = "bpline1",
      addressLine2 = "bpline2",
      addressLine3 = Some("bpline3"),
      addressLine4 = Some("bpline4"),
      postalCode = Some("postCode"),
      countryCode = "GB"))



  override def view: Html = injectedViewInstance(Some(correspondence),
    Some(businessPartnerDetails), emailConsent = true, None, None, Html(""), Some("http://backLink"))
}
