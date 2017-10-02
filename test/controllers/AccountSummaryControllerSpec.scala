/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.{AgentClientMandateFrontendConnector, DataCacheConnector}
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.{DetailsService, SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.play.frontend.auth.DummyDelegationData
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.HtmlPartial
import utils.AtedConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, UserId }

class AccountSummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockReturnSummaryService = mock[SummaryReturnsService]
  val mockSubscriptionDataService = mock[SubscriptionDataService]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockDetailsService = mock[DetailsService]
  val mockAgentClientMandateFrontendConnector = mock[AgentClientMandateFrontendConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val organisationName = "OrganisationName"
  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789013"

  implicit def atedContext2AuthContext(implicit atedContext: AtedContext) = atedContext.user.authContext
  implicit val hc: HeaderCarrier = HeaderCarrier()

  object TestAccountSummaryController extends AccountSummaryController {
    override val authConnector = mockAuthConnector
    override val summaryReturnsService = mockReturnSummaryService
    override val subscriptionDataService = mockSubscriptionDataService
    override val delegationConnector = mockDelegationConnector
    override val mandateFrontendConnector = mockAgentClientMandateFrontendConnector
    override val detailsService = mockDetailsService
    override val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockReturnSummaryService)
    reset(mockSubscriptionDataService)
    reset(mockDelegationConnector)
    reset(mockAgentClientMandateFrontendConnector)
    reset(mockDetailsService)
    reset(mockDataCacheConnector)
  }

  "AccountSummaryController" must {

    "use correct DelegationConnector" in {
      AccountSummaryController.delegationConnector must be(FrontendDelegationConnector)
    }

    "accountSummary" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/account-summary"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
      }

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the account summary view V_2_0 with balance (debit) if we have some Summary data" in {
          val address = Address(name1 = Some("name1"), name2 = Some("name2"), contactDetails = Some(ContactDetails(phoneNumber = Some("03000123456789"), mobileNumber = Some("09876543211"), emailAddress = Some("aa@aa.com"), faxNumber = Some("0223344556677"))), addressDetails = AddressDetails(AddressTypeCorrespondence, "addrLine1", "addrLine2", None, None, None, "GB"))

          val draftReturns1 = DraftReturns(2015, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
          val draftReturns2 = DraftReturns(2015, "", "some relief", None, TypeReliefDraft)
          val submittedReliefReturns1 = SubmittedReliefReturns(formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
          val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), true, "payment-ref-01")
          val submittedReturns = SubmittedReturns(2015, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
          val periodSummaryReturns = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
          val data = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))
          getWithAuthorisedUser(data, Some(address)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("sidebar.balance-header").text() must be("Your balance")
              document.getElementById("sidebar.balance-content").text() must be("£1,000 debit")
              document.getElementById("sidebar.link-text").text() must be("Deadlines and ways to pay")
              document.getElementById("sidebar.balance-info").text() must be("There can be a 24 hour delay before you see any updates to your balance")
              document.getElementById("sidebar.link-text").text() must be("Deadlines and ways to pay")
              document.getElementById("sidebar.link-text").attr("href") must be("https://www.gov.uk/guidance/pay-annual-tax-on-enveloped-dwellings")
              document.title() must be("Your ATED online service")
              document.getElementById("change-details-link").text() must be("View your ATED details")

              document.title() must be("Your ATED online service")
              document.getElementById("account-summary-header").text() must be("Your ATED online service")
              document.getElementById("return-summary-th-period").text() must be("Period")
              document.getElementById("return-summary-th-chargeable").text() must be("Chargeable")
              document.getElementById("return-summary-th-reliefs").text() must be("Relief")
              document.getElementById("return-summary-th-drafts").text() must be("Draft")
              document.getElementById("return-summary-th-action").text() must be("hidden text for empty th")
              document.getElementsByClass("return-summary-td-chargeable").text() must be("1")
              document.getElementsByClass("return-summary-td-reliefs").text() must be("1")
              document.getElementsByClass("return-summary-td-drafts").text() must be("2")
              document.getElementById("view-change-0").text() must include("View or change")
              document.getElementById("create-return").text() must be("Create a new return")

              Option(document.getElementById("return-summary-no-returns")) must be(None)
          }
        }

        "show the account summary view V_2_0 with balance (credit) if we have some Summary data" in {
          val address = Address(addressDetails = AddressDetails(AddressTypeCorrespondence, "addrLine1", "addrLine2", None, None, None, "GB"))

          val draftReturns1 = DraftReturns(2015, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
          val draftReturns2 = DraftReturns(2015, "", "some relief", None, TypeReliefDraft)
          val submittedReliefReturns1 = SubmittedReliefReturns(formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
          val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), true, "payment-ref-01")
          val submittedReturns = SubmittedReturns(2015, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
          val periodSummaryReturns = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
          val data = SummaryReturnsModel(Some(BigDecimal(-999.99)), Seq(periodSummaryReturns))
          getWithAuthorisedUser(data, Some(address)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("sidebar.balance-header").text() must be("Your balance")
              document.getElementById("sidebar.balance-content").text() must be("£1,000 credit")
              document.getElementById("sidebar.balance-info").text() must be("There can be a 24 hour delay before you see any updates to your balance")
              document.getElementById("sidebar.link-text").text() must be("Ways to be paid")
              document.getElementById("sidebar.link-text").attr("href") must be("https://www.gov.uk/guidance/pay-annual-tax-on-enveloped-dwellings")
              document.title() must be("Your ATED online service")
              document.getElementById("account-summary-header").text() must be("Your ATED online service")
              document.getElementById("return-summary-th-period").text() must be("Period")
              document.getElementsByClass("return-summary-td-chargeable").text() must be("1")
              document.getElementsByClass("return-summary-td-reliefs").text() must be("1")
              document.getElementsByClass("return-summary-td-drafts").text() must be("2")

              Option(document.getElementById("return-summary-no-returns")) must be(None)
          }
        }

        "show the account summary view V_2_0 with balance if we have some Summary data and balance is 0" in {
          val address = Address(addressDetails = AddressDetails(AddressTypeCorrespondence, "addrLine1", "addrLine2", None, None, None, "GB"))

          val draftReturns1 = DraftReturns(2015, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
          val draftReturns2 = DraftReturns(2015, "", "some relief", None, TypeReliefDraft)
          val submittedReliefReturns1 = SubmittedReliefReturns(formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
          val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), true, "payment-ref-01")
          val submittedReturns = SubmittedReturns(2015, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
          val periodSummaryReturns = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
          val data = SummaryReturnsModel(Some(BigDecimal(0)), Seq(periodSummaryReturns))
          getWithAuthorisedUser(data, Some(address)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("sidebar.balance-header").text() must be("Your balance")
              document.getElementById("sidebar.balance-content").text() must be("£0")


              document.title() must be("Your ATED online service")
              document.getElementById("account-summary-header").text() must be("Your ATED online service")

              document.getElementById("return-summary-th-period").text() must be("Period")
              document.getElementsByClass("return-summary-td-chargeable").text() must be("1")
              document.getElementsByClass("return-summary-td-reliefs").text() must be("1")
              document.getElementsByClass("return-summary-td-drafts").text() must be("2")

              Option(document.getElementById("return-summary-no-returns")) must be(None)
          }
        }
        

        "show the create a return and appoint an agent link if there are no returns and no delegation" in {
          val data = SummaryReturnsModel(None, Seq())
          getWithAuthorisedUser(data, None) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Your ATED online service")
              document.getElementById("create-return").hasClass("link") must be(true)
              document.getElementById("create-return").hasClass("button") must be(false)
              document.getElementById("appoint-agent").hasClass("link") must be(true)
          }
        }

        "show the create a return button and no appoint an agent link if there are no returns and there is delegation" in {
          val data = SummaryReturnsModel(None, Seq())
          getWithAuthorisedDelegatedUser(data, None) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Your ATED online service")
              document.getElementById("create-return").hasClass("link") must be(false)
              document.getElementById("create-return").hasClass("button") must be(true)
              Option(document.getElementById("appoint-agent")) must be(None)
          }
        }

        "throw exception for no safe id" in {
          val data = SummaryReturnsModel(None, Seq())
          val userId = s"user-${UUID.randomUUID}"
          implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
          AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

          when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(200)))
          when(mockReturnSummaryService.getSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(data))
          when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
          when(mockDetailsService.cacheClientReference(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful("XN1200000100001"))
          when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
          when(mockSubscriptionDataService.getSafeId(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
          when(mockAgentClientMandateFrontendConnector.getClientBannerPartial(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))

          val result = TestAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must be("Could not get safeId")
        }

        "show the create a return button and no appoint an agent link if there are returns and delegation" in {
          val draftReturns1 = DraftReturns(2015, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
          val draftReturns2 = DraftReturns(2015, "", "some relief", None, TypeReliefDraft)
          val submittedReliefReturns1 = SubmittedReliefReturns(formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
          val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), true, "payment-ref-01")
          val submittedReturns = SubmittedReturns(2015, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
          val periodSummaryReturns = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
          val data = SummaryReturnsModel(Some(BigDecimal(0)), Seq(periodSummaryReturns))
          getWithAuthorisedDelegatedUser(data, None) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Your ATED online service")
              document.getElementById("create-return").hasClass("link") must be(true)
              document.getElementById("create-return").hasClass("button") must be(false)
              Option(document.getElementById("appoint-agent")) must be(None)
          }
        }
      }
    }
  }


  def getWithAuthorisedUser(returnsSummaryWithDraft: SummaryReturnsModel,
                            correspondence: Option[Address] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(200)))
    when(mockReturnSummaryService.getSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnsSummaryWithDraft))
    when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(correspondence))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    when(mockSubscriptionDataService.getSafeId(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("safeId")))
    when(mockAgentClientMandateFrontendConnector.getClientBannerPartial(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
    when(mockDetailsService.cacheClientReference(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful("XN1200000100001"))

    val result = TestAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedDelegatedUser(returnsSummaryWithDraft: SummaryReturnsModel,
                                     correspondence: Option[Address] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createDelegatedAuthContext(userId, "company name|display name"))
    implicit val hc: HeaderCarrier = HeaderCarrier(userId = Some(UserId(userId)))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(200)))
    when(mockReturnSummaryService.getSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnsSummaryWithDraft))
    when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(correspondence))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    when(mockDelegationConnector.getDelegationData(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(DummyDelegationData.returnData)))
    when(mockSubscriptionDataService.getSafeId(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("safeId")))
    when(mockAgentClientMandateFrontendConnector.getClientBannerPartial(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
    when(mockDetailsService.cacheClientReference(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful("XN1200000100001"))
    val result = TestAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSessionDelegation(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

}
