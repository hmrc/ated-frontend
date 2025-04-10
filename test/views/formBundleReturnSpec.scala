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

package views

import builders.PropertyDetailsBuilder
import config.ApplicationConfig
import models._
import java.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.{AtedConstants, PeriodUtils}
import views.html.formBundleReturn

class formBundleReturnSpec extends AnyFeatureSpec with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedViewInstance: formBundleReturn = app.injector.instanceOf[views.html.formBundleReturn]

  val formBundleProp: FormBundleProperty = FormBundleProperty(BigDecimal(100), LocalDate.parse("2015-09-08"),
    LocalDate.parse("2015-10-12"), AtedConstants.LiabilityReturnType, None)
  val formBundleAddress: FormBundleAddress = FormBundleAddress("100 addressLine1", "addressLine2", Some("addressLine3"), Some("AddressLine4"), Some("XX11XX"), "GB")
  val formBundlePropertyDetails: FormBundlePropertyDetails = FormBundlePropertyDetails(Some("title here"), formBundleAddress, Some("additional details"))
  val viewReturnWithSinglePeriod: FormBundleReturn =
    FormBundleReturn("2014",
    formBundlePropertyDetails, Some(LocalDate.parse("2013-10-10")), Some(BigDecimal(100)), Some("ABCdefgh"), Some("12345678"), Some("1234"), professionalValuation = true, ninetyDayRuleApplies = true,
      LocalDate.parse("2015-05-10"), BigDecimal(9324), "1234567891",
      List(formBundleProp))

  val viewReturnWithMultiPeriod =
    FormBundleReturn("2014",
      formBundlePropertyDetails, Some(LocalDate.parse("2013-10-10")), Some(BigDecimal(100)), Some("ABCdefgh"), Some("12345678"), Some("1234"), professionalValuation = true, ninetyDayRuleApplies = true,
      LocalDate.parse("2015-05-10"), BigDecimal(9324), "1234567891",
      List(
        FormBundleProperty(BigDecimal(100), LocalDate.parse("2015-04-01"),
          LocalDate.parse("2015-10-31"), AtedConstants.LiabilityReturnType, None ),
        FormBundleProperty(BigDecimal(200), LocalDate.parse("2015-11-01"),
          LocalDate.parse("2016-03-31"), AtedConstants.ReliefReturnType, Some("Property rental businesses"))
      )
    )

  val viewWithDisposePeriod =
    FormBundleReturn("2014",
      formBundlePropertyDetails, Some(LocalDate.parse("2013-10-10")), Some(BigDecimal(100)), Some("ABCdefgh"), Some("12345678"), Some("1234"), professionalValuation = true, ninetyDayRuleApplies = true,
      LocalDate.parse("2015-05-10"), BigDecimal(9324), "1234567891",
      List(
        FormBundleProperty(BigDecimal(100), LocalDate.parse("2015-04-01"),
          LocalDate.parse("2015-10-31"), AtedConstants.LiabilityReturnType, None ),
        FormBundleProperty(BigDecimal(100), LocalDate.parse("2015-11-01"), LocalDate.parse("2016-03-31"), AtedConstants.DisposeReturnType, None)
      )
    )


  Feature("The user can view their previous returns") {

    info("as a client i want to be able to view my previous returns")

    Scenario("View the Form Bundle when we have none") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, 2015)
      assert(displayPeriods.size === 2)
      val html = injectedViewInstance(2015, None, "formBundleNo", Some("ACME Ltd"), changeAllowed = false, editAllowed = false, Nil, Nil, Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - View return")
      assert(document.getElementsByTag("h1").text contains "View return")

      Then("The subheader should be - ACME Ltd")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "You are logged in as: ACME Ltd")

      Then("The text should be - From the ATED chargeable period from 1 April 2015 to 31 March 2016.")
      assert(document.getElementById("form-bundle-text").text() === "From the ATED chargeable period from 1 April 2015 to 31 March 2016.")

      Then("The the fields should be correct")
      assert(document.getElementById("th-view-return-property") === null)

      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
    }
    Scenario("View the Form Bundle when we have a Form Bundle with a single period") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, 2015)
      assert(displayPeriods.size === 2)
      val valuesToDisplay = PeriodUtils.getOrderedReturnPeriodValues(viewReturnWithSinglePeriod.lineItem, viewReturnWithSinglePeriod.dateOfAcquisition)
      val periodsToDisplay = PeriodUtils.getDisplayFormBundleProperties(viewReturnWithSinglePeriod.lineItem, 2015)
      val html = injectedViewInstance(2015, Some(viewReturnWithSinglePeriod), "formBundleNo", None, changeAllowed = false, editAllowed =  false, valuesToDisplay, periodsToDisplay, Html(""), Some("http://backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - View return")
      assert(document.getElementsByTag("h1").text contains "View return")

      Then("The subheader should be - ''")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "You are logged in as:")

      Then("The text should be - From the ATED chargeable period from 1 April 2015 to 31 March 2016.")
      assert(document.getElementById("form-bundle-text").text() === "From the ATED chargeable period from 1 April 2015 to 31 March 2016.")

      Then("The the fields should be correct")
      assert(document.getElementById("th-view-return-property").text() === "Property")
      assert(document.getElementById("th-view-return-property-address").text() === "Address")
      assert(document.getElementById("td-view-return-property-address").text().contains("100 addressLine1"))
      assert(document.getElementById("td-view-return-property-address").text().contains("addressLine2"))
      assert(document.getElementById("td-view-return-property-address").text().contains("XX11XX"))
      assert(document.getElementById("th-view-return-property-title-no").text() === "Property’s title number")
      assert(document.getElementById("td-view-return-property-title-no").text() === "title here")

      assert(document.getElementById("th-view-return-property-value").text() === "Value of the property")
      assert(document.getElementById("th-view-return-value-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("td-view-return-value-0").text() === "£100")
      assert(document.getElementById("th-view-return-property-valuation-date-0").text() === "Professionally valued")
      assert(document.getElementById("td-view-return-property-valuation-date-0").text() === "Yes")
      assert(document.getElementById("th-view-return-value-1") === null)

      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text === "Liable for charge")
      assert(document.getElementById("period-0").text === "8 September 2015 to 12 October 2015")

      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("th-view-return-additional-info").text() === "Additional information")
      assert(document.getElementById("td-view-return-additional-info").text() === "additional details")

      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("th-view-return-tax-avoidance").text() === "Avoidance scheme reference number")
      assert(document.getElementById("td-view-return-tax-avoidance").text() === "ABCdefgh")
      assert(document.getElementById("th-view-return-promoter").text() === "Promoter reference number")
      assert(document.getElementById("td-view-return-promoter").text() === "12345678")
      assert(document.getElementById("th-view-return-payment").text() === "Payment reference")
      assert(document.getElementById("td-view-return-payment").text() === "1234567891")
      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("th-view-return-status").text() === "Status")
      assert(document.getElementById("td-view-return-status").text() === "Submitted")
      assert(document.getElementById("th-view-return-date").text() === "Return date")
      assert(document.getElementById("td-view-return-date").text() === "10 May 2015")


      assert(document.getElementById("return-charge-text").text() === "Based on the information you have given us your ATED charge is £9,324")
      assert(document.getElementById("return-charge").text() === "£9,324")

      assert(document.getElementById("submit") === null)

      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
    }

    Scenario("View the Form Bundle when we have a Form Bundle with a single period and is editable") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, 2015)
      assert(displayPeriods.size === 2)
      val valuesToDisplay = PeriodUtils.getOrderedReturnPeriodValues(viewReturnWithSinglePeriod.lineItem, viewReturnWithSinglePeriod.dateOfAcquisition)
      val periodsToDisplay = PeriodUtils.getDisplayFormBundleProperties(viewReturnWithSinglePeriod.lineItem, 2015)
      val html = injectedViewInstance(2015, Some(viewReturnWithSinglePeriod), "formBundleNo", Some("ACME Ltd"), changeAllowed = true, editAllowed = true, valuesToDisplay, periodsToDisplay,  Html(""), None)

      val document = Jsoup.parse(html.toString())

      Then("The header should match - View return")
      assert(document.getElementsByTag("h1").text contains "View return")

      Then("The subheader should be - ACME Ltd")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "You are logged in as: ACME Ltd")

      Then("The text should be - From the ATED chargeable period from 1 April 2015 to 31 March 2016.")
      assert(document.getElementById("form-bundle-text").text() === "From the ATED chargeable period from 1 April 2015 to 31 March 2016.")

      Then("The the fields should be correct")
      assert(document.getElementById("th-view-return-property").text() === "Property")
      assert(document.getElementById("th-view-return-property-address").text() === "Address")
      assert(document.getElementById("td-view-return-property-address").text().contains("100 addressLine1"))
      assert(document.getElementById("td-view-return-property-address").text().contains("addressLine2"))
      assert(document.getElementById("td-view-return-property-address").text().contains("XX11XX"))
      assert(document.getElementById("th-view-return-property-title-no").text() === "Property’s title number")
      assert(document.getElementById("td-view-return-property-title-no").text() === "title here")

      assert(document.getElementById("th-view-return-property-value").text() === "Value of the property")
      assert(document.getElementById("th-view-return-value-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("td-view-return-value-0").text() === "£100")
      assert(document.getElementById("th-view-return-value-1") === null)
      assert(document.getElementById("th-view-return-property-valuation-date-0").text() === "Professionally valued")
      assert(document.getElementById("td-view-return-property-valuation-date-0").text() === "Yes")

      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text === "Liable for charge")
      assert(document.getElementById("period-0").text === "8 September 2015 to 12 October 2015")

      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("th-view-return-additional-info").text() === "Additional information")
      assert(document.getElementById("td-view-return-additional-info").text() === "additional details")

      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("th-view-return-tax-avoidance").text() === "Avoidance scheme reference number")
      assert(document.getElementById("td-view-return-tax-avoidance").text() === "ABCdefgh")
      assert(document.getElementById("th-view-return-promoter").text() === "Promoter reference number")
      assert(document.getElementById("td-view-return-promoter").text() === "12345678")
      assert(document.getElementById("th-view-return-payment").text() === "Payment reference")
      assert(document.getElementById("td-view-return-payment").text() === "1234567891")
      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("th-view-return-status").text() === "Status")
      assert(document.getElementById("td-view-return-status").text() === "Submitted")
      assert(document.getElementById("th-view-return-date").text() === "Return date")
      assert(document.getElementById("td-view-return-date").text() === "10 May 2015")


      assert(document.getElementById("return-charge-text").text() === "Based on the information you have given us your ATED charge is £9,324")
      assert(document.getElementById("return-charge").text() === "£9,324")

      assert(document.getElementById("submit").text() === "Change return")
    }

    Scenario("View the Form Bundle when we have a Form Bundle with a multiple periods and is editable") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, 2015)
      assert(displayPeriods.size === 2)
      val valuesToDisplay = PeriodUtils.getOrderedReturnPeriodValues(viewReturnWithMultiPeriod.lineItem, viewReturnWithMultiPeriod.dateOfAcquisition)
      val periodsToDisplay = PeriodUtils.getDisplayFormBundleProperties(viewReturnWithMultiPeriod.lineItem, 2015)
      val html = injectedViewInstance(2015, Some(viewReturnWithMultiPeriod), "formBundleNo", Some("ACME Ltd"), changeAllowed = true, editAllowed = true, valuesToDisplay, periodsToDisplay, Html(""), None)

      val document = Jsoup.parse(html.toString())

      Then("The header should match - View return")
      assert(document.getElementsByTag("h1").text contains "View return")

      Then("The subheader should be - ACME Ltd")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "You are logged in as: ACME Ltd")

      Then("The text should be - From the ATED chargeable period from 1 April 2015 to 31 March 2016.")
      assert(document.getElementById("form-bundle-text").text() === "From the ATED chargeable period from 1 April 2015 to 31 March 2016.")

      Then("The the fields should be correct")
      assert(document.getElementById("th-view-return-property").text() === "Property")
      assert(document.getElementById("th-view-return-property-address").text() === "Address")
      assert(document.getElementById("td-view-return-property-address").text().contains("100 addressLine1"))
      assert(document.getElementById("td-view-return-property-address").text().contains("addressLine1"))
      assert(document.getElementById("td-view-return-property-address").text().contains("XX11XX"))
      assert(document.getElementById("th-view-return-property-title-no").text() === "Property’s title number")
      assert(document.getElementById("td-view-return-property-title-no").text() === "title here")

      assert(document.getElementById("th-view-return-property-value").text() === "Value of the property")
      assert(document.getElementById("th-view-return-value-0").text() === "Initial value for the purposes of ATED")
      assert(document.getElementById("td-view-return-value-0").text() === "£100")
      assert(document.getElementById("th-view-return-property-valuation-date-0").text() === "Professionally valued")
      assert(document.getElementById("td-view-return-property-valuation-date-0").text() === "Yes")
      assert(document.getElementById("th-view-return-value-1").text() === "New value")
      assert(document.getElementById("td-view-return-value-1").text() === "£200")
      assert(document.getElementById("th-view-return-property-valuation-date-1").text() === "Professionally valued")
      assert(document.getElementById("td-view-return-property-valuation-date-1").text() === "Yes")

      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text === "Liable for charge")
      assert(document.getElementById("period-0").text === "1 April 2015 to 31 October 2015")
      assert(document.getElementById("return-type-1").text === "Rental business")
      assert(document.getElementById("period-1").text === "1 November 2015 to 31 March 2016")
      assert(document.getElementById("return-type-2") === null)
      assert(document.getElementById("period-2") === null)

      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("th-view-return-additional-info").text() === "Additional information")
      assert(document.getElementById("td-view-return-additional-info").text() === "additional details")

      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("th-view-return-tax-avoidance").text() === "Avoidance scheme reference number")
      assert(document.getElementById("td-view-return-tax-avoidance").text() === "ABCdefgh")
      assert(document.getElementById("th-view-return-promoter").text() === "Promoter reference number")
      assert(document.getElementById("td-view-return-promoter").text() === "12345678")
      assert(document.getElementById("th-view-return-payment").text() === "Payment reference")
      assert(document.getElementById("td-view-return-payment").text() === "1234567891")
      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("th-view-return-status").text() === "Status")
      assert(document.getElementById("td-view-return-status").text() === "Submitted")
      assert(document.getElementById("th-view-return-date").text() === "Return date")
      assert(document.getElementById("td-view-return-date").text() === "10 May 2015")


      assert(document.getElementById("return-charge-text").text() === "Based on the information you have given us your ATED charge is £9,324")
      assert(document.getElementById("return-charge").text() === "£9,324")

      assert(document.getElementById("submit").text() === "Change return")
    }

    Scenario("View the Form Bundle when we have a Form Bundle that has been disposed") {

      Given("the client is creating a new liability and want to add multiple periods")
      When("The user views the page")

      val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
      Then("The config should have - 2 periods")
      val displayPeriods = PeriodUtils.getDisplayPeriods(propertyDetails.period, 2015)
      assert(displayPeriods.size === 2)
      val valuesToDisplay = PeriodUtils.getOrderedReturnPeriodValues(viewWithDisposePeriod.lineItem, viewReturnWithMultiPeriod.dateOfAcquisition)
      val periodsToDisplay = PeriodUtils.getDisplayFormBundleProperties(viewWithDisposePeriod.lineItem, 2015)
      val html = injectedViewInstance(2015, Some(viewWithDisposePeriod), "formBundleNo", Some("ACME Ltd"), changeAllowed = false, editAllowed = false, valuesToDisplay, periodsToDisplay, Html(""), None)

      val document = Jsoup.parse(html.toString())

      Then("The header should match - View return")
      assert(document.getElementsByTag("h1").text contains "View return")

      Then("The subheader should be - ACME Ltd")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "You are logged in as: ACME Ltd")

      Then("The text should be - From the ATED chargeable period from 1 April 2015 to 31 March 2016.")
      assert(document.getElementById("form-bundle-text").text() === "From the ATED chargeable period from 1 April 2015 to 31 March 2016.")

      Then("The the fields should be correct")
      assert(document.getElementById("th-view-return-property").text() === "Property")
      assert(document.getElementById("th-view-return-property-address").text() === "Address")
      assert(document.getElementById("td-view-return-property-address").text().contains("100 addressLine1"))
      assert(document.getElementById("td-view-return-property-address").text().contains("addressLine2"))
      assert(document.getElementById("td-view-return-property-address").text().contains("XX11XX"))
      assert(document.getElementById("th-view-return-property-title-no").text() === "Property’s title number")
      assert(document.getElementById("td-view-return-property-title-no").text() === "title here")

      assert(document.getElementById("th-view-return-property-value").text() === "Value of the property")
      assert(document.getElementById("th-view-return-value-0").text() === "Value for the purposes of ATED")
      assert(document.getElementById("td-view-return-value-0").text() === "£100")
      assert(document.getElementById("th-view-return-property-valuation-date-0").text() === "Professionally valued")
      assert(document.getElementById("td-view-return-property-valuation-date-0").text() === "Yes")


      assert(document.getElementById("dates-of-liability-header").text() === "Dates of liability")
      assert(document.getElementById("return-type-0").text === "Liable for charge")
      assert(document.getElementById("period-0").text === "1 April 2015 to 31 October 2015")
      assert(document.getElementById("return-type-1").text === "Disposed of property")
      assert(document.getElementById("period-1").text === "1 November 2015")
      assert(document.getElementById("return-type-2") === null)
      assert(document.getElementById("period-2") === null)

      assert(document.getElementById("supporting-info-header").text() === "Supporting information")
      assert(document.getElementById("th-view-return-additional-info").text() === "Additional information")
      assert(document.getElementById("td-view-return-additional-info").text() === "additional details")

      assert(document.getElementById("avoidance-scheme-header").text() === "Avoidance scheme")
      assert(document.getElementById("th-view-return-tax-avoidance").text() === "Avoidance scheme reference number")
      assert(document.getElementById("td-view-return-tax-avoidance").text() === "ABCdefgh")
      assert(document.getElementById("th-view-return-promoter").text() === "Promoter reference number")
      assert(document.getElementById("td-view-return-promoter").text() === "12345678")
      assert(document.getElementById("th-view-return-payment").text() === "Payment reference")
      assert(document.getElementById("td-view-return-payment").text() === "1234567891")
      assert(document.getElementById("return-status-header").text() === "Return status")
      assert(document.getElementById("th-view-return-status").text() === "Status")
      assert(document.getElementById("td-view-return-status").text() === "Submitted")
      assert(document.getElementById("th-view-return-date").text() === "Return date")
      assert(document.getElementById("td-view-return-date").text() === "10 May 2015")


      assert(document.getElementById("return-charge-text").text() === "Based on the information you have given us your ATED charge is £9,324")
      assert(document.getElementById("return-charge").text() === "£9,324")

      assert(document.getElementById("submit") === null)
    }
  }
}
