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

package views.reliefs

import config.ApplicationConfig
import forms.AtedForms.editReliefForm
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
import views.html.reliefs.changeReliefReturn


class changeReliefReturnSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance: changeReliefReturn = app.injector.instanceOf[views.html.reliefs.changeReliefReturn]

  Feature("The user can change their relief return") {

    info("as a client i want to be able to change my relief return")

    Scenario("allow editing of a relief return") {

      Given("the client has clicked change on a relief")
      When("The user views the page")

      val html = injectedViewInstance(2015, "form-bundle-123", editReliefForm,  Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header and title should match - What would you like to do with your ATED return?")
      assert(document.title() === "What would you like to do with your ATED return? - GOV.UK")
      assert(document.select("h1").text contains "What would you like to do with your ATED return?")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")


      Then("The text fields should match")
      assert(document.getElementById("relief-return-change-text").text() === "If you move into charge for all or part of the period, you need to create a chargeable return for that property.")
      assert(document.getElementById("relief-return-change-text-2").text() === "If you are changing to a new relief type for one or more of your properties you need to select change the return details and make a claim for the new relief.")
      assert(document.getElementsByClass("govuk-details__summary-text").text() === "Ending a relief claim")
      assert(document.getElementById("titleNumber-text").text() === "If any change in your circumstances means you will no longer claim for one or more relief types next year, you need to contact HMRC on atedadditionalinfo.ctiaa@hmrc.gov.uk to tell us which relief types you will not claim.")
      assert(document.getElementById("titleNumber-text-2").text() === "This will help to keep our records up to date so we know not to expect a return for that type of relief next year.")
      assert(document.getElementById("titleNumber-text-3").text() === "When emailing please include your ATED reference number or if you do not have the number please give your company name. Do not include any further personal or financial details. Sending information over the internet is generally not completely secure, and we cannot guarantee the security of your data while it’s in transit. Any data you send is at your own risk.")
      assert(document.getElementsByAttributeValue("for", "changeRelief").text() === "Change an existing ATED return")
      assert(document.getElementsByAttributeValue("for", "changeRelief-2").text() === "Create a chargeable return")


      Then("The submit button should have the correct name")
      assert(document.getElementsByClass("govuk-button").text() === "Continue")


      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
    }
  }
}
