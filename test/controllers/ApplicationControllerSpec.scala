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

package controllers

import java.util.UUID
import builders.SessionBuilder
import config.ApplicationConfig
import controllers.auth.AuthAction
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import views.html.error.individual
import views.html.unauthorised

import scala.concurrent.Future


class ApplicationControllerSpec extends PlaySpec with MockitoSugar with GuiceOneServerPerSuite with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val injectedUnauthorisedView: unauthorised = app.injector.instanceOf[views.html.unauthorised]
  val injectedIndividualView: individual = app.injector.instanceOf[views.html.error.individual]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testApplicationController: ApplicationController = new ApplicationController(
      mockMcc,
      mockAuthAction,
      injectedUnauthorisedView,
      injectedIndividualView
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setAuthMocks(authMock)
      val result = testApplicationController.unauthorised(false).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthorisedUserSa(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Individual, saEnrolmentSet)
      setAuthMocks(authMock)
      val result = testApplicationController.unauthorised(true).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def keepAliveWithAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testApplicationController.keepAlive().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

  }

  "ApplicationController" must {
    "unauthorised" must {

      "For user with SA account" should {
        "respond with an OK" in new Setup {
          getWithUnAuthorisedUserSa { result =>
            status(result) must be(OK)
          }
        }

        "load the unauthorised page" in new Setup {
          getWithUnAuthorisedUserSa { result =>
            contentAsString(result) must include("You have tried to sign in to your ATED account using your Self Assessment Government Gateway user ID.")
            contentAsString(result) must include("If you are an overseas landlord or client you need to sign out and then sign in using the Government Gateway user ID for your business to access your ATED account")
          }
        }
      }

      "For user without SA account" should {

        "respond with an OK" in new Setup {
          getWithUnAuthorisedUser { result =>
            status(result) must be(OK)
          }
        }

        "load the unauthorised page" in new Setup {
          getWithUnAuthorisedUser { result =>
            contentAsString(result) must include("You need to sign in with a different Gateway ID")
            contentAsString(result) must include("UK businesses, trusts and partnerships")
            contentAsString(result) must include("Non-UK businesses, trusts and partnerships, including non-resident landlords")
            contentAsString(result) must include("Do not set up a new Gateway ID if your business is already registered for ATED")
          }
        }
      }
    }

    "Cancel" must {

      "respond with a redirect" in new Setup {
        val result: Future[Result] = testApplicationController.cancel().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the login page" in new Setup {
        val result: Future[Result] = testApplicationController.cancel().apply(FakeRequest())
        redirectLocation(result).get must include("https://www.gov.uk/")
      }

      "Logout" must {

        "respond with a redirect" in new Setup {
          val result: Future[Result] = testApplicationController.logout().apply(FakeRequest())
          status(result) must be(SEE_OTHER)
        }

        "be redirected to the logout page" in new Setup {
          val result: Future[Result] = testApplicationController.logout().apply(FakeRequest())
          redirectLocation(result).get must include("/feedback/ATED")
        }
      }
    }

    "Keep Alive" must {

      "respond with an OK" in new Setup {
        keepAliveWithAuthorisedUser { result =>
          status(result) must be(OK)
        }
      }
    }

    "redirectToGuidance" must {
      "redirect the user" in new Setup {
        val userId = s"user-${UUID.randomUUID}"
        val authMock: Enrolments ~ Some[AffinityGroup] ~ Some[String] = authResultDefault(AffinityGroup.Individual, defaultEnrolmentSet)
        setAuthMocks(authMock)
        val result: Future[Result] = testApplicationController.redirectToGuidance().apply(SessionBuilder.buildRequestWithSession(userId))
        redirectLocation(result).get must include("/guidance/register-for-the-annual-tax-on-enveloped-dwellings-online-service")
      }
    }

    "redirectToSignIn" must {
      "redirect the user" in new Setup {
        val result: Future[Result] = testApplicationController.redirectToSignIn().apply(FakeRequest())
        redirectLocation(result).get must include ("http://localhost:9553/bas-gateway/sign-in?continue_url=http://localhost:9916/ated/home")
        status(result) mustBe SEE_OTHER
      }
    }
  }
}
