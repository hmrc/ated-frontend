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

package views

import config.ApplicationConfig
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.AtedConstants._
import views.html.periodSummaryPastReturns

class periodSummaryPastReturnsSpec extends AnyFeatureSpec with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val injectedViewInstance: periodSummaryPastReturns = app.injector.instanceOf[views.html.periodSummaryPastReturns]

  val organisationName: String = "OrganisationName"
  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789013"
  val formBundleNo3: String = "123456789014"
  val year: Int = 2015
  val draftReturns1: DraftReturns = DraftReturns(year, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
  val draftReturns2: DraftReturns = DraftReturns(year, "", "some relief", None, TypeReliefDraft)
  val submittedReliefReturns1: SubmittedReliefReturns = SubmittedReliefReturns(formBundleNo1, "some relief",
    new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
  val submittedReliefReturns1Older: SubmittedReliefReturns = SubmittedReliefReturns(formBundleNo1, "some relief",
    new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-04-05"))
  val submittedLiabilityReturns1: SubmittedLiabilityReturns = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00),
    new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), changeAllowed = true, "payment-ref-01")
  val submittedLiabilityReturns2: SubmittedLiabilityReturns = SubmittedLiabilityReturns(formBundleNo3, "addr1+2", BigDecimal(1234.00),
    new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-06-06"), changeAllowed = true, "payment-ref-01")

  val submittedReturns: SubmittedReturns = SubmittedReturns(year, Seq(submittedReliefReturns1, submittedReliefReturns1Older), Seq(submittedLiabilityReturns1))
  val submittedReturnsWithOld: SubmittedReturns = SubmittedReturns(year, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1), Seq(submittedLiabilityReturns2))
  val periodSummaryReturns: PeriodSummaryReturns = PeriodSummaryReturns(year, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
  val periodSummaryReturnsWithOld: PeriodSummaryReturns = PeriodSummaryReturns(year, Seq(draftReturns1, draftReturns2), Some(submittedReturnsWithOld))
  Feature("The user can view their returns") {

    info("as a client i want to be able to view my returns")

    Scenario("Show None if we have no returns") {

      Given("the client has no returns")
      When("The user views the page")

      val html = injectedViewInstance(year, None, None,  Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - GOV.UK")

      Then("We should only have the current tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns").text() === "Past returns")

      Then("The summary list should have no data")
      assert(document.getElementById("view-edit-0") === null)
      assert(document.getElementById("liability-submitted-0") === null)
      assert(document.getElementById("relief-submitted-0") === null)
      assert(document.getElementById("draft-liability-0") === null)
      assert(document.getElementById("draft-relief-1") === null)

      assert(document.getElementsByClass("govuk-back-link").text === "Back")

      Then("add the link to create a return")
      assert(document.getElementById("create-return").text() === "Create a new return")
    }

    Scenario("Show No data if we only have new data") {

      Given("the client has no returns")
      When("The user views the page")

      val html = injectedViewInstance(year, Some(periodSummaryReturns), Some(organisationName), Html(""), Some("http://backlink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - GOV.UK")

      Then("We should only have the current tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns").text() === "Past returns")

      Then("The summary list should have data")
      assert(document.getElementById("view-edit-0") === null)
      assert(document.getElementById("liability-submitted-0") === null)

      assert(document.getElementById("relief-submitted-0").text() === "View")

      assert(document.getElementsByClass("govuk-back-link").text() === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backlink")

      Then("add the link to create a return")
      assert(document.getElementById("create-return").text() === "Create a new return")
    }

    Scenario("Show old data if we have some") {

      Given("the client has no returns")
      When("The user views the page")

      val html = injectedViewInstance(year, Some(periodSummaryReturnsWithOld), Some(organisationName), Html(""), Some("http://backlink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - GOV.UK")

      Then("We should only have the current tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns").text() === "Past returns")

      Then("The summary list should have data")
      assert(document.select("#liability-submitted-0").text() === "View or change")
      assert(document.select("#liability-submitted-0").attr("href") === "/ated/form-bundle/123456789014/2015")

      assert(document.getElementsByClass("govuk-back-link").text() === "Back")
      assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backlink")

      Then("add the link to create a return")
      assert(document.getElementById("create-return").text() === "Create a new return")
    }
  }
}
