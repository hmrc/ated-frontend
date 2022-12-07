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

package views.reliefs

import config.ApplicationConfig
import models.{Reliefs, ReliefsTaxAvoidance, StandardAuthRetrievals, TaxAvoidance}
import org.joda.time.LocalDate
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
import views.html.reliefs.reliefsSummary

class ReliefsSummarySpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance: reliefsSummary = app.injector.instanceOf[views.html.reliefs.reliefsSummary]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  Feature("The user can view the relief summary page") {

    info("As a client I want to be able to view my relief return summary")

    Scenario("show the summary of the relief return during the draft period (month of March)") {

      Given("the client has created a new relief return and is viewing the summary of entered info")
      When("The user views the page")

      val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefsTaxAvoidance("123456", 2015, Reliefs(
        2015, socialHousing = true, socialHousingDate = Some(LocalDate.parse("2015-04-01"))
      ), TaxAvoidance(), LocalDate.now(), LocalDate.now())

      val html = injectedViewInstance(2015, Some(reliefsTaxAvoidance), canSubmit = false, isComplete = true, Html(""), None)

      val document = Jsoup.parse(html.toString())

      assert(document.getElementById("social-housing").text() contains "Social housing")

      Then("The text for disabling the submit button should be visible")
      assert(document.getElementById("submit-disabled-text").text() contains "You cannot submit returns until 1 April.")
    }

    Scenario("show the summary of the relief return during the draft period (month of March) in 2020") {

      Given("the client has created a new relief return and is viewing the summary of entered info")
      When("The user views the page")

      val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefsTaxAvoidance("123456", 2020, Reliefs(
        2020, socialHousing = true, socialHousingDate = Some(LocalDate.parse("2020-04-01"))
      ), TaxAvoidance(), LocalDate.now(), LocalDate.now())

      val html = injectedViewInstance(2020, Some(reliefsTaxAvoidance), canSubmit = false, isComplete = true, Html(""), None)

      val document = Jsoup.parse(html.toString())

      assert(document.getElementById("social-housing").text() contains "Provider of social housing or housing co-operative")

      Then("The text for disabling the submit button should be visible")
      assert(document.getElementById("submit-disabled-text").text() contains "You cannot submit returns until 1 April.")
    }

    Scenario("show the summary of the relief return in February before the draft period with no avoidance scheme provided") {

      Given("the client has created a new relief return and is viewing the summary of entered info")
      When("The user views the page")

      val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefsTaxAvoidance("123456", 2021,
        Reliefs(
          2021,
          rentalBusiness = true,
          rentalBusinessDate = Some(LocalDate.parse("2021-04-01")),
          openToPublic = true,
          openToPublicDate = Some(LocalDate.parse("2021-04-01")),
          propertyDeveloper = true,
          propertyDeveloperDate = Some(LocalDate.parse("2021-04-01")),
          propertyTrading = true,
          propertyTradingDate = Some(LocalDate.parse("2021-04-01")),
          lending = true,
          lendingDate = Some(LocalDate.parse("2021-04-01")),
          employeeOccupation = true,
          employeeOccupationDate = Some(LocalDate.parse("2021-04-01")),
          farmHouses = true,
          farmHousesDate = Some(LocalDate.parse("2021-04-01")),
          socialHousing = true,
          socialHousingDate = Some(LocalDate.parse("2021-04-01")),
          equityRelease = true,
          equityReleaseDate = Some(LocalDate.parse("2021-04-01"))
        ), TaxAvoidance(), LocalDate.now(), LocalDate.now())

      val html = injectedViewInstance(2021, Some(reliefsTaxAvoidance), canSubmit = true, isComplete = true, Html(""), None)

      val document = Jsoup.parse(html.toString())

      assert(document.getElementById("rentalBusiness").text() contains "Rental property")
      assert(document.getElementById("avoidance-scheme-header-rentalBusinessScheme-value-not-provided").text() contains "Not provided")
      assert(document.getElementById("openToPublic").text() contains "Open to the public")
      assert(document.getElementById("avoidance-scheme-header-openToPublicScheme-value-not-provided").text() contains "Not provided")
      assert(document.getElementById("property-developer").text() contains "Property developers")
      assert(document.getElementById("avoidance-scheme-header-propertyDeveloperScheme-value-not-provided").text() contains "Not provided")
      assert(document.getElementById("property-trading").text() contains "Property trading")
      assert(document.getElementById("avoidance-scheme-header-propertyTradingScheme-value-not-provided").text() contains "Not provided")
      assert(document.getElementById("lending").text() contains "Lending")
      assert(document.getElementById("avoidance-scheme-header-lendingScheme-value-not-provided").text() contains "Not provided")
      assert(document.getElementById("employee-occupation").text() contains "Employee occupation")
      assert(document.getElementById("avoidance-scheme-header-employeeOccupationScheme-value-not-provided").text() contains "Not provided")
      assert(document.getElementById("farm-houses").text() contains "Farmhouses")
      assert(document.getElementById("avoidance-scheme-header-farmHousesScheme-value-not-provided").text() contains "Not provided")
      assert(document.getElementById("social-housing").text() contains "Provider of social housing or housing co-operative")
      assert(document.getElementById("avoidance-scheme-header-socialHousingScheme-value-not-provided").text() contains "Not provided")
      assert(document.getElementById("equity-release").text() contains "Equity Release")
      assert(document.getElementById("avoidance-scheme-header-equityReleaseScheme-value-not-provided").text() contains "Not provided")

      Then("The text for the Confirm and continue button should be visible")
      assert(document.getElementById("submit").text() contains "Confirm and continue")
    }

    Scenario("show the summary of the relief return in February before the draft period with the avoidance scheme provided") {

      Given("the client has created a new relief return and is viewing the summary of entered info")
      When("The user views the page")

      val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefsTaxAvoidance("123456", 2021,
        Reliefs(
          2021,
          rentalBusiness = true,
          rentalBusinessDate = Some(LocalDate.parse("2021-04-01")),
          openToPublic = true,
          openToPublicDate = Some(LocalDate.parse("2021-04-01")),
          propertyDeveloper = true,
          propertyDeveloperDate = Some(LocalDate.parse("2021-04-01")),
          propertyTrading = true,
          propertyTradingDate = Some(LocalDate.parse("2021-04-01")),
          lending = true,
          lendingDate = Some(LocalDate.parse("2021-04-01")),
          employeeOccupation = true,
          employeeOccupationDate = Some(LocalDate.parse("2021-04-01")),
          farmHouses = true,
          farmHousesDate = Some(LocalDate.parse("2021-04-01")),
          socialHousing = true,
          socialHousingDate = Some(LocalDate.parse("2021-04-01")),
          equityRelease = true,
          equityReleaseDate = Some(LocalDate.parse("2021-04-01"))
        ), TaxAvoidance(
          rentalBusinessScheme = Some("12345678"),
          rentalBusinessSchemePromoter = Some("12345678"),
          openToPublicScheme = Some("12345678"),
          openToPublicSchemePromoter = Some("12345678"),
          propertyDeveloperScheme = Some("12345678"),
          propertyDeveloperSchemePromoter = Some("12345678"),
          propertyTradingScheme = Some("12345678"),
          propertyTradingSchemePromoter = Some("12345678"),
          lendingScheme = Some("12345678"),
          lendingSchemePromoter = Some("12345678"),
          employeeOccupationScheme = Some("12345678"),
          employeeOccupationSchemePromoter = Some("12345678"),
          farmHousesScheme = Some("12345678"),
          farmHousesSchemePromoter = Some("12345678"),
          socialHousingScheme = Some("12345678"),
          socialHousingSchemePromoter = Some("12345678"),
          equityReleaseScheme = Some("12345678"),
          equityReleaseSchemePromoter = Some("12345678")
        ), LocalDate.now(), LocalDate.now())

      val html = injectedViewInstance(2021, Some(reliefsTaxAvoidance), canSubmit = true, isComplete = true, Html(""), None)

      val document = Jsoup.parse(html.toString())

      assert(document.getElementById("rentalBusiness").text() contains "Rental property")
      assert(document.getElementById("tas-rb").text() contains "12345678")
      assert(document.getElementById("tasp-rb").text() contains "12345678")
      assert(document.getElementById("openToPublic").text() contains "Open to the public")
      assert(document.getElementById("tas-otp").text() contains "12345678")
      assert(document.getElementById("tasp-otp").text() contains "12345678")
      assert(document.getElementById("property-developer").text() contains "Property developers")
      assert(document.getElementById("tas-pd").text() contains "12345678")
      assert(document.getElementById("tasp-pd").text() contains "12345678")
      assert(document.getElementById("property-trading").text() contains "Property trading")
      assert(document.getElementById("tas-pt").text() contains "12345678")
      assert(document.getElementById("tasp-pt").text() contains "12345678")
      assert(document.getElementById("lending").text() contains "Lending")
      assert(document.getElementById("tas-ln").text() contains "12345678")
      assert(document.getElementById("tasp-ln").text() contains "12345678")
      assert(document.getElementById("employee-occupation").text() contains "Employee occupation")
      assert(document.getElementById("tas-eo").text() contains "12345678")
      assert(document.getElementById("tasp-eo").text() contains "12345678")
      assert(document.getElementById("farm-houses").text() contains "Farmhouses")
      assert(document.getElementById("tas-fh").text() contains "12345678")
      assert(document.getElementById("tasp-fh").text() contains "12345678")
      assert(document.getElementById("social-housing").text() contains "Provider of social housing or housing co-operative")
      assert(document.getElementById("tas-sh").text() contains "12345678")
      assert(document.getElementById("tasp-sh").text() contains "12345678")
      assert(document.getElementById("equity-release").text() contains "Equity Release")
      assert(document.getElementById("tas-er").text() contains "12345678")
      assert(document.getElementById("tasp-er").text() contains "12345678")

      Then("The text for the Confirm and continue button should be visible")
      assert(document.getElementById("submit").text() contains "Confirm and continue")
    }
  }
}
