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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DelegationService
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.MockAuthUtil

import scala.concurrent.Future


class ApplicationControllerSpec extends PlaySpec with MockitoSugar with GuiceOneServerPerSuite with BeforeAndAfterEach with MockAuthUtil {

implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  object TestApplicationController extends ApplicationController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService

  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "ApplicationController" must {

    "unauthorised" must {

      "For user with SA account" should {
        "respond with an OK" in {
          getWithUnAuthorisedUserSa { result =>
            status(result) must be(OK)
          }
        }

        "load the unauthorised page" in {
          getWithUnAuthorisedUserSa { result =>
            contentAsString(result) must include("You are trying to sign in with your Self Assessment ID. " +
              "If you are an overseas landlord or client you need to use your limited company ID")
          }
        }
      }

      "For user without SA account" should {

        "respond with an OK" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(OK)
          }
        }

        "load the unauthorised page" in {
          getWithUnAuthorisedUser { result =>
            contentAsString(result) must include("You are not authorised to use this service")
          }
        }
      }
    }

    "Cancel" must {

      "respond with a redirect" in {
        val result = controllers.ApplicationController.cancel().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the login page" in {
        val result = controllers.ApplicationController.cancel().apply(FakeRequest())
        redirectLocation(result).get must include("https://www.gov.uk/")
      }


      "Logout" must {

        "respond with a redirect" in {
          val result = controllers.ApplicationController.logout().apply(FakeRequest())
          status(result) must be(SEE_OTHER)
        }

        "be redirected to the logout page" in {
          val result = controllers.ApplicationController.logout().apply(FakeRequest())
          redirectLocation(result).get must include("/feedback/ATED")
        }
      }
    }

    "Keep Alive" must {

      "respond with an OK" in {
        keepAliveWithAuthorisedUser { result =>
          status(result) must be(OK)
        }
      }
    }
  }


  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestApplicationController.unauthorised(false).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUserSa(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Individual, saEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestApplicationController.unauthorised(true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def keepAliveWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestApplicationController.keepAlive().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
