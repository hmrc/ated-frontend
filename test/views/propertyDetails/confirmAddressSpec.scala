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

package views.propertyDetails

import builders.PropertyDetailsBuilder
import config.ApplicationConfig
import models.{PropertyDetailsAddress, StandardAuthRetrievals}
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
import views.html.propertyDetails.confirmAddress

class confirmAddressSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance: confirmAddress = app.injector.instanceOf[views.html.propertyDetails.confirmAddress]

  Feature("The user can view their property address details before they confirm and continue") {

    info("as a client I want to view my property address details")

    Scenario("return the property address") {

      Given("the client has entered an address")
      When("The user views the confirm address page")
      val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(postCode = Some("XX1 1XX"))
      val html = injectedViewInstance("1", 2015, propertyDetails, mode = None, Html(""), Some("http://backLink"))
      val document = Jsoup.parse(html.toString())
      Then("The title should match - Confirm address - GOV.UK")
      assert(document.title() === "Confirm address - GOV.UK")

      Then("The header should match - Confirm address")
      assert(document.getElementsByTag("h1").text() contains "Confirm address")

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return")

      assert(document.getElementById("address").text() contains  "addr1")
      assert(document.getElementById("address").text() contains "addr2")
      assert(document.getElementById("address").text() contains "addr3")
      assert(document.getElementById("address").text() contains "addr4")
      assert(document.getElementById("address").text() contains "XX1 1XX")
      assert(document.getElementById("edit-address-link").text() === "Edit address")
      assert(document.getElementsByClass("govuk-button").text() === "Confirm and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }
}
