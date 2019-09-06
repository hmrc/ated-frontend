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

import builders.SessionBuilder
import connectors.BackLinkCacheConnector
import models._
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{DelegationService, SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.MockAuthUtil

import scala.concurrent.Future

class PeriodSummaryControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  val mockSummaryReturnsService: SummaryReturnsService = mock[SummaryReturnsService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val periodKey: Int = 2015
  val organisationName: String = "OrganisationName"
  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789013"

  object TestPeriodSummaryController extends PeriodSummaryController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val summaryReturnsService: SummaryReturnsService = mockSummaryReturnsService
    override val subscriptionDataService: SubscriptionDataService = mockSubscriptionDataService
    override val delegationService: DelegationService = mockDelegationService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSummaryReturnsService)
    reset(mockBackLinkCache)
  }

  "PeriodSummaryController" must {

    "use correct DelegationConnector" in {
      PeriodSummaryController.delegationService must be(DelegationService)
    }

    "PeriodSummary" must {

      "unauthorised users" must {
        "respond with a redirect and be redirected to the unauthorised page" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the period summary view" in {
          getWithAuthorisedUser(None) { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("Your ATED returns for")
            document.getElementById("period-summary-header").text() must include("Your ATED returns for")
          }
        }

        "show the period summary view post return" in {
          getWithAuthorisedUserPastReturns(None) { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("Your ATED returns for")
            document.getElementById("period-summary-header").text() must include("Your ATED returns for")
          }
        }
        "create a return must forward to the Return Type Page" in {

          createReturnWithAuthorisedUser() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/return-type/2015")
          }
        }

        "view return" in {

          viewReturnWithAuthorisedUser() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/reliefs/2015/relief-summary")
          }
        }

        "view chargeable" in {

          viewChargeableWithAuthorisedUser() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/summary/1")
          }
        }

        "view chargeable in draft from submitted return" in {

          viewChargeableEditWithAuthorisedUser() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("ated/liability/1/change/summary")
          }
        }

        "view disposal" in {

          viewDisposalWithAuthorisedUser() { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/1/dispose")
          }
        }
      }

    }

  }

  def createReturnWithAuthorisedUser()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPeriodSummaryController.createReturn(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(periodSummaries: Option[PeriodSummaryReturns] = None)(test: Future[Result] => Any) {
    val period: Int = 2015
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockSummaryReturnsService.getPeriodSummaryReturns(Matchers.eq(period))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(periodSummaries))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = TestPeriodSummaryController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserPastReturns(periodSummaries: Option[PeriodSummaryReturns] = None)(test: Future[Result] => Any) {
    val period: Int = 2015
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockSummaryReturnsService.getPeriodSummaryReturns(Matchers.eq(period))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(periodSummaries))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = TestPeriodSummaryController.viewPastReturns(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestPeriodSummaryController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewReturnWithAuthorisedUser()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPeriodSummaryController.viewReturn(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewChargeableWithAuthorisedUser()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.clearBackLinks(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Nil))
    val result = TestPeriodSummaryController.viewChargeable(periodKey, "1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewChargeableEditWithAuthorisedUser()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.clearBackLinks(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Nil))
    val result = TestPeriodSummaryController.viewChargeableEdit(periodKey, "1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewDisposalWithAuthorisedUser()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPeriodSummaryController.viewDisposal(periodKey, "1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
