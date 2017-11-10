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

package controllers.subscriptionData

import java.util.UUID

import builders.AuthBuilder.{createAtedContext, createDelegatedAuthContext}
import builders.{AuthBuilder, SessionBuilder, TitleBuilder}
import config.FrontendDelegationConnector
import models._
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import services.SubscriptionDataService
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class EditContactEmailControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val mockSubscriptionDataService = mock[SubscriptionDataService]


  object TestEditContactEmailController extends EditContactEmailController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val subscriptionDataService = mockSubscriptionDataService

  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSubscriptionDataService)
  }

  "EditContactEmailController " must {

    "use correct DelegationConnector ...." in {
      EditContactEmailController.delegationConnector must be(FrontendDelegationConnector)
    }

    "not respond with NOT_FOUND for the GET" in {
      val result = route(FakeRequest(GET, "/ated/edit-contact-email"))

      result.isDefined must be(true)
      status(result.get) must not be (NOT_FOUND)
    }

    "unauthorised users" must {

      "respond with a redirect" in {
        getWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }
    "edit" must {
      "return show the correct page if a client and we have empty data" in {
        getWithAuthorisedUser(None, true) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Edit your ATED email address"))
            document.getElementById("emailConsent-true").attr("checked") must be("")
            document.getElementById("emailConsent-false").attr("checked") must be("")
            document.getElementById("emailAddress").attr("value") must be("")
        }
      }

      "show email consent and address with populated data" in {

        val testContactEmail = EditContactDetailsEmail(emailConsent = true, emailAddress = "hrmc@hmrc.com")

        getWithAuthorisedUser(Some(testContactEmail), true) {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("emailAddress").attr("value") must be("hrmc@hmrc.com")
            document.getElementById("emailConsent-true").attr("checked") must be("checked")
            document.getElementById("emailConsent-false").attr("checked") must be("")
        }
      }


    }

    "submit" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(POST, "/ated/edit-contact-email"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
      }

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

        "validate form" must {


          "Email address must not be more than 241 characters if entered" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val emailTest = "a" * 240 + "@gmail.com"
            val inputJson = Json.parse( s"""{"emailAddress": "$emailTest", "emailConsent": true }""")

            submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Email address must not be more than 241 characters")
            }
          }

          "Email address must be a valid email address format" in {

            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJson = Json.parse( s"""{ "emailAddress": "abcdef@com", "emailConsent": true }""")

            submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Email address is not valid")
            }

          }

          "If edited contact address is valid, submit must redirect" in {

            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJson = Json.parse( s"""{ "emailAddress": "aa@mail.com", "emailConsent": true }""")
            val contactAddress: EditContactDetailsEmail = inputJson.as[EditContactDetailsEmail]

            submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include("/ated/company-details")
            }
          }
        }
      }
    }


  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestEditContactEmailController.edit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(contactDetailsEmail: Option[EditContactDetailsEmail] = None, emailConsent: Boolean)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockSubscriptionDataService.getEmailWithConsent(Matchers.any(), Matchers.any())).thenReturn(Future.successful(contactDetailsEmail))
    val result = TestEditContactEmailController.edit().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestEditContactEmailController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestEditContactEmailController.submit().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUserSuccess(testAddress: Option[EditContactDetailsEmail] = None)
                                     (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionDataService.editEmailWithConsent(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testAddress))
    val result = TestEditContactEmailController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)

  }
}
