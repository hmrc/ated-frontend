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

class disposeLiabilityHasBankDetailsSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
  val userId = s"user-${UUID.randomUUID}"
  implicit val user = createAtedContext(createUserAuthContext(userId, "name"))

  feature("The user can whether they have bank details") {

    info("as a client i want change whether I send my bank details")

    scenario("allow indicating bank details status") {

      Given("the client is prompted to add thier bank details")
      When("The user views the page")

      val html = views.html.editLiability.disposeLiabilityHasBankDetails(hasBankDetailsForm, "1", Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Do you have the bank details for a repayment?")
      assert(document.title() === "Do you have the bank details for a repayment?")
      assert(document.select("h1").text === "Do you have the bank details for a repayment?")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "Change return")

      Then("The text should be - We need bank details for the payment of any refunds that may be due.")
      assert(document.getElementById("has-bank-details-text").text() === "We need bank details for the payment of any refunds that may be due.")

      Then("The date fields should have the correct titles")
      And("No data is populated")
      assert(document.getElementById("hasBankDetails-id").text() === "Do you have the bank details for a repayment? Yes No")
      assert(document.getElementById("hasBankDetails-true").text() === "")
      assert(document.getElementById("hasBankDetails-false").text() === "")

      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }

}
