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
import testhelpers.MockAuthUtil
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import utils.AtedConstants._

class periodSummarySpec extends FeatureSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with MockAuthUtil {

  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val organisationName = "OrganisationName"
  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789013"
  val formBundleNo3 = "123456789014"

  val draftReturns1 = DraftReturns(2015, "1", "desc", Some(BigDecimal(100.00)), TypeLiabilityDraft)
  val draftReturns2 = DraftReturns(2015, "", "some relief", None, TypeReliefDraft)
  val submittedReliefReturns1 = SubmittedReliefReturns(formBundleNo1, "some relief",
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

      val html = views.html.periodSummary(2015, None, None, Some("backLink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - GOV.UK")

      Then("We should only have the current tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns") === null)

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

    scenario("Show Data but no past returns if we have data") {

      Given("the client has no returns")
      When("The user views the page")

      val html = views.html.periodSummary(2015, Some(periodSummaryReturns), Some(organisationName), Some("http://backlink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - GOV.UK")

      Then("We should only have the current tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns") === null)

      Then("The table should have data")
      assert(document.getElementById("view-edit-0").text() === "View or change return, addr1+2")
      assert(document.getElementById("liability-submitted-0").attr("href") === "/ated/form-bundle/123456789013/2015")

      assert(document.getElementById("relief-submitted-0").text() === "View or change return, some relief")
      assert(document.getElementById("draft-liability-0").text() === "View or change return, desc")
      assert(document.getElementById("draft-relief-1").text() === "View or change return, some relief")

      assert(document.getElementById("backLinkHref").text() === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backlink")
    }

    scenario("Show Data but with past returns if we have data") {

      Given("the client has no returns")
      When("The user views the page")

      val html = views.html.periodSummary(2015, Some(periodSummaryReturnsWithOld), Some(organisationName), Some("http://backlink"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - Your ATED returns for 2015 to 2016")
      assert(document.title() === "Your ATED returns for 2015 to 2016 - GOV.UK")

      Then("We should only have the current tab")
      assert(document.getElementById("current-returns").text() === "Current returns")
      assert(document.getElementById("past-returns").text() === "Past returns")

      Then("The table should have data")
      assert(document.getElementById("view-edit-0").text() === "View or change return, addr1+2")
      assert(document.getElementById("liability-submitted-0").attr("href") === "/ated/form-bundle/123456789013/2015")

      assert(document.getElementById("relief-submitted-0").text() === "View or change return, some relief")
      assert(document.getElementById("draft-liability-0").text() === "View or change return, desc")
      assert(document.getElementById("draft-relief-1").text() === "View or change return, some relief")

      Then("Show the back link")
      assert(document.getElementById("backLinkHref").text() === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backlink")

      Then("add the link to create a return")
      assert(document.getElementById("create-return").text() === "Create a new return")
    }
  }
}
