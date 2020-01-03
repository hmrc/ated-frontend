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

package views.propertyDetails

import config.ApplicationConfig
import forms.PropertyDetailsForms._
import testhelpers.MockAuthUtil
import models.{PeriodChooseRelief, StandardAuthRetrievals}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import utils.ReliefsUtils

class periodChooseReliefSpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]

  feature("The user can add a period that the property is in relief") {

    info("as a client i want to indicate when my property is in relief")

    scenario("allow selecting a relief") {

      Given("the client is adding a relief")
      When("The user views the page")

      val periodStartDate = new LocalDate("2015-01-01")
      val periodEndDate = new LocalDate("2016-02-02")
      val html = views.html.propertyDetails.periodChooseRelief("1", 2015, periodChooseReliefForm, None)

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Select the type of relief")
      assert(document.select("h1").text === "Select the type of relief")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

      assert(document.getElementById("reliefDescription-property_rental_businesses_field").text() === "Rental business")
      assert(document.getElementById("reliefDescription-property_rental_businesses").attr("checked") === "")
      assert(document.getElementById("reliefDescription-dwellings_opened_to_the_public_field").text() === "Open to the public")
      assert(document.getElementById("reliefDescription-dwellings_opened_to_the_public").attr("checked") === "")
      assert(document.getElementById("reliefDescription-property_developers_field").text() === "Property developer")
      assert(document.getElementById("reliefDescription-property_developers").attr("checked") === "")
      assert(document.getElementById("reliefDescription-property_traders_carrying_on_a_property_trading_business_field").text() === "Property trading")
      assert(document.getElementById("reliefDescription-property_traders_carrying_on_a_property_trading_business").attr("checked") === "")
      assert(document.getElementById("reliefDescription-financial_institutions_acquiring_dwellings_in_the_course_of_lending_field").text() === "Lending")
      assert(document.getElementById("reliefDescription-financial_institutions_acquiring_dwellings_in_the_course_of_lending").attr("checked") === "")
      assert(document.getElementById("reliefDescription-dwellings_used_for_trade_purposes_field").text() === "Employee occupation")
      assert(document.getElementById("reliefDescription-dwellings_used_for_trade_purposes").attr("checked") === "")
      assert(document.getElementById("reliefDescription-farmhouses_field").text() === "Farmhouse")
      assert(document.getElementById("reliefDescription-farmhouses").attr("checked") === "")
      assert(document.getElementById("reliefDescription-registered_providers_of_social_housing_field").text() === "Social housing")
      assert(document.getElementById("reliefDescription-registered_providers_of_social_housing").attr("checked") === "")
      assert(document.getElementById("reliefDescription-equity_release_scheme_field").text() === "Equity release scheme (home reversion plans)")
      assert(document.getElementById("reliefDescription-equity_release_scheme").attr("checked") === "")

      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref") === null)
    }

    scenario("display a selected a relief") {

      Given("the client is adding a relief")
      When("The user views the page")

      val periodStartDate = new LocalDate("2015-01-01")
      val periodEndDate = new LocalDate("2016-02-02")
      val html = views.html.propertyDetails.periodChooseRelief("1",
        2015, periodChooseReliefForm.fill(PeriodChooseRelief(ReliefsUtils.RentalBusinessDesc)), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Select the type of relief")
      assert(document.select("h1").text === "Select the type of relief")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

      assert(document.getElementById("reliefDescription-property_rental_businesses").attr("checked") === "checked")
      assert(document.getElementById("reliefDescription-dwellings_opened_to_the_public").attr("checked") === "")
      assert(document.getElementById("reliefDescription-property_developers").attr("checked") === "")
      assert(document.getElementById("reliefDescription-property_traders_carrying_on_a_property_trading_business").attr("checked") === "")
      assert(document.getElementById("reliefDescription-financial_institutions_acquiring_dwellings_in_the_course_of_lending").attr("checked") === "")
      assert(document.getElementById("reliefDescription-dwellings_used_for_trade_purposes").attr("checked") === "")
      assert(document.getElementById("reliefDescription-farmhouses").attr("checked") === "")
      assert(document.getElementById("reliefDescription-registered_providers_of_social_housing").attr("checked") === "")

      Then("The submit button should have the correct name")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }

}
