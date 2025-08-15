/*
 * Copyright 2025 HM Revenue & Customs
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
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import views.html.editLiability.ukBankDetails

class UkBankDetailsSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach
  with GivenWhenThen with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedViewInstance: ukBankDetails = app.injector.instanceOf[views.html.editLiability.ukBankDetails]

  Feature("The user can enter their UK bank account details") {

    info("as a client i want enter my UK bank account details")

    Scenario("allow entering bank details") {

      Given("the client is prompted to add their UK bank account details")
      When("The user views the page")

      val html = injectedViewInstance(bankDetailsForm, "1", Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Is the bank account in the UK?")
      assert(document.title() === "Enter your bank or building society account details - Submit and view your ATED returns - GOV.UK")
      assert(document.select("h1").text.contains("Enter your bank or building society account details"))

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text.contains("This section is: Change return"))

      Then("The fields should have the correct titles")
      And("No data is populated")

      assert(document.getElementById("name-of-person").text() === "Name on the account")

      assert(document.getElementsByAttributeValue("for" ,"sortCode").text() === "Sort code")
      assert(document.getElementById("sortCode-hint").text() === "Must be 6 digits long")
      assert(document.getElementById("sortCode").attr("type") === "text")
      assert(document.getElementById("sortCode").attr("inputmode") === "numeric")

      assert(document.getElementsByAttributeValue("for","accountNumber").text() === "Account number")
      assert(document.getElementById("accountNumber-hint").text() === "Must be between 6 and 8 digits long")
      assert(document.getElementById("accountNumber").attr("type") === "text")
      assert(document.getElementById("accountNumber").attr("inputmode") === "numeric")

      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }

}
