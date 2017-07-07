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

package views.editLiability

import java.util.UUID

import builders.AuthBuilder._
import forms.BankDetailForms._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class disposeLiabilityBankDetailsSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
  val userId = s"user-${UUID.randomUUID}"
  implicit val user = createAtedContext(createUserAuthContext(userId, "name"))

  feature("The user can whether they have bank details") {

    info("as a client i want change whether I send my bank details")

    scenario("allow indicating bank details status") {

      Given("the client is prompted to add thier bank details")
      When("The user views the page")

      val html = views.html.editLiability.disposeLiabilityBankDetails(bankDetailsForm, "1", Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Are your bank details for a UK bank account?")
      assert(document.title() === "Are your bank details for a UK bank account?")
      assert(document.select("h1").text === "Are your bank details for a UK bank account?")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "Change return")

      Then("The fields should have the correct titles")
      And("No data is populated")

      assert(document.getElementById("hasUKBankAccount-id").text() === "Are your bank details for a UK bank account? Yes No")
      assert(document.getElementById("name-of-person").text() === "Name of bank account holder")

      assert(document.getElementById("hidden-bank-details-uk").text() === "Account number Sort code id id id  ")
      assert(document.getElementById("account-number").text() === "Account number")
      assert(document.getElementById("sort-code").text() === "Sort code id id id  ")

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
