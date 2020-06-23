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

package views

import config.ApplicationConfig
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.MockAuthUtil
import utils.AtedConstants._

class periodSummaryPastReturnsSpec extends FeatureSpec with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val organisationName: String = "OrganisationName"
  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789013"
  val formBundleNo3: String = "123456789014"
  val draftReturns1: DraftReturns = DraftReturns(2015, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft, LocalDate.now())
  val draftReturns2: DraftReturns = DraftReturns(2015, "", "some relief", None, TypeReliefDraft, LocalDate.now())
  val submittedReliefReturns1: SubmittedReliefReturns = SubmittedReliefReturns(formBundleNo1, "some relief",
    new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
  val submittedReliefReturns1Older = SubmittedReliefReturns(formBundleNo1, "some relief",
    new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-04-05"))
  val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00),
    new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), changeAllowed = true, "payment-ref-01")
  val submittedLiabilityReturns2 = SubmittedLiabilityReturns(formBundleNo3, "addr1+2", BigDecimal(1234.00),
    new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-06-06"), changeAllowed = true, "payment-ref-01")

  val submittedReturns = SubmittedReturns(2015, Seq(submittedReliefReturns1, submittedReliefReturns1Older), Seq(submittedLiabilityReturns1))
  val submittedReturnsWithOld = SubmittedReturns(2015, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1), Seq(submittedLiabilityReturns2))
  val periodSummaryReturns = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
  val periodSummaryReturnsWithOld = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), Some(submittedReturnsWithOld))
  feature("The user can view their returns") {

    info("as a client i want to be able to view my returns")

    scenario("Show None if we have no returns") {

      Given("the client has no returns")
      When("The user views the page")

      val html = views.html.periodSummaryPastReturns(2015, None, None,  Html(""), Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - GOV.UK")

      Then("We should only have the current tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns").text() === "Past returns")

      Then("The table should have no data")
      assert(document.getElementById("view-edit-0") === null)
      assert(document.getElementById("liability-submitted-0") === null)
      assert(document.getElementById("relief-submitted-0") === null)
      assert(document.getElementById("draft-liability-0") === null)
      assert(document.getElementById("draft-relief-1") === null)

      assert(document.getElementById("backLinkHref").text === "Back")

      Then("add the link to create a return")
      assert(document.getElementById("create-return").text() === "Create a new return")
    }

    scenario("Show No data if we only have new data") {

      Given("the client has no returns")
      When("The user views the page")

      val html = views.html.periodSummaryPastReturns(2015, Some(periodSummaryReturns), Some(organisationName), Html(""), Some("http://backlink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - GOV.UK")

      Then("We should only have the current tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns").text() === "Past returns")

      Then("The table should have data")
      assert(document.getElementById("view-edit-0") === null)
      assert(document.getElementById("liability-submitted-0") === null)

      assert(document.getElementById("relief-submitted-0").text() === "View")

      assert(document.getElementById("backLinkHref").text() === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backlink")

      Then("add the link to create a return")
      assert(document.getElementById("create-return").text() === "Create a new return")
    }

    scenario("Show old data if we have some") {

      Given("the client has no returns")
      When("The user views the page")

      val html = views.html.periodSummaryPastReturns(2015, Some(periodSummaryReturnsWithOld), Some(organisationName), Html(""), Some("http://backlink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - GOV.UK")

      Then("We should only have the current tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns").text() === "Past returns")

      Then("The table should have data")
      assert(document.select("#liability-submitted-0 a").text() === "View or change")
      assert(document.select("#liability-submitted-0 a").attr("href") === "/ated/form-bundle/123456789014/2015")

      assert(document.getElementById("backLinkHref").text() === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backlink")

      Then("add the link to create a return")
      assert(document.getElementById("create-return").text() === "Create a new return")
    }
  }
}
