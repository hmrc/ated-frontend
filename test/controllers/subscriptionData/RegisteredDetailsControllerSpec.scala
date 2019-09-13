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
import connectors.DataCacheConnector
import models._
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

class RegisteredDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()


  object TestRegisteredDetailsController extends RegisteredDetailsController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService
    override val subscriptionDataService: SubscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSubscriptionDataService)
  }

  "RegisteredDetailsController" should {
    "use correct delegationService" in {
      RegisteredDetailsController.delegationService mustBe DelegationService
    }
    "use correct controllerId" in {
      RegisteredDetailsController.subscriptionDataService mustBe SubscriptionDataService
    }
    "use correct dataCacheConnector" in {
      RegisteredDetailsController.dataCacheConnector mustBe DataCacheConnector
    }

  }

  "RegisteredDetailsController" must {

    "editAddress" must {

      "unauthorised users" must {
        "respond with a redirect" in {
            status(getWithUnAuthorisedUser) must be(SEE_OTHER)
        }

        "be redirected to the login page" in {
            redirectLocation(getWithUnAuthorisedUser).get must include("/ated/unauthorised")

        }
      }

      "Authorised users" must {

        "respond with OK" in {
          getWithAuthorisedUser() {
            result =>
              status(result) must be(OK)
          }
        }

        "show the Registered details view with empty data" in {
          getWithAuthorisedUser(None) {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be (TitleBuilder.buildTitle("Edit business name and address"))
              document.getElementById("registered-details-header").text() must be("Edit business name and address")
              document.getElementById("registered-address-line-1").attr("value") must be("")
              document.getElementById("registered-address-line-2").attr("value") must be("")
              document.getElementById("registered-address-line-3").attr("value") must be("")
              document.getElementById("registered-address-line-4").attr("value") must be("")
              document.getElementById("addressDetails-postalCode").attr("value") must be("")
              document.getElementById("registered-country_field").text() must include("Country")
              document.getElementById("submit").text() must be("Save")
          }
        }

        "show the Registered details view with populated data" in {
          val businessPartnerDetails = RegisteredDetails(isEditable = false, "testName",
            RegisteredAddressDetails(addressLine1 = "bpline1",
              addressLine2 = "bpline2",
              addressLine3 = Some("bpline3"),
              addressLine4 = Some("bpline4"),
              postalCode = Some("postCode"),
              countryCode = "GB"))

          getWithAuthorisedUser(Some(businessPartnerDetails)) {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be (TitleBuilder.buildTitle("Edit business name and address"))
              document.getElementById("registered-details-header").text() must be("Edit business name and address")
              document.getElementById("registered-address-line-1").attr("value") must be("bpline1")
              document.getElementById("registered-address-line-2").attr("value") must be("bpline2")
              document.getElementById("registered-address-line-3").attr("value") must be("bpline3")
              document.getElementById("registered-address-line-4").attr("value") must be("bpline4")
              document.getElementById("addressDetails-postalCode").attr("value") must be("postCode")
              document.getElementById("registered-country_field").text() must include("Country")
              document.getElementById("submit").text() must be("Save")
          }
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

        "be redirected to the unauthorised page" in {
            redirectLocation(getWithUnAuthorisedUser).get must include("/ated/unauthorised")
        }
      }

      "Authorised users" must {

        "validate form" must {

          "If registration details entered are valid, save and continue button must redirect to contact details page if the save worked" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJson = Json.parse(
              """{ "isEditable": true,
                | "name": "sdfsdf",
                |"addressDetails" : {
                |"addressLine1": "sdfsdf",
                |"addressLine2": "sdfsdf",
                |"addressLine3": "sdfsdf",
                |"addressLine4": "sdfsdf",
                |"countryCode": "AR"}}""".stripMargin)
            val registeredDetails: RegisteredDetails = inputJson.as[RegisteredDetails]
            submitWithAuthorisedUserSuccess(Some(registeredDetails))(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include("/ated/company-details")
            }
          }

          "If registration details entered are valid but the save fails, throw a validation error" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJson = Json.parse(
              """{ "isEditable": true,
              | "name": "sdfsdf",
              |"addressDetails" : {
              |"addressLine1": "sdfsdf",
              |"addressLine2": "sdfsdf",
              |"addressLine3": "sdfsdf",
              |"addressLine4": "sdfsdf",
              |"countryCode": "AR"}}""".stripMargin)

            submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Something has gone wrong, try again later.")
            }
          }

          "not be empty" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJson = Json.parse(
              """{ "isEditable": true,
                | "name": "",
                |"addressDetails" : {
                |"addressLine1": "",
                |"addressLine2": "",
                |"addressLine3": "",
                |"addressLine4": "",
                |"postalCode": "",
                |"countryCode": ""}}""".stripMargin)
            val registeredDetails: RegisteredDetails = inputJson.as[RegisteredDetails]
            submitWithAuthorisedUserSuccess(Some(registeredDetails))(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(
                  result) must be(BAD_REQUEST)
                contentAsString(result) must include("You must enter Business name")
                contentAsString(result) must include("You must enter Address line 1")
                contentAsString(result) must include("You must enter Address line 2")
                contentAsString(result) must include("You must enter Country")
            }
          }

          "Details entered contains spaces" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJson = Json.parse(
              """{ "isEditable": true,
                | "name": " ",
                |"addressDetails" : {
                |"addressLine1": " ",
                |"addressLine2": " ",
                |"addressLine3": "",
                |"addressLine4": "",
                |"postalCode": "",
                |"countryCode": ""}}""".stripMargin)
            val registeredDetails: RegisteredDetails = inputJson.as[RegisteredDetails]
            submitWithAuthorisedUserSuccess(Some(registeredDetails))(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(
                  result) must be(BAD_REQUEST)
                contentAsString(result) must include("You must enter Business name")
                contentAsString(result) must include("You must enter Address line 1")
                contentAsString(result) must include("You must enter Address line 2")
                contentAsString(result) must include("You must enter Country")
            }
          }

          "If entered, data must not be too long" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            val inputJson = Json.parse(
              """{ "isEditable": true,
                |"name": "testName",
                |"addressDetails" : {
                |"addressLine1": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                |"addressLine2": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                |"addressLine3": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                |"addressLine4": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                |"postalCode": "12345678910",
                |"countryCode": ""}}""".stripMargin)
            val
            registeredDetails: RegisteredDetails = inputJson.as[RegisteredDetails]
            submitWithAuthorisedUserSuccess(Some(registeredDetails))(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Address line 1 cannot be more than 35 characters")
                contentAsString(result) must include("Address line 2 cannot be more than 35 characters")
                contentAsString(result) must include("Address line 3 cannot be more than 35 characters")
                contentAsString(result) must include("Address line 4 cannot be more than 35 characters")
                contentAsString(result) must include("You must enter a valid postcode")
                contentAsString(result) must include("You must enter Country")

            }
          }
        }
      }
    }
  }

  def getWithAuthorisedUser(registeredDetails: Option[RegisteredDetails]=None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockSubscriptionDataService.getRegisteredDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(registeredDetails))

    val result = TestRegisteredDetailsController.edit().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser: Future[Result] =  {
    val userId = s"user-${UUID.randomUUID}"
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    TestRegisteredDetailsController.edit()(SessionBuilder.buildRequestWithSession(userId))
  }

  def submitWithAuthorisedUserSuccess(updatedDetails: Option[RegisteredDetails]=None)
                                     (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockSubscriptionDataService.updateRegisteredDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(updatedDetails))
    val result = TestRegisteredDetailsController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestRegisteredDetailsController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
