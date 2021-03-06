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

package views.editLiability

import config.ApplicationConfig
import forms.BankDetailForms._
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil

class disposeLiabilityHasBankDetailsSpec extends FeatureSpec with GuiceOneAppPerSuite
  with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedViewInstance = app.injector.instanceOf[views.html.editLiability.disposeLiabilityHasBankDetails]

  feature("The user can whether they have bank details") {

    info("as a client i want change whether I send my bank details")

    scenario("allow indicating bank details status") {

      Given("the client is prompted to add thier bank details")
      When("The user views the page")

      val html = injectedViewInstance(hasBankDetailsForm, "1", Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Do you have the bank details for a repayment?")
      assert(document.title() === "Do you have a bank account where we could pay a refund? - GOV.UK")
      assert(document.select("h1").text === "Do you have a bank account where we could pay a refund?")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      Then("The date fields should have the correct titles")
      And("No data is populated")
      assert(document.getElementById("hasBankDetails-id").text() === "Yes No")
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
