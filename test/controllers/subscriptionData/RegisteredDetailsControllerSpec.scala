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

import builders.{AuthBuilder, SessionBuilder}
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
import play.api.test.Helpers._
import services.SubscriptionDataService
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class RegisteredDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockSubscriptionDataService = mock[SubscriptionDataService]

  object TestRegisteredDetailsController extends RegisteredDetailsController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val subscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSubscriptionDataService)
  }

  "RegisteredDetailsController" must {

    "editAddress" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/registered-details"))
        result.isDefined must be(true)
        status(result.get) must not be(NOT_FOUND)
      }

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
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

              document.title() must be("Edit business name and address")
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
          val businessPartnerDetails = RegisteredDetails(false, "testName",
            RegisteredAddressDetails(addressLine1 = "bpline1",
              addressLine2 = "bpline2",
              addressLine3 = Some("bpline3"),
              addressLine4 = Some("bpline4"),
              postalCode = Some("postCode"),
              countryCode = "GB"))

          getWithAuthorisedUser(Some(businessPartnerDetails)) {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be("Edit business name and address")
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
      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(POST, "/ated/registered-details/testName"))
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
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
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
            submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(
                  result) must be(BAD_REQUEST)
                contentAsString(result) must include("You must enter Business name")
                contentAsString(result) must include("You must enter Address line 1")
                contentAsString(result) must include("You must enter Address line 2")
                contentAsString(result) must include("You must enter Country")
            }
          }

          "Details enetered contains spaces" in {
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
            submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
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
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionDataService.getRegisteredDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(registeredDetails))

    val result = TestRegisteredDetailsController.edit().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestRegisteredDetailsController.edit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestRegisteredDetailsController.edit().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUserSuccess(updatedDetails: Option[RegisteredDetails]=None)
                                     (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionDataService.updateRegisteredDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(updatedDetails))
    val result = TestRegisteredDetailsController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestRegisteredDetailsController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestRegisteredDetailsController.submit().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }
}
