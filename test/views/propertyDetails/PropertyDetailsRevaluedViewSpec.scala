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
import models.{PropertyDetailsRevalued, StandardAuthRetrievals}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import utils.AtedUtils

class PropertyDetailsRevaluedViewSpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
feature("The user can view an empty property revalue page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page for the first time") {

      Given("A user visits the page and clicks yes")
      When("The user views the page and clicks yes")

      implicit val request = FakeRequest()

      val html = views.html.propertyDetails.propertyDetailsRevalued("1", 2015, propertyDetailsRevaluedForm, None, Some("backLink"))

      val document = Jsoup.parse(html.toString())
      Then("the page title : Have you had the property revalued since you made the £40,000 change?")
      assert(document.title() === "Has the property been revalued since the £40,000 or more change? - GOV.UK")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

      And("No data is populated")
      assert(document.getElementById("isPropertyRevalued").text() === "Has the property been revalued since the £40,000 or more change? Yes No")
      assert(document.getElementById("isPropertyRevalued-true").text() === "")
      assert(document.getElementById("isPropertyRevalued-false").text() === "")
      assert(document.getElementById("revaluedDate-day").attr("value") === "")
      assert(document.getElementById("revaluedDate-month").attr("value") === "")
      assert(document.getElementById("revaluedDate-year").attr("value") === "")
      assert(document.getElementById("partAcqDispDate-day").attr("value") === "")
      assert(document.getElementById("partAcqDispDate-month").attr("value") === "")
      assert(document.getElementById("partAcqDispDate-year").attr("value") === "")

      And("the revalued date is setup correctly")
      assert(document.getElementById("revaluedValue_field").text() === "What is the new valuation of the property in GBP?")
      assert(document.getElementById("revaluedDate_legend").text() === "What date did you get the property revalued?")
      assert(document.getElementById("revaluedValue").attr("value") === "")

      And("the save button is correct")
      assert(document.getElementById("submit").text() === "Save and continue")

      assert(document.getElementById("notRevalued").text() === "The property must be revalued before you can submit this chargeable return")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
    }

  }

  feature("The user can edit a property revalue page where they previously said it was revalued") {

    info("The yes option has been clicked may a user")

    scenario("The user views the page to edit the data") {

      Given("A user visits the page to edit data")
      When("The user views the page to edit data")

      implicit val request = FakeRequest()

      val propertyDetailsRevalued = PropertyDetailsRevalued(isPropertyRevalued = Some(true),
        revaluedValue = Some(BigDecimal(123456.34)),
        revaluedDate = Some(new LocalDate("1971-01-01")),
        partAcqDispDate = Some(new LocalDate("1972-02-02")))

      val html = views.html.propertyDetails.propertyDetailsRevalued("1", 2015,
        propertyDetailsRevaluedForm.fill(propertyDetailsRevalued), Some(AtedUtils.EDIT_SUBMITTED), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("the page title : Have you had the property revalued since you made the £40,000 change?")
      assert(document.title() === "Has the property been revalued since the £40,000 or more change? - GOV.UK")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      And("The data is populated for a property value set to true")
      assert(document.getElementById("isPropertyRevalued").text() === "Has the property been revalued since the £40,000 or more change? Yes No")
      assert(document.getElementById("isPropertyRevalued-true").attr("checked") === "checked")
      assert(document.getElementById("isPropertyRevalued-false").attr("checked") === "")
      assert(document.getElementById("revaluedDate-day").attr("value") === "1")
      assert(document.getElementById("revaluedDate-month").attr("value") === "1")
      assert(document.getElementById("revaluedDate-year").attr("value") === "1971")
      assert(document.getElementById("partAcqDispDate-day").attr("value") === "2")
      assert(document.getElementById("partAcqDispDate-month").attr("value") === "2")
      assert(document.getElementById("partAcqDispDate-year").attr("value") === "1972")
      assert(document.getElementById("revaluedValue").attr("value") === "123456.34")

      assert(document.getElementById("submit").text() === "Save and continue")
      assert(document.getElementById("notRevalued").text() === "The property must be revalued before you can submit this chargeable return")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }

  feature("The user can edit a property revalue page where they previously said it was NOT revalued") {

      info("The no option has been clicked may a user")

      scenario("The user has not made a £40,000 change to property") {

        Given("A user has clicked no")
        When("A user has clicked no")

        implicit val request = FakeRequest()

        val propertyDetailsRevalued = PropertyDetailsRevalued(isPropertyRevalued = Some(false),
          revaluedValue = None,
          revaluedDate = None,
          partAcqDispDate = None)

        val html = views.html.propertyDetails.propertyDetailsRevalued("1", 2015, propertyDetailsRevaluedForm.fill(propertyDetailsRevalued), None, Some("backLink"))

        val document = Jsoup.parse(html.toString())
        Then("the page title : Have you had the property revalued since you made the £40,000 change?")
        assert(document.title() === "Has the property been revalued since the £40,000 or more change? - GOV.UK")

        And("The data is populated for a property value set to false")
        assert(document.getElementById("isPropertyRevalued").text() === "Has the property been revalued since the £40,000 or more change? Yes No")
        assert(document.getElementById("isPropertyRevalued-true").attr("checked") === "")
        assert(document.getElementById("isPropertyRevalued-false").attr("checked") === "checked")
        assert(document.getElementById("revaluedDate-day").attr("value") === "")
        assert(document.getElementById("revaluedDate-month").attr("value") === "")
        assert(document.getElementById("revaluedDate-year").attr("value") === "")
        assert(document.getElementById("partAcqDispDate-day").attr("value") === "")
        assert(document.getElementById("partAcqDispDate-month").attr("value") === "")
        assert(document.getElementById("partAcqDispDate-year").attr("value") === "")
        assert(document.getElementById("revaluedValue").attr("value") === "")

        assert(document.getElementById("submit").text() === "Save and continue")

        assert(document.getElementById("notRevalued").text() === "The property must be revalued before you can submit this chargeable return")

        Then("The back link is correct")
        assert(document.getElementById("backLinkHref").text === "Back")
      }

    }
  }
