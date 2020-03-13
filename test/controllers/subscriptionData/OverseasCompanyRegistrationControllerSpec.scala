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

package controllers.subscriptionData

import java.util.UUID

import builders.{SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import controllers.auth.AuthAction
import testhelpers.MockAuthUtil
import models.Identification
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Environment
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SubscriptionDataService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class OverseasCompanyRegistrationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockEnvironment: Environment = app.injector.instanceOf[Environment]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testOverseasCompanyRegistrationController: OverseasCompanyRegistrationController = new OverseasCompanyRegistrationController (
      mockMcc,
      mockAuthAction,
      mockSubscriptionDataService,
      mockEnvironment
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testOverseasCompanyRegistrationController.edit().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(overseasInfo: Option[Identification] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockSubscriptionDataService.getOverseasCompanyRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(overseasInfo))
      val result = testOverseasCompanyRegistrationController.edit().apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testOverseasCompanyRegistrationController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUserSuccess(input: Option[Identification] = None)
                                       (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockSubscriptionDataService.updateOverseasCompanyRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(input))
      val result = testOverseasCompanyRegistrationController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

      test(result)

    }
  }

  "OverseasCompanyRegistrationController " must {
    "unauthorised users" must {

      "respond with a redirect" in new Setup {
        getWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }
    "edit" must {
      "show page with empty data" in new Setup {
        getWithAuthorisedUser(None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Edit your overseas company registration number"))
            document.getElementById("businessUniqueId").attr("value") must be("")
            document.getElementById("issuingInstitution").attr("value") must be("")
        }
      }

      "show page with populated data" in new Setup {
        val testIdentification = Identification("AAAAAAAAA", "Some Place", "FR")

        getWithAuthorisedUser(Some(testIdentification)) {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("businessUniqueId").attr("value") must be("AAAAAAAAA")
            document.getElementById("issuingInstitution").attr("value") must be("Some Place")
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

        "be redirected to the login page" in new Setup {
          submitWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {
        "validate form" must {

          "overseas company registration number must not be more than 60 chars and issuing institution must not be more than 40 chars" in new Setup {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val regNumber: String = "a" * 61
            val issuingInst: String = "a" * 41
            val inputJson: JsValue = Json.parse( s"""{"businessUniqueId": "$regNumber", "issuingInstitution": "$issuingInst", "countryCode": "FR" }""")

            submitWithAuthorisedUserSuccess()(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("The overseas company registration number cannot be more than 60 characters")
                contentAsString(result) must include("The institution that issued the overseas company registration number cannot be more than 40 characters")
            }
          }

          "If input is valid, submit must redirect" in new Setup {

            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJson: JsValue = Json.parse( s"""{"businessUniqueId": "AAAAAAAA", "issuingInstitution": "Some Place", "countryCode": "FR" }""")

            submitWithAuthorisedUserSuccess()(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include("/ated/company-details")
            }
          }
        }
      }
    }
  }
}
