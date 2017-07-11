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

package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class HomeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]

  object TestHomeController extends HomeController {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "HomeController" must {
    "Home" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/home"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
      }

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
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithUnsubscribedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnsubscribedUser(userId, mockAuthConnector)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithUnsubscribedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnsubscribedAgent(userId, mockAuthConnector)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithAuthorisedUserFromBTA(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestHomeController.home(Some("bta")).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def homeWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

}
