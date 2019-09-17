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

package controllers.auth

import models.StandardAuthRetrievals
import org.mockito.Matchers
import org.mockito.Mockito.{reset, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.redirectLocation
import services.DelegationService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite with TestUtil with DefaultAwaitTimeout {
  val mockAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]
  val mockDelegationService: DelegationService = mock[DelegationService]

  implicit val hc: HeaderCarrier = HeaderCarrier()


  object TestAuthAction extends AuthAction {
    override val delegationService: DelegationService = mockDelegationService
    override val authConnector: PlayAuthConnector = mockAuthConnector
  }

  override def beforeEach: Unit = {
    reset(mockDelegationService)
    reset(mockAuthConnector)
  }

  implicit val fq: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = mock[Messages]

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
      "affinity group is authorised with valid delegation" in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Organisation, atedOnlyEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = TestAuthAction.authorisedForNoEnrolments(func)
        status(res) shouldBe 200
      }

      "affinity group is authorised with no delegation returned" in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Agent, agentOnlyEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = TestAuthAction.authorisedForNoEnrolments(func)
        status(res) shouldBe 200
      }
    }

    "return 303 redirect response" when {
      "affinity group fails authorisation for reason UnsupportedAffinityGroup (AuthorisationException)" in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(UnsupportedAffinityGroup("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = TestAuthAction.authorisedForNoEnrolments(func)

        status(res) shouldBe 303
        redirectLocation(res) shouldBe Some("/ated/unauthorised")
      }

      "affinity group fails authorisation for reason InvalidBearerToken (NoActiveSession)" in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(InvalidBearerToken("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = TestAuthAction.authorisedForNoEnrolments(func)

        status(res) shouldBe 303
        redirectLocation(res) shouldBe Some("http://localhost:9025/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9916%2Fated%2Fhome&origin=ated-frontend")
      }

      "affinity group fails authorisation for reason InsufficientConfidenceLevel (AuthorisationException)" in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(InsufficientConfidenceLevel("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = TestAuthAction.authorisedForNoEnrolments(func)

        status(res) shouldBe 303
        redirectLocation(res) shouldBe Some("/ated/unauthorised")
      }
    }
  }

  "validateAgainstSaEnrolment" should {
    "return true" when {
      "the only enrolment is in IR-SA " in {
        val enrolments = Enrolments(saEnrolmentSet)
        TestAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe true
      }

      "the enrolments for in IR-CT and IR-SA " in {
        val enrolments = Enrolments(irSaCtEnrolmentSet)
        TestAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe true
      }
    }

    "return false" when {
      "the only enrolment is in Ated enrolment" in {
        val enrolments = Enrolments(atedOnlyEnrolmentSet)
        TestAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe false
      }

      "the only enrolment is in Agent enrolment" in {
        val enrolments = Enrolments(agentOnlyEnrolmentSet)
        TestAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe false
      }

      "the there are enrolments for IR-SA and Ated enrolment" in {
        val enrolments = Enrolments(irSaAtedEnrolmentSet)
        TestAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe false
      }

      "there are enrolments for Ated and Agents" in {
        val enrolments = Enrolments(atedAgentEnrolmentSet)
        TestAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe false
      }
    }
  }

  "Authorised Action" should {
    "redirect to unauthorised url" when {
      "enrolment is for an individual IR-SA"in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Individual, saEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = TestAuthAction.authorisedAction(func)
        status(res) shouldBe 303

      }

      "affinity group fails authorisation for reason InsufficientConfidenceLevel (AuthorisationException)"in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(InsufficientConfidenceLevel("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = TestAuthAction.authorisedAction(func)
        status(res) shouldBe 303

      }

      "affinity group fails authorisation for reason BearerTokenExpired (NoActiveSession)"in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(BearerTokenExpired("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = TestAuthAction.authorisedAction(func)
        status(res) shouldBe 303

      }

    }

    "return a valid 200 making a call to Delegation model" when {
      val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
      val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

      "affinity Organisation groups are authorised with valid delegation "in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Organisation, atedAgentEnrolmentSet)) )

        val res: Future[Result] = TestAuthAction.authorisedAction(func)
        status(res) shouldBe 200

      }

      "affinity Agent groups are authorised with valid delegation "in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Agent, atedAgentEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = TestAuthAction.authorisedAction(func)
        status(res) shouldBe 200

      }

      "affinity group is authorised with no delegation returned "in {

        when(mockDelegationService.delegationCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Organisation, atedOnlyEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = TestAuthAction.authorisedAction(func)
        status(res) shouldBe 200

      }
    }

  }

}
