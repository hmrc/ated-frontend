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
import forms.ReliefForms.reliefsForm
import models.{Reliefs, StandardAuthRetrievals}
import java.time.LocalDate
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
import utils.PeriodUtils
import views.html.reliefs.chooseReliefs
import views.formatDate

class chooseReliefsSpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance: chooseReliefs = app.injector.instanceOf[views.html.reliefs.chooseReliefs]

  val periodKey = 2015

  Feature("The user can view the choose reliefs page") {

    info("as a client I want to be able to select reliefs for my properties")

    Scenario("show the reliefs we can choose") {

      Given("the client is creating a new relief and want to see the options")
      When("The user views the page")

      val html = injectedViewInstance(periodKey, reliefsForm, LocalDate.parse("2015-04-01"), Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - What reliefs are you claiming?")
      assert(document.getElementsByTag("h1").text contains "What reliefs are you claiming?")

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return")

      Then("The the text on the screen should be correct")
      assert(document.getElementById("lede-text").text() === "You can select more than one relief code. A single relief code can cover one or more properties.")
      assert(document.getElementById("choose-reliefs-label").text() === "Select all reliefs that apply")
      assert(document.getElementsByAttributeValue("for", "rentalBusiness").text() === "Rental businesses")
      assert(document.select("#conditional-rentalBusiness > div > fieldset > legend").text() === "When did the Rental business start?")
      assert(document.getElementById("rentalBusinessDate-hint")
        .text() ===s"For example, ${formatDate(PeriodUtils.periodStartDate(periodKey), messages("ated.date-format.numeric"))}")
      assert(document.getElementsByAttributeValue("for", "openToPublic").text() === "Open to the public")
      assert(document.select("#conditional-openToPublic > div > fieldset > legend").text() === "When did the Open to the public start?")
      assert(document.getElementById("openToPublicDate-hint")
        .text() ===s"For example, ${formatDate(PeriodUtils.periodStartDate(periodKey), messages("ated.date-format.numeric"))}")
      assert(document.getElementsByAttributeValue("for", "propertyDeveloper").text() === "Property developers")
      assert(document.select("#conditional-propertyDeveloper > div > fieldset > legend").text() === "When did the Property developer start?")
      assert(document.getElementById("propertyDeveloperDate-hint")
        .text() ===s"For example, ${formatDate(PeriodUtils.periodStartDate(periodKey), messages("ated.date-format.numeric"))}")
      assert(document.getElementsByAttributeValue("for", "propertyTrading").text() === "Property trading")
      assert(document.select("#conditional-propertyTrading > div > fieldset > legend").text() === "When did the Property trading start?")
      assert(document.getElementById("propertyTradingDate-hint")
        .text() ===s"For example, ${formatDate(PeriodUtils.periodStartDate(periodKey), messages("ated.date-format.numeric"))}")
      assert(document.getElementsByAttributeValue("for", "lending").text() === "Lending")
      assert(document.select("#conditional-lending > div > fieldset > legend").text() === "When did the Lending start?")
      assert(document.getElementById("lendingDate-hint")
        .text() ===s"For example, ${formatDate(PeriodUtils.periodStartDate(periodKey), messages("ated.date-format.numeric"))}")
      assert(document.getElementsByAttributeValue("for", "employeeOccupation").text() === "Employee occupation")
      assert(document.select("#conditional-employeeOccupation > div > fieldset > legend").text() === "When did the Employee occupation start?")
      assert(document.getElementById("employeeOccupationDate-hint")
        .text() ===s"For example, ${formatDate(PeriodUtils.periodStartDate(periodKey), messages("ated.date-format.numeric"))}")
      assert(document.getElementsByAttributeValue("for", "farmHouses").text() === "Farmhouses")
      assert(document.select("#conditional-farmHouses > div > fieldset > legend").text() === "When did the Farmhouse start?")
      assert(document.getElementById("farmHousesDate-hint")
        .text() ===s"For example, ${formatDate(PeriodUtils.periodStartDate(periodKey), messages("ated.date-format.numeric"))}")
      assert(document.getElementsByAttributeValue("for", "socialHousing").text() === "Social housing")
      assert(document.select("#conditional-socialHousing > div > fieldset > legend").text() === "When did the Social housing start?")
      assert(document.getElementById("socialHousingDate-hint")
        .text() ===s"For example, ${formatDate(PeriodUtils.periodStartDate(periodKey), messages("ated.date-format.numeric"))}")
      assert(document.getElementsByAttributeValue("for", "equityRelease").text() === "Equity release scheme (home reversion plans)")
      assert(document.select("#conditional-equityRelease > div > fieldset > legend").text() === "When did the Equity release scheme (home reversion plans) start?")
      assert(document.getElementById("equityReleaseDate-hint")
        .text() ===s"For example, ${formatDate(PeriodUtils.periodStartDate(periodKey), messages("ated.date-format.numeric"))}")
      assertResult(false)(document.getElementById("rentalBusiness").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("openToPublic").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("propertyDeveloper").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("propertyTrading").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("lending").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("employeeOccupation").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("farmHouses").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("socialHousing").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("equityRelease").outerHtml().contains("checked"))

      assert(document.getElementById("submit").text() === "Save and continue")

      assert(document.getElementsByAttributeValue("for", "rentalBusiness").text() === "Rental businesses")
      assert(document.getElementById("rentalBusiness").attr("checked") === "")
      assert(document.select("#conditional-rentalBusiness > div > fieldset > legend").text() === "When did the Rental business start?")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
    }

    Scenario("show the reliefs we have previously chosen") {

      Given("the client is creating a new relief and want to see the options")
      When("The user views the page")

      val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true)
      val html = injectedViewInstance(periodKey, reliefsForm.fill(reliefs), LocalDate.parse("2015-04-01"), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - What reliefs are you claiming?")
      assert(document.getElementsByTag("h1").text() contains ("What reliefs are you claiming?"))

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return")

      Then("The the text on the screen should be correct")
      assert(document.getElementById("lede-text").text() === "You can select more than one relief code. A single relief code can cover one or more properties.")

      assert(document.getElementById("rentalBusiness").outerHtml() contains "checked")
      assertResult(false)(document.getElementById("openToPublic").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("propertyDeveloper").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("propertyTrading").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("lending").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("employeeOccupation").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("farmHouses").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("socialHousing").outerHtml().contains("checked"))
      assertResult(false)(document.getElementById("equityRelease").outerHtml().contains("checked"))

      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")

    }
  }

}
