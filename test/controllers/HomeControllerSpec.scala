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

package controllers

import java.util.UUID

import builders.SessionBuilder
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import services.DelegationService
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.MockAuthUtil

import scala.concurrent.Future

class HomeControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

object TestHomeController extends HomeController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDelegationService)
  }

  "HomeController" should {
    "use correct delegationService" in {
      HomeController.delegationService mustBe DelegationService
    }
  }

  "HomeController" must {
    "Home" must {

      "unauthorised users" must {
        "be redirected to the unauthorised page" in {
          homeWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "unsubscribed" must {
        "users be redirected to the subscription service" in {
          homeWithUnsubscribedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/start")
          }
        }

        "agents be redirected to the subscription service" in {
          homeWithUnsubscribedAgent { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/start")
          }
        }
      }

      "Authenticated users" must {

        "be redirected to the Account Summary page " in {
          homeWithAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/account-summary")
          }
        }

        "be redirected to the Account Summary page with bta in session" in {
          homeWithAuthorisedUserFromBTA { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/account-summary")
            session(result).data("callerId") must be("bta")
          }
        }
      }

      "Authenticated agents" must {

        "be redirected to the Agent Client Summary page " in {
          homeWithAuthorisedAgent { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/mandate/agent/service")
          }
        }
      }
    }

  }


  def homeWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Agent, agentEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithUnsubscribedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithUnsubscribedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithAuthorisedUserFromBTA(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestHomeController.home(Some("bta")).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
