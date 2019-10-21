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

package views.reliefs

import config.ApplicationConfig
import forms.ReliefForms.reliefsForm
import models.{Reliefs, StandardAuthRetrievals}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import utils.{MockAuthUtil, PeriodUtils}

class chooseReliefsSpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val periodKey = 2015

  feature("The user can view the choose reliefs page") {

    info("as a client I want to be able to select reliefs for my properties")

    scenario("show the reliefs we can choose") {

      Given("the client is creating a new relief and want to see the options")
      When("The user views the page")

      val html = views.html.reliefs.chooseReliefs(periodKey, reliefsForm, new LocalDate("2015-04-01"), None)

      val document = Jsoup.parse(html.toString())

      Then("The header should match - What reliefs are you claiming?")
      assert(document.select("h1").text === "What reliefs are you claiming?")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

      Then("The the text on the screen should be correct")
      assert(document.getElementById("lede-text").text() === "You can select more than one relief code. A single relief code can cover one or more properties.")
      assert(document.getElementById("choose-reliefs-label").text() === "Select all reliefs that apply")
      assert(document.getElementById("rentalBusiness_field").text() === "Rental businesses")
      assert(document.getElementById("rentalBusinessDate_legend").text() === "When did the Rental business start?")
      assert(document.getElementById("rentalBusinessDate_hint")
        .text() ===s"For example, ${PeriodUtils.periodStartDate(periodKey).toString(Messages("ated.date-format.numeric"))}")
      assert(document.getElementById("openToPublic_field").text() === "Open to the public")
      assert(document.getElementById("openToPublicDate_legend").text() === "When did the Open to the public start?")
      assert(document.getElementById("openToPublicDate_hint")
        .text() ===s"For example, ${PeriodUtils.periodStartDate(periodKey).toString(Messages("ated.date-format.numeric"))}")
      assert(document.getElementById("propertyDeveloper_field").text() === "Property developers")
      assert(document.getElementById("propertyDeveloperDate_legend").text() === "When did the Property developer start?")
      assert(document.getElementById("propertyDeveloperDate_hint")
        .text() ===s"For example, ${PeriodUtils.periodStartDate(periodKey).toString(Messages("ated.date-format.numeric"))}")
      assert(document.getElementById("propertyTrading_field").text() === "Property trading")
      assert(document.getElementById("propertyTradingDate_legend").text() === "When did the Property trading start?")
      assert(document.getElementById("propertyTradingDate_hint")
        .text() ===s"For example, ${PeriodUtils.periodStartDate(periodKey).toString(Messages("ated.date-format.numeric"))}")
      assert(document.getElementById("lending_field").text() === "Lending")
      assert(document.getElementById("lendingDate_legend").text() === "When did the Lending start?")
      assert(document.getElementById("lendingDate_hint")
        .text() ===s"For example, ${PeriodUtils.periodStartDate(periodKey).toString(Messages("ated.date-format.numeric"))}")
      assert(document.getElementById("employeeOccupation_field").text() === "Employee occupation")
      assert(document.getElementById("employeeOccupationDate_legend").text() === "When did the Employee occupation start?")
      assert(document.getElementById("employeeOccupationDate_hint")
        .text() ===s"For example, ${PeriodUtils.periodStartDate(periodKey).toString(Messages("ated.date-format.numeric"))}")
      assert(document.getElementById("farmHouses_field").text() === "Farmhouses")
      assert(document.getElementById("farmHousesDate_legend").text() === "When did the Farmhouse start?")
      assert(document.getElementById("farmHousesDate_hint")
        .text() ===s"For example, ${PeriodUtils.periodStartDate(periodKey).toString(Messages("ated.date-format.numeric"))}")
      assert(document.getElementById("socialHousing_field").text() === "Social housing")
      assert(document.getElementById("socialHousingDate_legend").text() === "When did the Social housing start?")
      assert(document.getElementById("socialHousingDate_hint")
        .text() ===s"For example, ${PeriodUtils.periodStartDate(periodKey).toString(Messages("ated.date-format.numeric"))}")
      assert(document.getElementById("equityRelease_field").text() === "Equity release scheme (home reversion plans)")
      assert(document.getElementById("equityReleaseDate_legend").text() === "When did the Equity release scheme (home reversion plans) start?")
      assert(document.getElementById("equityReleaseDate_hint")
        .text() ===s"For example, ${PeriodUtils.periodStartDate(periodKey).toString(Messages("ated.date-format.numeric"))}")
      assert(document.getElementById("rentalBusiness").attr("checked") === "")
      assert(document.getElementById("openToPublic").attr("checked") === "")
      assert(document.getElementById("propertyDeveloper").attr("checked") === "")
      assert(document.getElementById("propertyTrading").attr("checked") === "")
      assert(document.getElementById("lending").attr("checked") === "")
      assert(document.getElementById("employeeOccupation").attr("checked") === "")
      assert(document.getElementById("farmHouses").attr("checked") === "")
      assert(document.getElementById("socialHousing").attr("checked") === "")
      assert(document.getElementById("equityRelease").attr("checked") === "")

      assert(document.getElementById("submit").text() === "Save and continue")

      assert(document.getElementById("rentalBusiness_field").text() === "Rental businesses")
      assert(document.getElementById("rentalBusiness").attr("checked") === "")
      assert(document.getElementById("rentalBusinessDate_legend").text() === "When did the Rental business start?")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref") === null)
    }

    scenario("show the reliefs we have previously chosen") {

      Given("the client is creating a new relief and want to see the options")
      When("The user views the page")

      val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true)
      val html = views.html.reliefs.chooseReliefs(periodKey, reliefsForm.fill(reliefs), new LocalDate("2015-04-01"), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - What reliefs are you claiming?")
      assert(document.select("h1").text === "What reliefs are you claiming?")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

      Then("The the text on the screen should be correct")
      assert(document.getElementById("lede-text").text() === "You can select more than one relief code. A single relief code can cover one or more properties.")

      assert(document.getElementById("rentalBusiness").attr("checked") === "checked")
      assert(document.getElementById("openToPublic").attr("checked") === "")
      assert(document.getElementById("propertyDeveloper").attr("checked") === "")
      assert(document.getElementById("propertyTrading").attr("checked") === "")
      assert(document.getElementById("lending").attr("checked") === "")
      assert(document.getElementById("employeeOccupation").attr("checked") === "")
      assert(document.getElementById("farmHouses").attr("checked") === "")
      assert(document.getElementById("socialHousing").attr("checked") === "")
      assert(document.getElementById("equityRelease").attr("checked") === "")

      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")

    }
  }

}
