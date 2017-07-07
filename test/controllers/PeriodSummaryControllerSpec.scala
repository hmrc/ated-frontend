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
import connectors.BackLinkCacheConnector
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
import services.{SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants._

import scala.concurrent.Future

class PeriodSummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockSummaryReturnsService = mock[SummaryReturnsService]
  val mockSubscriptionDataService = mock[SubscriptionDataService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]

  val organisationName = "OrganisationName"
  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789013"

  object TestPeriodSummaryController extends PeriodSummaryController {
    override val authConnector = mockAuthConnector
    override val summaryReturnsService = mockSummaryReturnsService
    override val subscriptionDataService = mockSubscriptionDataService
    override val delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSummaryReturnsService)
    reset(mockBackLinkCache)
  }

  "PeriodSummaryController" must {

    "use correct DelegationConnector" in {
      PeriodSummaryController.delegationConnector must be(FrontendDelegationConnector)
    }

    "PeriodSummary" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/period-summary/2015"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
      }

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
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPeriodSummaryController.createReturn(2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(periodSummaries: Option[PeriodSummaryReturns] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockSummaryReturnsService.getPeriodSummaryReturns(Matchers.eq(2015))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(periodSummaries))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = TestPeriodSummaryController.view(2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserPastReturns(periodSummaries: Option[PeriodSummaryReturns] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockSummaryReturnsService.getPeriodSummaryReturns(Matchers.eq(2015))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(periodSummaries))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = TestPeriodSummaryController.viewPastReturns(2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestPeriodSummaryController.view(2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestPeriodSummaryController.view(2015).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }


  def viewReturnWithAuthorisedUser()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPeriodSummaryController.viewReturn(2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewChargeableWithAuthorisedUser()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.clearBackLinks(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Nil))
    val result = TestPeriodSummaryController.viewChargeable(2015, "1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewDisposalWithAuthorisedUser()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPeriodSummaryController.viewDisposal(2015, "1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
