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

package controllers.auth

import config.ApplicationConfig
import models.StandardAuthRetrievals
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.Helpers.redirectLocation
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import services.DelegationService
import testhelpers.TestUtil
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with TestUtil with DefaultAwaitTimeout {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = mock[MessagesApi].preferred(request)

  val mockAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]
  val mockDelegationService: DelegationService = mock[DelegationService]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  class Setup {
    val testAuthAction : AuthAction = new AuthAction(mockAppConfig, mockDelegationService, mockAuthConnector)
  }

  override def beforeEach: Unit = {
    reset(mockDelegationService)
    reset(mockAuthConnector)
  }

  type RetrievalType = Enrolments ~ Some[AffinityGroup] ~ Some[String]

  def buildRetrieval(affinityGroup: AffinityGroup, enrolments: Set[Enrolment]): Enrolments ~ Some[AffinityGroup] ~ Some[String] = {
    new ~(
      new ~(
        Enrolments(enrolments),
        Some(affinityGroup)
      ),
      Some("Id")
    )
  }

  "authorisedForNoEnrolments" should {
    "return a valid 200 response" when {
      "affinity group is authorised with valid delegation"  in new Setup {

        when(mockDelegationService.delegationCall(any())(any())).thenReturn(Future.successful(Some(delegationModel)))
        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Organisation, atedOnlyEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = testAuthAction.authorisedForNoEnrolments(func)
        status(res) mustBe 200
      }

      "affinity group is authorised with no delegation returned"  in new Setup {
        when(mockDelegationService.delegationCall(any())(any()))
          .thenReturn(Future.successful(None))
        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Agent, agentOnlyEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = testAuthAction.authorisedForNoEnrolments(func)
        status(res) mustBe 200
      }
    }

    "return 303 redirect response" when {
      "affinity group fails authorisation for reason UnsupportedAffinityGroup (AuthorisationException)"  in new Setup {
        when(mockDelegationService.delegationCall(any())(any()))
          .thenReturn(Future.successful(Some(delegationModel)))
        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.failed(UnsupportedAffinityGroup("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture
        val res: Future[Result] = testAuthAction.authorisedForNoEnrolments(func)

        status(res) mustBe 303
        redirectLocation(res).get must include("/unauthorised")
      }

      "affinity group fails authorisation for reason InvalidBearerToken (NoActiveSession)"  in new Setup {
        when(mockAppConfig.loginURL).thenReturn("http://localhost:9553/bas-gateway/sign-in")
        when(mockAppConfig.continueURL).thenReturn("http://localhost:9916/ated/home")

        when(mockDelegationService.delegationCall(any())(any()))
          .thenReturn(Future.successful(Some(delegationModel)))
        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.failed(InvalidBearerToken("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture
        val res: Future[Result] = testAuthAction.authorisedForNoEnrolments(func)

        status(res) mustBe 303
        redirectLocation(res) mustBe Some("http://localhost:9553/bas-gateway/sign-in?continue_url=http%3A%2F%2Flocalhost%3A9916%2Fated%2Fhome&origin=ated-frontend")
      }

      "affinity group fails authorisation for reason InsufficientConfidenceLevel (AuthorisationException)"  in new Setup {

        when(mockDelegationService.delegationCall(any())(any()))
          .thenReturn(Future.successful(Some(delegationModel)))
        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.failed(InsufficientConfidenceLevel("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture
        val res: Future[Result] = testAuthAction.authorisedForNoEnrolments(func)

        status(res) mustBe 303
        redirectLocation(res).get must include("/unauthorised")
      }
    }
  }

  "validateAgainstSaEnrolment" should {
    "return true" when {
      "the only enrolment is  in new Setup IR-SA "  in new Setup {
        val enrolments: Enrolments = Enrolments(saEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) mustBe true
      }

      "the enrolments for  in new Setup IR-CT and IR-SA "  in new Setup {
        val enrolments: Enrolments = Enrolments(irSaCtEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) mustBe true
      }
    }

    "return false" when {
      "the only enrolment is  in new Setup Ated enrolment"  in new Setup {
        val enrolments: Enrolments = Enrolments(atedOnlyEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) mustBe false
      }

      "the only enrolment is  in new Setup Agent enrolment"  in new Setup {
        val enrolments: Enrolments = Enrolments(agentOnlyEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) mustBe false
      }

      "the there are enrolments for IR-SA and Ated enrolment"  in new Setup {
        val enrolments: Enrolments = Enrolments(irSaAtedEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) mustBe false
      }

      "there are enrolments for Ated and Agents"  in new Setup {
        val enrolments: Enrolments = Enrolments(atedAgentEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) mustBe false
      }
    }
  }

  "Authorised Action" should {

    val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
    val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

    "redirect to unauthorised url" when {

      "enrolment is for an individual IR-SA" in new Setup {

        when(mockDelegationService.delegationCall(any())(any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Individual, saEnrolmentSet)) )

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) mustBe 303

      }

      "affinity group fails authorisation for reason InsufficientConfidenceLevel (AuthorisationException)" in new Setup {

        when(mockDelegationService.delegationCall(any())(any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.failed(InsufficientConfidenceLevel("error")))

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) mustBe 303

      }

      "affinity group fails authorisation for reason BearerTokenExpired (NoActiveSession)" in new Setup {
        when(mockAppConfig.loginURL).thenReturn("http://localhost:9553/sign-in")
        when(mockAppConfig.continueURL).thenReturn("http://localhost:9553/continue")

        when(mockDelegationService.delegationCall(any())(any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.failed(BearerTokenExpired("error")))

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) mustBe 303

      }

    }

    "return a valid 200 making a call to Delegation model" when {

      "affinity Organisation groups are authorised with valid delegation " in new Setup {
        when(mockDelegationService.delegationCall(any())(any()))
          .thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Organisation, atedAgentEnrolmentSet)) )

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) mustBe 200
      }

      "affinity Agent groups are authorised with valid delegation " in new Setup {
        when(mockDelegationService.delegationCall(any())(any()))
          .thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Agent, atedAgentEnrolmentSet)) )

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) mustBe 200
      }

      "affinity group is authorised with no delegation returned " in new Setup {
        when(mockDelegationService.delegationCall(any())(any()))
          .thenReturn(Future.successful(None))

        when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Organisation, atedOnlyEnrolmentSet)) )

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) mustBe 200
      }
    }

  }

}
