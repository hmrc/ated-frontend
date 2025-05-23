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

import config.ApplicationConfig
import models._
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
import utils.AtedConstants._
import views.html.periodSummary

class periodSummarySpec extends AnyFeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val injectedViewInstance: periodSummary = app.injector.instanceOf[views.html.periodSummary]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val organisationName = "OrganisationName"
  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789013"
  val formBundleNo3 = "123456789014"

  val draftReturns1: DraftReturns = DraftReturns(2015, "1", "draftDescription", Some(BigDecimal(100.00)), TypeLiabilityDraft)
  val draftReturns2: DraftReturns = DraftReturns(2015, "", "draftReturn", None, TypeReliefDraft)

  val submittedReliefReturns1: SubmittedReliefReturns = SubmittedReliefReturns(formBundleNo1, "reliefType1",
    LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"))
  val submittedReliefReturns1Older: SubmittedReliefReturns = SubmittedReliefReturns(formBundleNo1, "reliefType2",
    LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"), LocalDate.parse("2015-04-05"))
  val submittedLiabilityReturns1: SubmittedLiabilityReturns = SubmittedLiabilityReturns(formBundleNo2, "addressLine1+2", BigDecimal(1234.00),
    LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"), changeAllowed = true, "payment-ref-01")
  val submittedLiabilityReturns2: SubmittedLiabilityReturns = SubmittedLiabilityReturns(formBundleNo3, "addressLine1+2", BigDecimal(1234.00),
    LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"), LocalDate.parse("2015-06-06"), changeAllowed = true, "payment-ref-01")

  val submittedReturns: SubmittedReturns = SubmittedReturns(2015, Seq(submittedReliefReturns1, submittedReliefReturns1Older), Seq(submittedLiabilityReturns1))
  val submittedReturnsWithOld: SubmittedReturns = SubmittedReturns(2015, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1), Seq(submittedLiabilityReturns2))

  val periodSummaryReturns: PeriodSummaryReturns = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
  val periodSummaryReturnsWithOld: PeriodSummaryReturns = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), Some(submittedReturnsWithOld))
  val previousPeriodSummaryReturns: PeriodSummaryReturns = PeriodSummaryReturns(2015, Nil, Some(submittedReturnsWithOld))

  val currentReturnsTabText: String = "address, addressLine1+2 Status Submitted View or change the return for addressLine1+2 " +
    "relief type, reliefType1 Status Submitted View or change the return for reliefType1 " +
    "relief type, reliefType2 Status Submitted View or change the return for reliefType2 address, " +
    "draftDescription Status Draft View or change the return for draftDescription address, " +
    "draftReturn Status Draft View or change the return for draftReturn"

  val currentReturnsTabTextWithOld: String = "address, addressLine1+2 Status Submitted View or change the return for addressLine1+2 relief type," +
    " reliefType1 Status Submitted View or change the return for reliefType1 address," +
    " draftDescription Status Draft View or change the return for draftDescription address, " +
    "draftReturn Status Draft View or change the return for draftReturn"

  val previousReturnsTabText: String = "address, addressLine1+2 Status Submitted View or change the return for addressLine1+2 relief type," +
    " reliefType1 Status Submitted View or change the return for reliefType1"

  Feature("The user can view their returns") {
    info("as a client i want to be able to view my returns")

    Scenario("Show no return data if we have no returns") {

      Given("the client has no returns")
      When("The user views the page")

      val html = injectedViewInstance(2015, None, None, None, Html(""), Some("backLink"))


      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - Submit and view your ATED returns - GOV.UK")

      Then("We should only have the current tab visible")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns") === null)

      Then("The summary list should have no data")
      assert(document.getElementById("view-edit-0") === null)
      assert(document.getElementById("liability-submitted-0") === null)
      assert(document.getElementById("relief-submitted-0") === null)
      assert(document.getElementById("draft-liability-0") === null)
      assert(document.getElementById("draft-relief-1") === null)

      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")

      Then("add the link to create a return")
      assert(document.getElementById("create-return").text() === "Create a new return")
    }

    Scenario("Show the current returns tab without the previous returns tab when there are no previous returns") {

      Given("The client has no previous returns")
      When("The user views the page")

      val html = injectedViewInstance(2015, Some(periodSummaryReturns), None, Some(organisationName), Html(""), Some("http://backlink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - Submit and view your ATED returns - GOV.UK")

      Then("We should only have the current tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns") === null)

      Then("The summary list should have data")
      assert(document.getElementById("current-returns-tab-content").text() === currentReturnsTabText)
      assert(document.getElementById("create-return").text() === "Create a new return")
      assert(document.select("#current-liability-submitted-0").text() === "View or change the return for addressLine1+2")
      assert(document.select("#current-liability-submitted-0").attr("href") === s"/ated/form-bundle/123456789013/2015")

      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backlink")
    }

    Scenario("Show both the current returns tab and the previous returns tab when there are previous returns.") {

      Given("the client has previous returns")
      When("The user views the page")

      val html = injectedViewInstance(2015, Some(periodSummaryReturnsWithOld), Some(previousPeriodSummaryReturns), Some(organisationName), Html(""), Some("http://backlink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - Submit and view your ATED returns - GOV.UK")

      Then("We should have both the current tab and the past returns tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns").text() === "Past returns")

      Then("The summary list should have data")
      assert(document.getElementById("current-returns-tab-content").text() === currentReturnsTabTextWithOld)
      assert(document.getElementById("create-return").text() === "Create a new return")
      assert(document.getElementById("previous-returns-tab-content").text() === previousReturnsTabText)
      assert(document.getElementById("previous-relief-submitted-0").text() === "View or change the return for reliefType1")
      assert(document.getElementById("current-draft-liability-0").text() === "View or change the return for draftDescription")

      assert(document.getElementById("current-draft-relief-1").text() === "View or change the return for draftReturn")

      assert(document.getElementById("previous-liability-submitted-0").text() === "View or change the return for addressLine1+2")

      assert(document.select("#current-liability-submitted-0").attr("href") === s"/ated/form-bundle/123456789013/2015")

      Then("Show the back link")
      assert(document.select(".govuk-width-container > a.govuk-back-link").text === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backlink")

      Then("add the link to create a return")
      assert(document.getElementById("create-return").text() === "Create a new return")
    }
  }
}
