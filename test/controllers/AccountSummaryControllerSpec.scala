/*
 * Copyright 2024 HM Revenue & Customs
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

import builders.{SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.{AgentClientMandateFrontendConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models._
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
import play.api.test.Injecting
import play.twirl.api.Html
import services._
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.partials.HtmlPartial
import utils.TestModels
import views.html.{BtaNavigationLinks, accountSummary}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AccountSummaryControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with MockAuthUtil with TestModels with Injecting {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = inject[ExecutionContext]
  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockSummaryReturnsService: SummaryReturnsService = mock[SummaryReturnsService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockMandateFrontendConnector: AgentClientMandateFrontendConnector = mock[AgentClientMandateFrontendConnector]
  val mockDetailsService: DetailsService = mock[DetailsService]
  val mockDateService: DateService = mock[DateService]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: accountSummary = app.injector.instanceOf[views.html.accountSummary]

  when(mockDateService.now()).thenReturn(LocalDate.now())
  when(mockAppConfig.atedPeakStartDay).thenReturn("27")
  when(mockAppConfig.urBannerLink).thenReturn("https://test")


  val periodKey2015: Int = 2015

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testAccountSummaryController: AccountSummaryController = new AccountSummaryController(
      mockMcc,
      mockAuthAction,
      mockSummaryReturnsService,
      mockSubscriptionDataService,
      mockMandateFrontendConnector,
      mockDetailsService,
      mockDataCacheConnector,
      mockDateService,
      mockServiceInfoService,
      injectedViewInstance
    )

    def getWithAuthorisedUser(returnsSummaryWithDraft: SummaryReturnsModel,
                              correspondence: Option[Address] = None)(test: Future[Result] => Any): Unit = {
      val httpValue = 200
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)

      when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(httpValue, "")))
      when(mockSummaryReturnsService.getSummaryReturns(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnsSummaryWithDraft))
      when(mockSubscriptionDataService.getCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(correspondence))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(organisationName)))
      when(mockSubscriptionDataService.getSafeId(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("safeId")))
      when(mockDetailsService.cacheClientReference(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful("XN1200000100001"))
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Html("")))
      when(mockDetailsService.getClientMandateDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(clientMandateDetails)))
      val result = testAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithForbiddenUser(returnsSummaryWithDraft: SummaryReturnsModel,
                             correspondence: Option[Address] = None)(test: Future[Result] => Any): Unit = {
      val httpValue = 200
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setForbiddenAuthMocks(authMock)

      when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(httpValue, "")))
      when(mockSummaryReturnsService.getSummaryReturns(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnsSummaryWithDraft))
      when(mockSummaryReturnsService.generateCurrentTaxYearReturns(ArgumentMatchers.any())).thenReturn(Future.successful(Tuple3(Seq(), 0, false)))
      when(mockSubscriptionDataService.getCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(correspondence))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(organisationName)))
      when(mockSubscriptionDataService.getSafeId(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("safeId")))
      when(mockMandateFrontendConnector.getClientBannerPartial(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
      when(mockDetailsService.cacheClientReference(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful("XN1200000100001"))
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Html("")))

      val result = testAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedDelegatedUser(returnsSummaryWithDraft: SummaryReturnsModel,
                                       correspondence: Option[Address] = None)(test: Future[Result] => Any): Unit = {
      val httpValue = 200
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Agent, agentEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(httpValue, "")))
      when(mockSummaryReturnsService.getSummaryReturns(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnsSummaryWithDraft))
      when(mockSubscriptionDataService.getCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(correspondence))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(organisationName)))
      when(mockSubscriptionDataService.getSafeId(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("safeId")))
      when(mockMandateFrontendConnector.getClientBannerPartial(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
      when(mockDetailsService.cacheClientReference(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful("XN1200000100001"))
      val result = testAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSessionDelegation(userId))
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  ".view" when {

    "the user is unauthorised" must {
      "respond with a redirect" in new Setup {
        getWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "redirect to the unauthorised page" in new Setup {
        getWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }

    "the user has invalid enrolments" must {
      "redirect to unauthorised URL" in new Setup {
        val data: SummaryReturnsModel = SummaryReturnsModel(None, Seq(), Seq())
        getWithForbiddenUser(data, None) { result =>
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }

    "the user is authorised" must {

      "show the account summary view" in new Setup {

        when(mockMandateFrontendConnector.getClientBannerPartial(ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
        getWithAuthorisedUser(summaryReturnsModel(periodKey = periodKey2015), Some(address)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be(TitleBuilder.buildTitle("Annual Tax on Enveloped Dwellings (ATED) summary"))
            document.getElementsByTag("h1").text() contains "Annual Tax on Enveloped Dwellings (ATED) summary"
        }
      }

      "show the account summary view with UR banner" in new Setup {
        val data: SummaryReturnsModel = summaryReturnsModel(periodKey = periodKey2015)

        when(mockAppConfig.urBannerToggle).thenReturn(true)

        when(mockMandateFrontendConnector.getClientBannerPartial(ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
        getWithAuthorisedUser(data, Some(address)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.getElementsByClass("hmrc-user-research-banner__title")
              .text() must be("Help make GOV.UK better")
            document.getElementsByClass("hmrc-user-research-banner__link")
              .text() must be("Sign up to take part in research (opens in new tab)")
            document.getElementsByClass("hmrc-user-research-banner__close")
              .text() must include("Hide message. I do not want to take part in research")
        }
      }

      "show the create a return and appoint an agent link if there are no returns and no delegation" in new Setup {
        val data: SummaryReturnsModel = SummaryReturnsModel(None, Seq())
        when(mockMandateFrontendConnector.getClientBannerPartial(ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
        getWithAuthorisedUser(data, None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Annual Tax on Enveloped Dwellings (ATED) summary"))
            document.getElementById("create-return") must not be None
            document.getElementById("appoint-agent") must not be None
        }
      }

      "show the create a return button and no appoint an agent link if there are no returns and there is delegation" in new Setup {
        val data: SummaryReturnsModel = SummaryReturnsModel(None, Seq())
        getWithAuthorisedDelegatedUser(data, None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Annual Tax on Enveloped Dwellings (ATED) summary"))
            document.getElementById("create-return") must not be None
            Option(document.getElementById("appoint-agent")) must be(None)
        }
      }

      "show the agent info for authorised delegated user" in new Setup {
        val data: SummaryReturnsModel = SummaryReturnsModel(None, Seq())
        getWithAuthorisedDelegatedUser(data, None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.getElementById("agent-info-text")
              .getElementsContainingText("As an agent, you are accessing information about your client").size() must not be 0
            document.getElementById("agent-change-client").getElementsContainingText("Change client").size() must not be 0
            document.getElementById("agent-change-client").attr("href") === "http://localhost:9959/mandate/agent/summary"
        }
      }

      "throw exception for no safe id" in new Setup {
        val httpValue = 200
        val data: SummaryReturnsModel = SummaryReturnsModel(None, Seq())
        val userId = s"user-${UUID.randomUUID}"
        val authMock: Enrolments ~ Some[AffinityGroup] ~ Some[String] = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
        setAuthMocks(authMock)
        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(httpValue, "")))
        when(mockSummaryReturnsService.getSummaryReturns(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(data))
        when(mockSubscriptionDataService.getCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockDetailsService.cacheClientReference(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful("XN1200000100001"))
        when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(organisationName)))
        when(mockSubscriptionDataService.getSafeId(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockMandateFrontendConnector.getClientBannerPartial(ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HtmlPartial.Success(Some("thepartial"), Html(""))))
        when(mockDetailsService.getClientMandateDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(clientMandateDetails)))

        val result: Future[Result] = testAccountSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
        val thrown: RuntimeException = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must be("Could not get safeId")
      }

      "show the create a return button and no appoint an agent link if there are returns and delegation" in new Setup {

        val data: SummaryReturnsModel = summaryReturnsModel(periodKey = periodKey2015)

        getWithAuthorisedDelegatedUser(data, None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Annual Tax on Enveloped Dwellings (ATED) summary"))
            document.getElementById("create-return") must not be None
            Option(document.getElementById("appoint-agent")) must be(None)
        }
      }

      "with valid response from ACM" in new Setup {
        val cancelHtml: String =
        """<!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
         |<html>
         |
         |<head>
         |  <title>Response from ACM</title>
         |</head>
         |
         |<body>
         |  <a id="client-banner-text-link" href="/testCancelACMLinkUri/testAgentId">Cancel</a>
         |</body>
         |
         |</html>""".stripMargin
        when(mockMandateFrontendConnector.getClientBannerPartial
          (ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HtmlPartial.Success(Some("CancelLinkData"), Html(cancelHtml))))
        getWithAuthorisedUser(summaryReturnsModel(periodKey = periodKey2015), Some(address)) {
          result =>
            status(result) must be(OK)
        }
      }
    }
  }

  ".duringPeak" must {
    "return true if the current date is within the ated peak period" in new Setup {
      when(mockAppConfig.atedPeakStartDay).thenReturn("27")
      when(mockDateService.now()).thenReturn(LocalDate.parse("2020-03-27"))
      testAccountSummaryController.duringPeak must be(true)
    }

    "return false if the current date is before the start of the ated peak period" in new Setup {
      when(mockAppConfig.atedPeakStartDay).thenReturn("28")
      when(mockDateService.now()).thenReturn(LocalDate.parse("2020-03-27"))
      testAccountSummaryController.duringPeak must be(false)
    }

    "return false if the current date is after the end of the ated peak period" in new Setup {
      when(mockDateService.now()).thenReturn(LocalDate.parse("2020-05-01"))
      testAccountSummaryController.duringPeak must be(false)
    }
  }
}
