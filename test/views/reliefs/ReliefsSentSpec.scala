/*
 * Copyright 2020 HM Revenue & Customs
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
import models.{ReliefReturnResponse, StandardAuthRetrievals, SubmitReturnsResponse}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import testhelpers.MockAuthUtil

class ReliefsSentSpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val periodKey = 2015

  feature("The user can view the relief sent page") {

    info("as a client I want to be able to see that my relief return has been submitted successfully")

    scenario("show the relief sent page") {

      Given("the client submits a relief return")
      When("the return is successfully received")

      val html = views.html.reliefs.reliefsSent(
        periodKey,
        SubmitReturnsResponse(
          LocalDate.now().toString,
          Some(Seq(ReliefReturnResponse("Test Description", "123456789"))),
          None
        )
      )

      val document = Jsoup.parse(html.toString())

      Then("The header should be correct")
      assert(document.select("h1").text === "Your returns have been successfully submitted")

      Then("The first paragraph should be correct")
      assert(document.select("#completed-returns").text === "You can view your completed returns, " +
        "payment references and ways to pay in the ATED online service.")

      Then("The second paragraph should be correct")
      assert(document.select("#email-confirmation").text === "You will not receive an email confirmation.")

      Then("There will be a link to print confirmation")
      val link = document.select("#print-friendly-relief-link")
      assert(link.text === "Print confirmation")
      assert(link.attr("href").contains("/ated/reliefs/2015/sent-reliefs-print"))

      Then("The first h2 should be correct")
      assert(document.select("h2").first().text === "The ATED charge for these returns is £0")

      Then("The sentence about payments made should be correct")
      assert(document.select("#amount-message").text === "This amount does not reflect any payments you " +
        "have already made or penalties that have been issued.")

      Then("The sentence including the returns reference should be correct")
      assert(document.select("#reference-number").text === "Your reference for these returns are 123456789.")

      Then("The sentence about balance should be correct")
      assert(document.select("#balance-message").text === "You can view your balance in your ATED online " +
        "service. There can be a 24-hour delay before you see any updates.")

      Then("The second h2 should be correct")
      assert(document.select("#content > article > h2:nth-child(11)")
        .text === "Change or ending of relief type")

      Then("The paragraph about change in circumstances should be correct")
      assert(document.select("#content > article > p:nth-child(12)").text === "If any change in your " +
        "circumstances means you will no longer claim for one or more relief types next year, you need to contact " +
        "HMRC on atedadditionalinfo.ctiaa@hmrc.gov.uk to tell us which relief types you will not claim.")

      Then("The sentence about keeping records up to date should be correct")
      assert(document.select("#content > article > p:nth-child(13)").text === "This will help to keep " +
        "our records up to date so we know not to expect a return for that type of relief next year.")

      Then("The sentence about emailing should be correct")
      assert(document.select("#content > article > p:nth-child(14)").text === "When emailing please " +
        "include your ATED reference number or if you do not have the number please give your company name. Do not " +
        "include any further personal or financial details. Sending information over the internet is generally not " +
        "completely secure, and we cannot guarantee the security of your data while it’s in transit. Any data you " +
        "send is at your own risk.")

      Then("There will be a button which takes the user to their ATED summary")
      val button = document.select(".button")
      assert(button.text === "Your ATED summary")
      assert(button.attr("href").contains("/ated/account-summary"))
    }

  }
}
