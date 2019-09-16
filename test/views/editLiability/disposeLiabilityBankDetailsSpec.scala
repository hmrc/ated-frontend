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

package views.editLiability

import forms.BankDetailForms._
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.MockAuthUtil

class disposeLiabilityBankDetailsSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
  val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
  setAuthMocks(authMock)
  implicit val authContext = organisationStandardRetrievals

    feature("The user can whether they have bank details") {

    info("as a client i want change whether I send my bank details")

    scenario("allow indicating bank details status") {

      Given("the client is prompted to add thier bank details")
      When("The user views the page")

      val html = views.html.editLiability.disposeLiabilityBankDetails(bankDetailsForm, "1", Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Is the bank account in the UK?")
      assert(document.title() === "Is the bank account in the UK? - GOV.UK")
      assert(document.select("h1").text === "Is the bank account in the UK?")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      Then("The fields should have the correct titles")
      And("No data is populated")

      assert(document.getElementById("hasUKBankAccount-id").text() === "Is the bank account in the UK? Yes No")
      assert(document.getElementById("name-of-person").text() === "Name of bank account holder")

      assert(document.getElementById("hidden-bank-details-uk").text() === "Account number Sort code First two numbers Second two numbers Third two numbers  ")
      assert(document.getElementById("account-number").text() === "Account number")
      assert(document.getElementById("sort-code").text() === "Sort code First two numbers Second two numbers Third two numbers  ")

      assert(document.getElementById("hidden-bank-details-non-uk").text() === "IBAN SWIFT code")
      assert(document.getElementById("iban-code").text() === "IBAN")
      assert(document.getElementById("bic-swift-code").text() === "SWIFT code")

      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }

}
