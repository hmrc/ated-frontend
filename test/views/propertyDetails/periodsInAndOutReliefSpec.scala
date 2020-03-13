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
import models.{LineItem, StandardAuthRetrievals}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import utils.AtedUtils

class periodsInAndOutReliefSpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  feature("The user can view the periods and add the property in and out of relief") {

    info("as a client i want to be able to view the periods in and out of relief, and add and delete them")

    scenario("return an empty table if we have no periods") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val html = views.html.propertyDetails.periodsInAndOutRelief("1", 2015, periodsInAndOutReliefForm, Nil, None, Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Add periods when the property was in relief and when it was liable for an ATED charge")
      assert(document.select("h1").text === "Add periods when the property was in relief and when it was liable for an ATED charge")

      Then("The subheader should be - Create return")
      assert(document.getElementById("pre-heading").text() === "This section is: Create return")

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
      assert(document.getElementById("backLinkHref").text === "Back")
    }

    scenario("return a populated table if we have periods") {

      Given("the client has a non uk company and the arrive at the overseas company registration")
      When("The user views the page")

      val periods = List[models.LineItem](
        LineItem("liability", new LocalDate(s"2015-4-1"), new LocalDate(s"2015-5-1"), Some("Liable for charge")),
        LineItem("relief", new LocalDate(s"2016-4-1"), new LocalDate(s"2016-5-1"), Some("Rental property"))
      )
      val html = views.html.propertyDetails.periodsInAndOutRelief("1", 2015,
        periodsInAndOutReliefForm, periods, Some(AtedUtils.EDIT_SUBMITTED), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Add periods when the property was in relief and when it was liable for an ATED charge")
      assert(document.select("h1").text === "Add periods when the property was in relief and when it was liable for an ATED charge")

      Then("The subheader should be - Change return")
      assert(document.getElementById("pre-heading").text() === "This section is: Change return")

      Then("Text should read No periods of relief or charge have been added yet")
      assert(document.getElementById("no-periods") === null)

      Then("The table should exist")
      assert(document.getElementById("date-from-header").text() === "Date from")
      assert(document.getElementById("date-to-header").text() === "Date to")
      assert(document.getElementById("return-type-header").text() === "Return type")

      assert(document.getElementById("date-from-value-0").text() === "1 April 2015")
      assert(document.getElementById("date-to-value-0").text() === "1 May 2015")
      assert(document.getElementById("return-type-value-0").text() === "Liable for charge")
      assert(document.getElementById("action-0").text() === "Delete Liable for charge 1 April 2015")

      assert(document.getElementById("date-from-value-1").text() === "1 April 2016")
      assert(document.getElementById("date-to-value-1").text() === "1 May 2016")
      assert(document.getElementById("return-type-value-1").text() === "Rental property")
      assert(document.getElementById("action-1").text() === "Delete Rental property 1 April 2016")

      Then("The buttons should have the correct text")
      assert(document.getElementById("add-period-charge").text() === "Add a period of charge")
      assert(document.getElementById("add-period-in-relief").text() === "Add a period of relief")
      assert(document.getElementById("submit").text() === "Save and continue")

      Then("The back link is correct")
      assert(document.getElementById("backLinkHref").text === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLink")
    }
  }

}
