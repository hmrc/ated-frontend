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

import config.ApplicationConfig
import models.StandardAuthRetrievals
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
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

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  val mockAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]
  val mockDelegationService: DelegationService = mock[DelegationService]
  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

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

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))
        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Organisation, atedOnlyEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = testAuthAction.authorisedForNoEnrolments(func)
        status(res) shouldBe 200
      }

      "affinity group is authorised with no delegation returned"  in new Setup {

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Agent, agentOnlyEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = testAuthAction.authorisedForNoEnrolments(func)
        status(res) shouldBe 200
      }
    }

    "return 303 redirect response" when {
      "affinity group fails authorisation for reason UnsupportedAffinityGroup (AuthorisationException)"  in new Setup {

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))
        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(UnsupportedAffinityGroup("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture
        val res: Future[Result] = testAuthAction.authorisedForNoEnrolments(func)

        status(res) shouldBe 303
        redirectLocation(res) shouldBe Some("/ated/unauthorised")
      }

      "affinity group fails authorisation for reason InvalidBearerToken (NoActiveSession)"  in new Setup {

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))
        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(InvalidBearerToken("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture
        val res: Future[Result] = testAuthAction.authorisedForNoEnrolments(func)

        status(res) shouldBe 303
        redirectLocation(res) shouldBe Some("http://localhost:9025/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9916%2Fated%2Fhome&origin=ated-frontend")
      }

      "affinity group fails authorisation for reason InsufficientConfidenceLevel (AuthorisationException)"  in new Setup {

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))
        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(InsufficientConfidenceLevel("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture
        val res: Future[Result] = testAuthAction.authorisedForNoEnrolments(func)

        status(res) shouldBe 303
        redirectLocation(res) shouldBe Some("/ated/unauthorised")
      }
    }
  }

  "validateAgainstSaEnrolment" should {
    "return true" when {
      "the only enrolment is  in new Setup IR-SA "  in new Setup {
        val enrolments = Enrolments(saEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe true
      }

      "the enrolments for  in new Setup IR-CT and IR-SA "  in new Setup {
        val enrolments = Enrolments(irSaCtEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe true
      }
    }

    "return false" when {
      "the only enrolment is  in new Setup Ated enrolment"  in new Setup {
        val enrolments = Enrolments(atedOnlyEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe false
      }

      "the only enrolment is  in new Setup Agent enrolment"  in new Setup {
        val enrolments = Enrolments(agentOnlyEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe false
      }

      "the there are enrolments for IR-SA and Ated enrolment"  in new Setup {
        val enrolments = Enrolments(irSaAtedEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe false
      }

      "there are enrolments for Ated and Agents"  in new Setup {
        val enrolments = Enrolments(atedAgentEnrolmentSet)
        testAuthAction.validateAgainstSaEnrolment(enrolments) shouldBe false
      }
    }
  }

  "Authorised Action" should {
    "redirect to unauthorised url" when {
      "enrolment is for an individual IR-SA" in new Setup {

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Individual, saEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) shouldBe 303

      }

      "affinity group fails authorisation for reason InsufficientConfidenceLevel (AuthorisationException)" in new Setup {

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(InsufficientConfidenceLevel("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) shouldBe 303

      }

      "affinity group fails authorisation for reason BearerTokenExpired (NoActiveSession)" in new Setup {

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(BearerTokenExpired("error")))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) shouldBe 303

      }

    }

    "return a valid 200 making a call to Delegation model" when {
      val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
      val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

      "affinity Organisation groups are authorised with valid delegation " in new Setup {

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Organisation, atedAgentEnrolmentSet)) )

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) shouldBe 200

      }

      "affinity Agent groups are authorised with valid delegation " in new Setup {

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))

        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Agent, atedAgentEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) shouldBe 200

      }

      "affinity group is authorised with no delegation returned " in new Setup {

        when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

        when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(buildRetrieval(AffinityGroup.Organisation, atedOnlyEnrolmentSet)) )

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: StandardAuthRetrievals => Future[Result] = (_: StandardAuthRetrievals) => myFuture

        val res: Future[Result] = testAuthAction.authorisedAction(func)
        status(res) shouldBe 200

      }
    }

  }

}
