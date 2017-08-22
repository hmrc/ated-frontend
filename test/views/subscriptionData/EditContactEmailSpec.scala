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

package views.subscriptionData

import java.util.UUID

import builders.AuthBuilder._
import forms.AtedForms.editContactDetailsEmailForm
import models._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class EditContactEmailSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{


  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The client can view their email details") {

    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))

    info("as a client I want to view my email details")

    scenario("user visits the page with email settings so they can edit them") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val html = views.html.subcriptionData.editContactEmail(editContactDetailsEmailForm, Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("Edit your ATED email address")
      assert(document.title() === "Edit your ATED email address")
      assert(document.getElementById("contact-details-email-header").text() === "Edit your ATED email address")

      And("The pre-header text is - Manage your ATED service")
      assert(document.getElementById("pre-header").text() === "This section is: Manage your ATED service")

      And("The the field names are correct")
      assert(document.getElementById("emailConsent-true_field").text() === "Yes")
      assert(document.getElementById("emailConsent-false_field").text() === "No")
      assert(document.getElementById("emailAddress_field").text() === "Email")

      And("The consent text is correct")
      assert(document.getElementById("lede").text() === "If we can use email rather than letter there will be less delays in dealing with enquiries.")

      assert(document.getElementById("email-risk-question").text() === "What are the risks of email and why we need your consent")
      assert(document.getElementById("email-consent-content-0").text().
        contains("HMRC may need to send information to you by email.") === true)

      assert(document.getElementById("email-consent-content-0").text().
        contains("Any emails from HMRC are not secure and any information you send to us by email is at your own risk. In order for us to send emails to you, you must confirm (for yourself or by your agent) that you are happy with this and that you understand the risks of sending information containing your personal details by email.") === true)

      assert(document.getElementById("email-consent-content-0").text().
        contains("Please note: If a response contains any confidential information, we will only reply by letter or telephone. Letters may lead to delays in dealing with enquiries. If you think you have been sent a suspicious email, do not follow any links within the email or disclose any personal details. Forward any suspicious emails to") === true)

      assert(document.getElementById("email-consent-content-0").text().
        contains("phishing@hmrc.gsi.gov.uk") === true)

      And("The the submit button is - Save changes")
      assert(document.getElementById("submit").text() === "Save changes")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }

  }
}
