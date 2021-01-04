/*
 * Copyright 2021 HM Revenue & Customs
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

import builders.{SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.{ReliefReturnResponse, SubmitReturnsResponse}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate}
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
import services.{ReliefsService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants._
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class ReliefsSentControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.reliefs.reliefsSent]

  val periodKey = 2015
  val submittedDate: String = LocalDate.now().toString(DateTimeFormat.forPattern("d MMMM yyyy"))
  val organisationName = "ACME Limited"

  override def beforeEach(): Unit = {
  }

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testReliefsSentController: ReliefsSentController = new ReliefsSentController(
      mockMcc,
      mockAuthAction,
      mockServiceInfoService,
      mockDataCacheConnector,
      mockReliefsService,
      injectedViewInstance
    )

    def getWithAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockReliefsService.retrieveDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val reliefReturnResponse = ReliefReturnResponse(reliefDescription = "Farmhouses",formBundleNumber = "form-bundle-123")
      val submitReturnsResponse = SubmitReturnsResponse(processingDate = DateTime.now().toString, reliefReturnResponse = Some(Seq(reliefReturnResponse)), None)
      when(mockDataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](ArgumentMatchers.eq(SubmitReturnsResponseFormId))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(submitReturnsResponse)))
      val result = testReliefsSentController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def getPrintFriendlyWithAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockReliefsService.retrieveDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val reliefReturnResponse = ReliefReturnResponse(reliefDescription = "Farmhouses",formBundleNumber = "form-bundle-123")
      val submitReturnsResponse = SubmitReturnsResponse(processingDate = DateTime.now().toString, reliefReturnResponse = Some(Seq(reliefReturnResponse)), None)
      when(mockDataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](ArgumentMatchers.eq(SubmitReturnsResponseFormId))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(submitReturnsResponse)))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))
      val result = testReliefsSentController.viewPrintFriendlyReliefSent(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }


    def getWithAuthorisedUserNoReliefs(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockReliefsService.retrieveDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](ArgumentMatchers.eq(SubmitReturnsResponseFormId))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testReliefsSentController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      val result = testReliefsSentController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  "ReliefsSentController" must {

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

      "Authorised users" must {

        "return sent relief success view" in new Setup {
          getWithAuthorisedUser { result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Your returns have been successfully submitted - Annual Tax on enveloped dwellings"))
          }
        }

        "return print friendly sent relief success view" in new Setup {
          getPrintFriendlyWithAuthorisedUser { result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Your returns have been successfully submitted - Annual Tax on enveloped dwellings")
          }
        }

        "redirect to relief declaration, if no reliefs found" in new Setup {
          getWithAuthorisedUserNoReliefs {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(s"/ated/reliefs/$periodKey/relief-declaration"))
          }
        }
      }

    }
  }
}
