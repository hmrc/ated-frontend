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

import builders.{AuthBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{AtedContext, SubmitReturnsResponse}
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import services.ReliefsService
import uk.gov.hmrc.play.frontend.auth.DummyDelegationData
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, UserId }

class ReliefDeclarationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockReliefsService = mock[ReliefsService]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  implicit def atedContext2AuthContext(implicit atedContext: AtedContext) = atedContext.user.authContext

  object TestReliefDeclarationController extends ReliefDeclarationController {
    override val authConnector = mockAuthConnector
    val reliefsService = mockReliefsService
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockReliefsService)
    reset(mockDelegationConnector)
    reset(mockBackLinkCache)
  }


  val periodKey = 2015

  "ReliefDeclarationController" must {

    "use correct DelegationConnector" in {
      ReliefDeclarationController.delegationConnector must be(FrontendDelegationConnector)
    }

    "view" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, s"/ated/reliefs/$periodKey/relief-summary"))
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

      "Authorised Users" must {

        "have a status of ok, for clients" in {
          getWithAuthorisedUser{
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Returns declaration")
              document.getElementById("relief-declaration-before-declaration-text").text() must be("Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
              document.getElementById("relief-declaration-mid-declaration-text").text() must be("Each type of relief claimed is an individual ATED return.")
              document.getElementById("declare-or-confirm").text() must be("I declare that:")
              document.getElementById("declaration-confirmation-text").text() must be("the information I have given on this return (or each of these returns) is correct")
              document.getElementById("submit").text() must be("Agree and submit returns")
          }
        }

        "have a status of ok, for agents" in {
          getWithAuthorisedDelegatedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Returns declaration")
              document.getElementById("relief-declaration-before-declaration-text").text() must be("Before your client’s return or returns can be submitted to HMRC, you must read and agree to the following statement. Your client’s approval may be in electronic or non-electronic form. If your client gives false information, they may have to pay financial penalties and face prosecution.")
              document.getElementById("relief-declaration-mid-declaration-text").text() must be("Each type of relief claimed is an individual ATED return.")
              document.getElementById("declare-or-confirm").text() must be("I confirm that my client has:")
              document.getElementById("declaration-confirmation-text").text() must be("approved the information contained in this return (or each of these returns) as being correct")
              document.getElementById("submit").text() must be("Agree and submit returns")
          }
        }

        "contain a Confirm button" in {
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

        "respond with a redirect" in {
          submitWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          submitWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "for valid data, redirect to sent relief page" in {
          submitWithAuthorisedUser {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include(s"/ated/reliefs/${periodKey}/sent-reliefs")
          }
        }

        "for invalid data, return BAD_REQUEST" in {
          submitWithAuthorisedUserInvlidAgent {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("There was a problem when you set up this client")
          }

        }
      }
    }
  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestReliefDeclarationController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }


  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestReliefDeclarationController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestReliefDeclarationController.view(periodKey).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.submitDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(userId)))))

    val result = TestReliefDeclarationController.submit(periodKey).apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
    test(result)
  }
  def submitWithAuthorisedUserInvlidAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.submitDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(Json.parse("""{"Reason":"Agent not Valid"}""")))))

    val result = TestReliefDeclarationController.submit(periodKey).apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
    test(result)
  }

  def submitWithAuthorisedOtherUsers(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.submitDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))

    val result = TestReliefDeclarationController.submit(periodKey).apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestReliefDeclarationController.submit(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestReliefDeclarationController.submit(periodKey).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getWithAuthorisedDelegatedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createDelegatedAuthContext(userId, "company name|display name"))
    implicit val hc: HeaderCarrier = HeaderCarrier(userId = Some(UserId(userId)))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockDelegationConnector.getDelegationData(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(DummyDelegationData.returnData)))
    val result = TestReliefDeclarationController.view(periodKey).apply(SessionBuilder.buildRequestWithSessionDelegation(userId))
    test(result)
  }

}
