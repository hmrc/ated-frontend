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

package controllers.reliefs

import java.util.UUID
import builders._
import config.ApplicationConfig
import controllers.auth.AuthAction
import models.{Reliefs, ReliefsTaxAvoidance, TaxAvoidance}
import java.time.LocalDate
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
import services.{BackLinkCacheService, DataCacheService, ReliefsService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, PeriodUtils}
import views.html.BtaNavigationLinks
import views.html.reliefs.{invalidPeriodKey, reliefsSummary}

import scala.concurrent.Future

class ReliefsSummaryControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockBackLinkCacheService: BackLinkCacheService = mock[BackLinkCacheService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockReliefDeclarationController: ReliefDeclarationController = mock[ReliefDeclarationController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: reliefsSummary = app.injector.instanceOf[views.html.reliefs.reliefsSummary]
  val injectedViewInstancePeriod: invalidPeriodKey = app.injector.instanceOf[views.html.reliefs.invalidPeriodKey]

  val organisationName = "ACME Limited"
  val periodKey = 2015

  override def beforeEach(): Unit = {

  }

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testReliefsSummaryController: ReliefsSummaryController = new ReliefsSummaryController(
      mockMcc,
      mockAuthAction,
      mockReliefDeclarationController,
      mockSubscriptionDataService,
      mockServiceInfoService,
      mockReliefsService,
      mockDataCacheService,
      mockBackLinkCacheService,
      injectedViewInstance,
      injectedViewInstancePeriod
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testReliefsSummaryController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(testReliefs: Option[ReliefsTaxAvoidance], periodKeyLocal: Int = periodKey)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.retrieveDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(testReliefs))
      when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("http://backLink")))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))
      val result = testReliefsSummaryController.view(periodKeyLocal).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = testReliefsSummaryController.continue(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def getWithDeleteDraftLink(test: Future[Result] => Any): Unit = {
      val periodKey: Int = 2017
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)

      val result = testReliefsSummaryController.deleteDraft(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  "ReliefsSummaryController" must {

    "view" must {

      "unauthorised users" must {
        "respond with a redirect" in new Setup {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in new Setup {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "for authorised user" must {

        "status should be OK and submit should be enabled" in new Setup {
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
              document.title() must be(TitleBuilder.buildTitle("Check your details are correct"))
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
              document.getElementsByTag("h1").text() contains "Check your details are correct"
              document.getElementsByClass("govuk-caption-xl").text() must be ("This section is: Create return")
              document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              document.getElementById("tas-er").text() must be("12345678")
              document.getElementById("ated-charge-value").text() must be("£0")
              document.getElementById("submit").text() must be("Confirm and continue")
              document.getElementById("submit-disabled-text") must be (null)
              document.getElementsByClass("govuk-back-link").text must be("Back")
              document.getElementsByClass("govuk-back-link").attr("href") must include("http://backLink")
          }
        }

        "status should be OK and submit should be enabled when tax avoidance 'No' has been chosen" in new Setup {
          getWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(false), equityRelease = true)
            ))) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Check your details are correct"))
              document.getElementsByTag("h1").text() contains "Check your details are correct"
              document.getElementsByClass("govuk-caption-xl").text() must be ("This section is: Create return")
              document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              document.getElementById("tas-er").text() must be("11111111")
              document.getElementById("ated-charge-value").text() must be("£0")
              document.getElementById("submit").text() must be("Confirm and continue")
              document.getElementById("submit-disabled-text") must be (null)
          }
        }

        "status should be OK and submit should be disabled when tax avoidance 'Yes' has been chosen and there is no schemes entered" in new Setup {
          getWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(true), equityRelease = true)
            ))) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Check your details are correct"))
              document.getElementsByTag("h1").text() contains "Check your details are correct"
              document.getElementsByClass("govuk-caption-xl").text() must be ("This section is: Create return")
              document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              document.getElementById("ated-charge-value").text() must be("Not yet calculated")
          }
        }

        "status should be OK and submit should be disabled when tax avoidance hasn't been selected" in new Setup {
          getWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = None, equityRelease = true)
            ))) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Check your details are correct"))
              document.getElementsByTag("h1").text() contains "Check your details are correct"
              document.getElementsByClass("govuk-caption-xl").text() must be ("This section is: Create return")
              document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              document.getElementById("ated-charge-value").text() must be("Not yet calculated")
          }
        }

        "redirect to bad request when the period key is in the future" in new Setup {

          getWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(true), openToPublic = true),
              taxAvoidance = TaxAvoidance(openToPublicScheme = Some("12345678"))
          )), PeriodUtils.calculatePeakStartYear(LocalDate.now().plusYears(2))) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-back-link").attr("href") must include("#back")
              document.title() must be(TitleBuilder.buildTitle("There has been a problem"))
              document.getElementsByTag("h1").text() must be ("There has been a problem")
              document.getElementById("relief-error-body")
                .text() must be("No details have been saved. You need to go back to your ATED summary to create a new ATED return.")
              document.getElementById("relief-error-ated-home-link").text() must be("Back to your ATED summary")
          }
        }

      }
    }

    "continue" must {

      "redirect to declaration page" in new Setup {
        when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/reliefs/2015/relief-declaration"))
        }
      }
    }

    "delete the draft redirect to delete confirmation page" in new Setup {
      getWithDeleteDraftLink { result =>
        status(result) must be(SEE_OTHER)
      }
    }
  }
}
