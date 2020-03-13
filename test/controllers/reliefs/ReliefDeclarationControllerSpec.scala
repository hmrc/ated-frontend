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

package controllers.reliefs

import java.util.UUID

import builders.{SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import testhelpers.MockAuthUtil
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import services.ReliefsService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UserId}
import utils.AtedConstants

import scala.concurrent.Future

class ReliefDeclarationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]

  val periodKey = 2015

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testReliefDeclarationController: ReliefDeclarationController = new ReliefDeclarationController (
      mockMcc,
      mockAuthAction,
      mockReliefsService,
      mockDelegationService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector
    )

    def getWithAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      noDelegationModelAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testReliefDeclarationController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testReliefDeclarationController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockReliefsService.submitDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(userId)))))

      val result = testReliefDeclarationController.submit(periodKey)
        .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
      test(result)
    }
    def submitWithAuthorisedUserInvalidAgent(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockReliefsService.submitDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(Json.parse("""{"Reason":"Agent not Valid"}""")))))

      val result = testReliefDeclarationController.submit(periodKey)
        .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
      test(result)
    }

    def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testReliefDeclarationController.submit(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithForbiddenUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setForbiddenAuthMocks(authMock)
      val result = testReliefDeclarationController.submit(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedDelegatedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Agent, defaultEnrolmentSet)
      setAuthMocks(authMock)
      implicit val hc: HeaderCarrier = HeaderCarrier(userId = Some(UserId(userId)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testReliefDeclarationController.view(periodKey).apply(SessionBuilder.buildRequestWithSessionDelegation(userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {

reset(mockReliefsService)
    reset(mockDelegationService)
    reset(mockDataCacheConnector)
    reset(mockBackLinkCacheConnector)
  }

  "ReliefDeclarationController" must {

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

      "Authorised Users" must {

        "have a status of ok, for clients" in new Setup {
          getWithAuthorisedUser{
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Returns declaration"))
              document.getElementById("relief-declaration-before-declaration-text")
                .text() must be(
                "Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
              document.getElementById("relief-declaration-mid-declaration-text").text() must be("Each type of relief claimed is an individual ATED return.")
              document.getElementById("declare-or-confirm").text() must be("I declare that:")
              document.getElementById("declaration-confirmation-text")
                .text() must be("the information I have given on this return (or each of these returns) is correct")
              document.getElementById("submit").text() must be("Agree and submit returns")
          }
        }

        "have a status of ok, for agents" in new Setup {
          getWithAuthorisedDelegatedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Returns declaration"))
              document.getElementById("relief-declaration-before-declaration-text")
                .text() must be("Before your client’s return or returns can be submitted to HMRC, you must read and agree to the following statement. Your client’s approval may be in electronic or non-electronic form. If your client gives false information, they may have to pay financial penalties and face prosecution.")
              document.getElementById("relief-declaration-mid-declaration-text").text() must be("Each type of relief claimed is an individual ATED return.")
              document.getElementById("declare-or-confirm").text() must be("I confirm that my client has:")
              document.getElementById("declaration-confirmation-text")
                .text() must be("approved the information contained in this return (or each of these returns) as being correct")
              document.getElementById("submit").text() must be("Agree and submit returns")
          }
        }

        "contain a Confirm button" in new Setup {
          getWithAuthorisedUser {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("submit").text() must be("Agree and submit returns")
          }
        }
      }
    }

    "submit" must {

      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          submitWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "Forbidden users" must {
          "respond with a redirect to unauthorised URL" in new Setup {
            submitWithForbiddenUser { result =>
              redirectLocation(result).get must include("/ated/unauthorised")
            }
          }
        }

        "be redirected to the login page" in new Setup {
          submitWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "for valid data, redirect to sent relief page" in new Setup {
          submitWithAuthorisedUser {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include(s"/ated/reliefs/$periodKey/sent-reliefs")
          }
        }

        "for invalid data, return BAD_REQUEST" in new Setup {
          submitWithAuthorisedUserInvalidAgent {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.body().getElementById("header").text() must include("There was a problem when you set up this client")
          }
        }
      }
    }
  }
}
