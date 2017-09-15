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

package controllers.reliefs

import java.util.UUID

import builders.{AuthBuilder, PropertyDetailsBuilder, ReliefBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{Reliefs, ReliefsTaxAvoidance, TaxAvoidance}
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
import services.{ReliefsService, SubscriptionDataService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.{AtedConstants, AtedUtils, PeriodUtils}

import scala.concurrent.Future

class ReliefsSummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockReliefsService = mock[ReliefsService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService = mock[SubscriptionDataService]
  val organisationName = "ACME Limited"

  object TestReliefsSummaryController extends ReliefsSummaryController {
    override val authConnector = mockAuthConnector
    val reliefsService = mockReliefsService
    override val delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
    override val subscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockReliefsService)
    reset(mockBackLinkCache)
    reset(mockSubscriptionDataService)
  }


  val periodKey = 2015

  "ReliefsSummaryController" must {

    "use correct DelegationConnector" in {
      ReliefsSummaryController.delegationConnector must be(FrontendDelegationConnector)
    }

    "view" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, s"/ated/reliefs/${periodKey}/relief-summary"))
        result.isDefined must be(true)
        status(result.get) must not be(NOT_FOUND)
      }

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "for authorised user" must {

        "status should be OK and submit should be enabled" in {
          getWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(true), equityRelease = true),
              taxAvoidance = TaxAvoidance(
                rentalBusinessScheme = Some("12345678"),
                openToPublicScheme = Some("12345678"),
                propertyDeveloperScheme = Some("12345678"),
                propertyTradingScheme = Some("12345678"),
                lendingScheme = Some("12345678"),
                employeeOccupationScheme = Some("12345678"),
                farmHousesScheme = Some("12345678"),
                socialHousingScheme = Some("12345678"),
                equityReleaseScheme = Some("12345678"), equityReleaseSchemePromoter = Some("12345678"))
            ))) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Check your details are correct")
              document.getElementById("property-details-summary-header").text() must be("Check your details are correct")
              document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              document.getElementById("tas-er").text() must be("12345678")
              document.getElementById("ated-charge-value").text() must be("£0")
              document.getElementById("submit").text() must be("Confirm and continue")
              document.getElementById("submit-disabled-text") must be (null)
          }
        }

        "status should be OK and submit should be enabled when tax avoidance 'No' has been chosen" in {
          getWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(false), equityRelease = true)
            ))) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Check your details are correct")
              document.getElementById("property-details-summary-header").text() must be("Check your details are correct")
              document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              document.getElementById("tas-er").text() must be("11111111")
              document.getElementById("ated-charge-value").text() must be("£0")
              document.getElementById("submit").text() must be("Confirm and continue")
              document.getElementById("submit-disabled-text") must be (null)
          }
        }

        "status should be OK and submit should be disabled when tax avoidance 'Yes' has been chosen and there is no schemes entered" in {
          getWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(true), equityRelease = true)
            ))) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Check your details are correct")
              document.getElementById("property-details-summary-header").text() must be("Check your details are correct")
              document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              document.getElementById("ated-charge-value").text() must be("Not yet calculated")
          }
        }

        "status should be OK and submit should be disabled when tax avoidance hasn't been selected" in {
          getWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = None, equityRelease = true)
            ))) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Check your details are correct")
              document.getElementById("property-details-summary-header").text() must be("Check your details are correct")
              document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              document.getElementById("ated-charge-value").text() must be("Not yet calculated")
          }
        }

        "redirect to bad request when the period key is in the future" in {

          getWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(true), openToPublic = true),
              taxAvoidance = TaxAvoidance(openToPublicScheme = Some("12345678"))
          )), PeriodUtils.calculatePeriod(new LocalDate().plusYears(2))) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("There is a problem with your ATED return")
              document.getElementById("relief-error-title").text() must be("There is a problem with your ATED return. No details have been saved so you must return to your account summary and start again.")
              document.getElementById("relief-error-ated-home-link").text() must be("ATED Account Summary")
          }
        }

      }
    }


    "continue" must {

      "redirect to declaration page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/reliefs/2015/relief-declaration"))
        }
      }

    }

    "print friendly view" must {

      "called for authorised user" must {

        "status should be OK" in {
          getPrintFriendlyWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(true), openToPublic = true),
              taxAvoidance = TaxAvoidance(openToPublicScheme = Some("12345678"))
            ))) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Check your details are correct")
              document.getElementById("property-details-summary-header").text() must be("Relief returns for ACME Limited")
              document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              document.getElementById("tas-otp-val").text() must be("12345678")
              document.getElementById("reliefs-print-charge-value").text() must be("£0")
          }
        }

      }

    }

    "delete the draft redirect to delete confirmation page" in {
      getWithDeleteDraftLink { result =>
        status(result) must be(SEE_OTHER)
      }
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
      val result = TestReliefsSummaryController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthenticated(test: Future[Result] => Any) {
      val result = TestReliefsSummaryController.view(periodKey).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def getWithAuthorisedUser(testReliefs: Option[ReliefsTaxAvoidance], periodKeyLocal: Int = periodKey)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReliefs))
      when(mockReliefsService.clearDraftReliefs(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, None)))
      when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
      val result = TestReliefsSummaryController.view(periodKeyLocal).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = TestReliefsSummaryController.continue(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def getPrintFriendlyWithAuthorisedUser(testReliefs: Option[ReliefsTaxAvoidance])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReliefs))
      when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
      val result = TestReliefsSummaryController.viewPrintFriendlyReliefReturn(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }


    def getWithDeleteDraftLink(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      val result = TestReliefsSummaryController.deleteDraft(2017).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

  }

}
