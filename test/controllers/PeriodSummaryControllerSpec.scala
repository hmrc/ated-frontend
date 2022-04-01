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

package controllers

import java.util.UUID
import builders.SessionBuilder
import config.ApplicationConfig
import connectors.BackLinkCacheConnector
import controllers.auth.AuthAction
import controllers.editLiability.DisposePropertyController
import controllers.propertyDetails.{AddressLookupController, PropertyDetailsSummaryController}
import controllers.reliefs.ReliefsSummaryController
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{ServiceInfoService, SubscriptionDataService, SummaryReturnsService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import views.html.{BtaNavigationLinks, periodSummary, periodSummaryPastReturns}

import scala.concurrent.Future

class PeriodSummaryControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockSummaryReturnsService: SummaryReturnsService = mock[SummaryReturnsService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockReturnTypeController: ReturnTypeController = mock[ReturnTypeController]
  val mockReliefsSummaryController: ReliefsSummaryController = mock[ReliefsSummaryController]
  val mockPropertyDetailsSummaryController: PropertyDetailsSummaryController = mock[PropertyDetailsSummaryController]
  val mockAddressLookupController: AddressLookupController = mock[AddressLookupController]
  val mockDisposePropertyController: DisposePropertyController = mock[DisposePropertyController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: periodSummary = app.injector.instanceOf[views.html.periodSummary]
  val injectedViewInstancePast: periodSummaryPastReturns = app.injector.instanceOf[views.html.periodSummaryPastReturns]

  val periodKey: Int = 2015
  val organisationName: String = "OrganisationName"
  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789013"

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testPeriodSummaryController: PeriodSummaryController = new PeriodSummaryController (
      mockMcc,
      mockAuthAction,
      mockSummaryReturnsService,
      mockSubscriptionDataService,
      mockServiceInfoService,
      mockBackLinkCacheConnector,
      injectedViewInstance,
      injectedViewInstancePast
    )
    def createReturnWithAuthorisedUser()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.eq(Some(routes.PeriodSummaryController.view(periodKey).url)))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      val result = testPeriodSummaryController.createReturn(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def createReturnWithAuthorisedUserFromAccountSummary()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.eq(Some(routes.AccountSummaryController.view.url)))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testPeriodSummaryController.createReturn(periodKey, fromAccountSummary = true).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(periodSummaries: Option[PeriodSummaryReturns] = None)(test: Future[Result] => Any) {
      val period: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockSummaryReturnsService.getPeriodSummaryReturns(ArgumentMatchers.eq(period))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(periodSummaries))

      periodSummaries.foreach{periodSum =>
        when(mockSummaryReturnsService.filterPeriodSummaryReturnReliefs(periodSum, past = false))
      }

      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))

      val result = testPeriodSummaryController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUserPastReturns(periodSummaries: Option[PeriodSummaryReturns] = None)(test: Future[Result] => Any) {
      val period: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockSummaryReturnsService.getPeriodSummaryReturns(ArgumentMatchers.eq(period))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(periodSummaries))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))

      val result = testPeriodSummaryController.viewPastReturns(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPeriodSummaryController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewReturnWithAuthorisedUser()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testPeriodSummaryController.viewReturn(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewChargeableWithAuthorisedUser()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheConnector.clearBackLinks(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Nil))
      val result = testPeriodSummaryController.viewChargeable(periodKey, "1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewChargeableEditWithAuthorisedUser()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheConnector.clearBackLinks(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Nil))
      val result = testPeriodSummaryController.viewChargeableEdit(periodKey, "1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewDisposalWithAuthorisedUser()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testPeriodSummaryController.viewDisposal(periodKey, "1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockBackLinkCacheConnector)
  }

  "PeriodSummaryController" must {
    "PeriodSummary" must {

      "unauthorised users" must {
        "respond with a redirect and be redirected to the unauthorised page" in new Setup {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the period summary view" in new Setup {
          getWithAuthorisedUser(None) { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("Your ATED returns for")
            document.getElementsByClass("govuk-caption-xl").text === s"You have logged in as:$organisationName"
            document.getElementsByTag("h1").text() must include("Your ATED returns for")
          }
        }

        "show the period summary view post return" in new Setup {
          getWithAuthorisedUserPastReturns(None) { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("Your ATED returns for")
            document.getElementsByClass("govuk-caption-xl").text === s"You have logged in as:$organisationName"
            document.getElementsByTag("h1").text() must include("Your ATED returns for")
          }
        }

        "create a return must forward to the Return Type Page" in new Setup {

          createReturnWithAuthorisedUser() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/return-type/2015")
          }
        }

        "create a return must forward to the Return Type Page when return true from account summary" in new Setup {

          createReturnWithAuthorisedUserFromAccountSummary() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/return-type/2015")
          }
        }

        "view return" in new Setup {

          viewReturnWithAuthorisedUser() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/reliefs/2015/relief-summary")
          }
        }

        "view chargeable" in new Setup {

          viewChargeableWithAuthorisedUser() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/summary/1")
          }
        }

        "view chargeable in draft from submitted return" in new Setup {

          viewChargeableEditWithAuthorisedUser() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("ated/liability/1/change/summary")
          }
        }

        "view disposal" in new Setup {

          viewDisposalWithAuthorisedUser() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/1/dispose")
          }
        }
      }
    }
  }
}
