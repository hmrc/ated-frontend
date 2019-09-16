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

package controllers.subscriptionData

import java.util.UUID

import builders.{SessionBuilder, TitleBuilder}
import models.Identification
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DelegationService, SubscriptionDataService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.MockAuthUtil

import scala.concurrent.Future

class OverseasCompanyRegistrationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  object TestOverseasCompanyRegistrationController extends OverseasCompanyRegistrationController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService
    override val subscriptionDataService: SubscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSubscriptionDataService)
  }

  "OverseasCompanyRegistrationController " must {

    "use correct DelegationConnector ...." in {
      OverseasCompanyRegistrationController.delegationService must be(DelegationService)
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
      "show page with empty data" in {
        getWithAuthorisedUser(None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Edit your overseas company registration number"))
            document.getElementById("businessUniqueId").attr("value") must be("")
            document.getElementById("issuingInstitution").attr("value") must be("")
        }
      }

      "show page with populated data" in {

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


          "overseas company registration number must not be more than 60 chars and issuing institution must not be more than 40 chars" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val regNumber = "a" * 61
            val issuingInst = "a" * 41
            val inputJson = Json.parse( s"""{"businessUniqueId": "$regNumber", "issuingInstitution": "$issuingInst", "countryCode": "FR" }""")

            submitWithAuthorisedUserSuccess()(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("The overseas company registration number cannot be more than 60 characters")
                contentAsString(result) must include("The institution that issued the overseas company registration number cannot be more than 40 characters")
            }
          }

          "If input is valid, submit must redirect" in {

            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJson = Json.parse( s"""{"businessUniqueId": "AAAAAAAA", "issuingInstitution": "Some Place", "countryCode": "FR" }""")

            submitWithAuthorisedUserSuccess()(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include("/ated/company-details")
            }
          }
        }
      }
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = TestOverseasCompanyRegistrationController.edit().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(overseasInfo: Option[Identification] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockSubscriptionDataService.getOverseasCompanyRegistration(Matchers.any(), Matchers.any())).thenReturn(Future.successful(overseasInfo))
      val result = TestOverseasCompanyRegistrationController.edit().apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = TestOverseasCompanyRegistrationController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUserSuccess(input: Option[Identification] = None)
                                       (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockSubscriptionDataService.updateOverseasCompanyRegistration(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(input))
      val result = TestOverseasCompanyRegistrationController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

      test(result)

    }
  }
}
