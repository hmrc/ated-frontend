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

package views.propertyDetails

import config.ApplicationConfig
import forms.PropertyDetailsForms._
import models.{LineItem, StandardAuthRetrievals}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.AtedUtils
import views.html.propertyDetails.periodsInAndOutRelief

class periodsInAndOutReliefSpec extends AnyFeatureSpecLike with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance: periodsInAndOutRelief = app.injector.instanceOf[views.html.propertyDetails.periodsInAndOutRelief]

  Feature("The user can view the periods and add the property in and out of relief") {

    info("as a client i want to be able to view the periods in and out of relief, and add and delete them")

    Scenario("return an empty table if we have no periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val html = injectedViewInstance("1", 2015, periodsInAndOutReliefForm, Nil, None, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The title should match - Add periods when the property was in relief and when it was liable for an ATED charge - Submit and view your ATED returns - GOV.UK")
      assert(document.title() === "Add periods when the property was in relief and when it was liable for an ATED charge - Submit and view your ATED returns - GOV.UK")

      Then("The header should match - Add periods when the property was in relief and when it was liable for an ATED charge")
      assert(document.getElementsByTag("h1").text contains "Add periods when the property was in relief and when it was liable for an ATED charge")

      Then("The subheader should be - Create return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Create return")

      Then("Text should read No periods of relief or charge have been added yet")
      assert(document.getElementById("no-periods").text() === "No periods of relief or charge have been added yet")

      Then("The buttons should have the correct text")
      assert(document.getElementById("add-period-charge").text() === "Add a period of charge")
      assert(document.getElementById("add-period-in-relief").text() === "Add a period of relief")

      Then("The table shouldn't exist")
      assert(document.getElementById("date-from-header") === null)
      assert(document.getElementById("date-to-header") === null)
      assert(document.getElementById("return-type-header") === null)

      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
    }

    Scenario("return a populated table if we have periods") {

      Given("the client has a non uk company and the arrive at the overseas company registration")
      When("The user views the page")

      val periods = List[models.LineItem](
        LineItem("liability", new LocalDate(s"2015-4-1"), new LocalDate(s"2015-5-1"), Some("Liable for charge")),
        LineItem("relief", new LocalDate(s"2016-4-1"), new LocalDate(s"2016-5-1"), Some("Rental property"))
      )
      val html = injectedViewInstance("1", 2015,
        periodsInAndOutReliefForm, periods, Some(AtedUtils.EDIT_SUBMITTED), Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())
      Then("The header should match - Add periods when the property was in relief and when it was liable for an ATED charge")
      assert(document.getElementsByTag("h1").text contains "Add periods when the property was in relief and when it was liable for an ATED charge")

      Then("The subheader should be - Change return")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: Change return")

      Then("The text for No periods of relief or charge have been added yet should not be displayed")
      assert(document.getElementById("no-periods") === null)

      Then("The table should exist")
      assert(document.getElementById("date-from-header").text() === "Date from")
      assert(document.getElementById("date-to-header").text() === "Date to")
      assert(document.getElementById("return-type-header").text() === "Return type")

      assert(document.getElementById("date-from-value-0").text() === "1 April 2015")
      assert(document.getElementById("date-to-value-0").text() === "1 May 2015")
      assert(document.getElementById("return-type-value-0").text() === "Liable for charge")
      assert(document.getElementById("action-0").text() === "Delete Liable for charge 1 April 2015 to 1 May 2015")

      assert(document.getElementById("date-from-value-1").text() === "1 April 2016")
      assert(document.getElementById("date-to-value-1").text() === "1 May 2016")
      assert(document.getElementById("return-type-value-1").text() === "Rental property")
      assert(document.getElementById("action-1").text() === "Delete Rental property 1 April 2016 to 1 May 2016")

      Then("The buttons should have the correct text")
      assert(document.getElementById("add-period-charge").text() === "Add a period of charge")
      assert(document.getElementById("add-period-in-relief").text() === "Add a period of relief")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementsByClass("govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }
  }

}
