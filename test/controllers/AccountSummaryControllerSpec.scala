/*
 * Copyright 2019 HM Revenue & Customs
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

import builders.{SessionBuilder, TitleBuilder}
import connectors.{AgentClientMandateFrontendConnector, DataCacheConnector}
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import play.twirl.api.Html
import services.{DelegationService, DetailsService, SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UserId}
import uk.gov.hmrc.play.partials.HtmlPartial
import utils.AtedConstants._
import utils.MockAuthUtil

import scala.concurrent.Future

class AccountSummaryControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  val mockReturnSummaryService: SummaryReturnsService = mock[SummaryReturnsService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockDetailsService: DetailsService = mock[DetailsService]
  val mockAgentClientMandateFrontendConnector: AgentClientMandateFrontendConnector = mock[AgentClientMandateFrontendConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val organisationName: String = "OrganisationName"
  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789013"
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  object TestAccountSummaryController extends AccountSummaryController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val summaryReturnsService: SummaryReturnsService = mockReturnSummaryService
    override val subscriptionDataService: SubscriptionDataService = mockSubscriptionDataService
    override val delegationService: DelegationService = mockDelegationService
    override val mandateFrontendConnector: AgentClientMandateFrontendConnector = mockAgentClientMandateFrontendConnector
    override val detailsService: DetailsService = mockDetailsService
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockReturnSummaryService)
    reset(mockSubscriptionDataService)
    reset(mockAgentClientMandateFrontendConnector)
    reset(mockDetailsService)
    reset(mockDataCacheConnector)
  }

  "AccountSummaryController" must {

    "use correct DelegationConnector" in {
      AccountSummaryController.delegationService must be(DelegationService)
    }

    "accountSummary" must {

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

        "respond with a redirect to unauthorised URL" in {
          val data = SummaryReturnsModel(None, Seq())
          getWithForbiddenUser(data, None) { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the account summary view V_2_0 with balance (debit) if we have some Summary data" in {
          val year = 2015
          val address = {
            Address(name1 = Some("name1"),
              name2 = Some("name2"),
              contactDetails = Some(ContactDetails(phoneNumber = Some("03000123456789"),
                mobileNumber = Some("09876543211"),
                emailAddress = Some("aa@aa.com"),
                faxNumber = Some("0223344556677"))),
              addressDetails = AddressDetails(AddressTypeCorrespondence, "addrLine1", "addrLine2", None, None, None, "GB"))}

          val draftReturns1 = DraftReturns(year, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
          val draftReturns2 = DraftReturns(year, "", "some relief", None, TypeReliefDraft)
          val submittedReliefReturns1 = SubmittedReliefReturns(
            formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
          val submittedLiabilityReturns1 = SubmittedLiabilityReturns(
            formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"),
            new LocalDate("2015-05-05"), changeAllowed = true, "payment-ref-01")
          val submittedReturns = SubmittedReturns(year, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
          val periodSummaryReturns = PeriodSummaryReturns(year, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
          val data = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))
          getWithAuthorisedUser(data, Some(address)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("sidebar.balance-header").text() must be("Your balance")
              document.getElementById("sidebar.balance-content").text() must be("£1,000 debit")
              document.getElementById("sidebar.link-text").text() must be("Deadlines and ways to pay")
              document.getElementById("sidebar.balance-info").text() must be("There can be a 24-hour delay before you see any updates to your balance.")
              document.getElementById("sidebar.link-text").text() must be("Deadlines and ways to pay")
              document.getElementById("sidebar.link-text").attr("href") must be("https://www.gov.uk/guidance/pay-annual-tax-on-enveloped-dwellings")
              document.title() must be(TitleBuilder.buildTitle("Your ATED online service"))
              document.getElementById("change-details-link").text() must be("View your ATED details")

              document.title() must be(TitleBuilder.buildTitle("Your ATED online service"))
              document.getElementById("account-summary-header").text() must be("Your ATED online service")
              document.getElementById("return-summary-period-heading").text() must be("Period")
              document.getElementById("return-summary-chargeable-heading").text() must be("Chargeable")
              document.getElementById("return-summary-reliefs-heading").text() must be("Relief")
              document.getElementById("return-summary-drafts-heading").text() must be("Draft")
              document.getElementById("return-summary-chargeable-data-0").text().toLowerCase() must be("number of chargeable 1")
              document.getElementById("return-summary-reliefs-data-0").text().toLowerCase() must be("number of relief 1")
              document.getElementById("return-summary-drafts-data-0").text().toLowerCase() must be("number of draft 2")
              document.getElementById("view-change-0").text() must include("View or change")
              document.getElementById("create-return").text() must be("Create a new return")

              Option(document.getElementById("return-summary-no-returns")) must be(None)
          }
        }

        "show the account summary view V_2_0 with balance (credit) if we have some Summary data" in {
          val year = 2015
          val address = Address(addressDetails = AddressDetails(AddressTypeCorrespondence, "addrLine1", "addrLine2", None, None, None, "GB"))
          val draftReturns1 = DraftReturns(year, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
          val draftReturns2 = DraftReturns(year, "", "some relief", None, TypeReliefDraft)
          val submittedReliefReturns1 = SubmittedReliefReturns(
            formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
          val submittedLiabilityReturns1 = SubmittedLiabilityReturns(
            formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"),
            new LocalDate("2015-05-05"), changeAllowed = true, "payment-ref-01")
          val submittedReturns = SubmittedReturns(year, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
          val periodSummaryReturns = PeriodSummaryReturns(year, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
          val data = SummaryReturnsModel(Some(BigDecimal(-999.99)), Seq(periodSummaryReturns))
          getWithAuthorisedUser(data, Some(address)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("sidebar.balance-header").text() must be("Your balance")
              document.getElementById("sidebar.balance-content").text() must be("£1,000 credit")
              document.getElementById("sidebar.balance-info").text() must be("There can be a 24-hour delay before you see any updates to your balance.")
              document.getElementById("sidebar.link-text").text() must be("Ways to be paid")
              document.getElementById("sidebar.link-text").attr("href") must be("https://www.gov.uk/guidance/pay-annual-tax-on-enveloped-dwellings")
              document.title() must be(TitleBuilder.buildTitle("Your ATED online service"))
              document.getElementById("account-summary-header").text() must be("Your ATED online service")
              document.getElementById("return-summary-period-heading").text() must be("Period")
              document.getElementById("return-summary-chargeable-data-0").text().toLowerCase() must be("number of chargeable 1")
              document.getElementById("return-summary-reliefs-data-0").text().toLowerCase() must be("number of relief 1")
              document.getElementById("return-summary-drafts-data-0").text().toLowerCase() must be("number of draft 2")

              Option(document.getElementById("return-summary-no-returns")) must be(None)
          }
        }

        "show the account summary view V_2_0 with balance if we have some Summary data and balance is 0" in {
          val year = 2015
          val address = Address(addressDetails = AddressDetails(AddressTypeCorrespondence, "addrLine1", "addrLine2", None, None, None, "GB"))

          val draftReturns1 = DraftReturns(year, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
          val draftReturns2 = DraftReturns(year, "", "some relief", None, TypeReliefDraft)
          val submittedReliefReturns1 = SubmittedReliefReturns(
            formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
          val submittedLiabilityReturns1 = SubmittedLiabilityReturns(
            formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"),
            new LocalDate("2015-05-05"), changeAllowed = true, "payment-ref-01")
          val submittedReturns = SubmittedReturns(year, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
          val periodSummaryReturns = PeriodSummaryReturns(year, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
          val data = SummaryReturnsModel(Some(BigDecimal(0)), Seq(periodSummaryReturns))
          getWithAuthorisedUser(data, Some(address)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("sidebar.balance-header").text() must be("Your balance")
              document.getElementById("sidebar.balance-content").text() must be("£0")


              document.title() must be(TitleBuilder.buildTitle("Your ATED online service"))
              document.getElementById("account-summary-header").text() must be("Your ATED online service")

              document.getElementById("return-summary-period-heading").text() must be("Period")
              document.getElementById("return-summary-chargeable-data-0").text().toLowerCase() must be("number of chargeable 1")
              document.getElementById("return-summary-reliefs-data-0").text().toLowerCase() must be("number of relief 1")
              document.getElementById("return-summary-drafts-data-0").text().toLowerCase() must be("number of draft 2")

              Option(document.getElementById("return-summary-no-returns")) must be(None)
          }
        }

        "show the account summary view with UR banner" in {
          val year = 2015
          val address = Address(addressDetails = AddressDetails(AddressTypeCorrespondence, "addrLine1", "addrLine2", None, None, None, "GB"))

          val draftReturns1 = DraftReturns(year, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
          val draftReturns2 = DraftReturns(year, "", "some relief", None, TypeReliefDraft)
          val submittedReliefReturns1 = SubmittedReliefReturns(
            formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
          val submittedLiabilityReturns1 = SubmittedLiabilityReturns(
            formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"),
            new LocalDate("2015-05-05"), changeAllowed = true, "payment-ref-01")
          val submittedReturns = SubmittedReturns(year, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
          val periodSummaryReturns = PeriodSummaryReturns(year, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
          val data = SummaryReturnsModel(Some(BigDecimal(0)), Seq(periodSummaryReturns))
          getWithAuthorisedUser(data, Some(address)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("ur-panel") must not be null
              document.getElementById("ur-panel")
                .text() must be ("Help improve digital services by joining the HMRC user panel (opens in new window) No thanks")
              document.getElementsByClass("banner-panel__close").text() must be("No thanks")
          }
        }

        "show the create a return and appoint an agent link if there are no returns and no delegation" in {
          val data = SummaryReturnsModel(None, Seq())
          getWithAuthorisedUser(data, None) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Your ATED online service"))
              document.getElementById("create-return") != null
              document.getElementById("create-return") == null
              document.getElementById("appoint-agent") != null
          }
        }

        "show the create a return button and no appoint an agent link if there are no returns and there is delegation" in {
          val data = SummaryReturnsModel(None, Seq())
          getWithAuthorisedDelegatedUser(data, None) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Your ATED online service"))
              document.getElementById("create-return").hasClass("link") must be(false)
              document.getElementById("create-return") != null
              Option(document.getElementById("appoint-agent")) must be(None)
          }
        }

        "throw exception for no safe id" in {
          val httpValue = 200
          val data = SummaryReturnsModel(None, Seq())
          val userId = s"user-${UUID.randomUUID}"
          val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
          setAuthMocks(authMock)
          when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(httpValue)))
          when(mockReturnSummaryService.getSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(data))
          when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
          when(mockDetailsService.cacheClientReference(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful("XN1200000100001"))
          when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
          when(mockSubscriptionDataService.getSafeId(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
          when(mockAgentClientMandateFrontendConnector.getClientBannerPartial(Matchers.any(), Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))

          val result = TestAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must be("Could not get safeId")
        }

        "show the create a return button and no appoint an agent link if there are returns and delegation" in {
          val year = 2015
          val draftReturns1 = DraftReturns(year, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
          val draftReturns2 = DraftReturns(year, "", "some relief", None, TypeReliefDraft)
          val submittedReliefReturns1 = SubmittedReliefReturns(
            formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
          val submittedLiabilityReturns1 = SubmittedLiabilityReturns(
            formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"),
            new LocalDate("2015-05-05"), changeAllowed = true, "payment-ref-01")
          val submittedReturns = SubmittedReturns(year, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
          val periodSummaryReturns = PeriodSummaryReturns(year, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
          val data = SummaryReturnsModel(Some(BigDecimal(0)), Seq(periodSummaryReturns))
          getWithAuthorisedDelegatedUser(data, None) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Your ATED online service"))
              document.getElementById("create-return") != null
              document.getElementById("create-return").hasClass("button") must be(false)
              Option(document.getElementById("appoint-agent")) must be(None)
          }
        }
      }
    }
  }

  def getWithAuthorisedUser(returnsSummaryWithDraft: SummaryReturnsModel,
                            correspondence: Option[Address] = None)(test: Future[Result] => Any) {
    val httpValue = 200
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)

    when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(httpValue)))
    when(mockReturnSummaryService.getSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnsSummaryWithDraft))
    when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(correspondence))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    when(mockSubscriptionDataService.getSafeId(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("safeId")))
    when(mockAgentClientMandateFrontendConnector.getClientBannerPartial(Matchers.any(), Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
    when(mockDetailsService.cacheClientReference(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful("XN1200000100001"))

    val result = TestAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithForbiddenUser(returnsSummaryWithDraft: SummaryReturnsModel,
                            correspondence: Option[Address] = None)(test: Future[Result] => Any) {
    val httpValue = 200
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setForbiddenAuthMocks(authMock)

    when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(httpValue)))
    when(mockReturnSummaryService.getSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnsSummaryWithDraft))
    when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(correspondence))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    when(mockSubscriptionDataService.getSafeId(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("safeId")))
    when(mockAgentClientMandateFrontendConnector.getClientBannerPartial(Matchers.any(), Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
    when(mockDetailsService.cacheClientReference(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful("XN1200000100001"))

    val result = TestAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedDelegatedUser(returnsSummaryWithDraft: SummaryReturnsModel,
                                     correspondence: Option[Address] = None)(test: Future[Result] => Any) {
    val httpValue = 200
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier(userId = Some(UserId(userId)))
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(httpValue)))
    when(mockReturnSummaryService.getSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnsSummaryWithDraft))
    when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(correspondence))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    when(mockSubscriptionDataService.getSafeId(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("safeId")))
    when(mockAgentClientMandateFrontendConnector.getClientBannerPartial(Matchers.any(), Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
    when(mockDetailsService.cacheClientReference(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful("XN1200000100001"))
    val result = TestAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSessionDelegation(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
