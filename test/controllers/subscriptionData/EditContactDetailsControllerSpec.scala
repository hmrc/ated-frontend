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
import models.{Address, AddressDetails, ContactDetails, EditContactDetails}
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

class EditContactDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val mockSubscriptionDataService = mock[SubscriptionDataService]

  object TestEditAtedContactController extends EditContactDetailsController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val subscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSubscriptionDataService)
  }

  "EditContactDetailsController " must {

    "use correct DelegationConnector ...." in {
      EditContactDetailsController.delegationConnector must be(FrontendDelegationConnector)
    }

    "not respond with NOT_FOUND for the GET" in {
      val result = route(FakeRequest(GET, "/ated/edit-contact"))
      result.isDefined must be(true)
      status(result.get) must not be (NOT_FOUND)
    }

    "unauthorised users" must {

      "respond with a redirect" in {
        getWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page" in {
        getWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }

    "Authorised Users" must {

      "return business ATED contact details view with empty data" in {

        getWithAuthorisedUser(None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Edit your ATED contact details")

            document.getElementById("phoneNumber").attr("value") must be("")

        }
      }

      "show contact address with populated data" in {

        val testContactAddress = ContactDetails(Some("12333"), Some("2333333"), Some("2333334"))
        val testAddressDetails = AddressDetails("Correspondence", "line_1", "line_2", Some("line_3"), Some("line_4"), Some("postCode"), "GB")
        val testAddress = Address(Some("name1"), Some("name2"), addressDetails = testAddressDetails, contactDetails = Some(testContactAddress))

        getWithAuthorisedUser(Some(testAddress)) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            document.getElementById("phoneNumber").attr("value") must be("12333")
            document.getElementById("firstName").attr("value") must be("name1")
            document.getElementById("lastName").attr("value") must be("name2")
        }
      }

      "if contact details in not present in correspondence address, phonenum and emailAddress are empty" in {

        val testAddressDetails = AddressDetails("Correspondence", "line_1", "line_2", Some("line_3"), Some("line_4"), Some("postCode"), "GB")
        val testAddress = Address(Some("name1"), Some("name2"), addressDetails = testAddressDetails, contactDetails = None)

        getWithAuthorisedUser(Some(testAddress)) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            document.getElementById("phoneNumber").attr("value") must be("")
            document.getElementById("firstName").attr("value") must be("name1")
            document.getElementById("lastName").attr("value") must be("name2")
        }
      }

      "If contact address is not pre-populated, have empty fields on the page" in {

        getWithAuthorisedUser(None) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            document.getElementById("phoneNumber").attr("value") must be("")
            document.getElementById("firstName").attr("value") must be("")
            document.getElementById("lastName").attr("value") must be("")
        }

      }

      "submit" must {

        "not respond with NOT_FOUND" in {
          val result = route(FakeRequest(POST, "/ated/contact-address"))
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

            "First name is not valid when entered spaces" in {
              implicit val hc: HeaderCarrier = HeaderCarrier()
              val phoneNum = "a" * 25
              val inputJson = Json.parse( s"""{ "firstName": " ", "lastName": "TestLastName", "phoneNumber": "$phoneNum"}""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("You must enter a first name")
              }
            }

            "Telephone number must not be more than 24 characters" in {
              implicit val hc: HeaderCarrier = HeaderCarrier()
              val phoneNum = "a" * 25
              val inputJson = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "TestLastName", "phoneNumber": "$phoneNum"}""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Telephone number must not be more than 24 characters")
              }
            }

            "Telephone number must not have invalid characters" in {
              implicit val hc: HeaderCarrier = HeaderCarrier()
              val inputJson = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "TestLastName", "phoneNumber": "@@@@@@@@"}""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Telephone number is not valid")
              }
            }

            "Telephone number must not have lower case characters" in {
              implicit val hc: HeaderCarrier = HeaderCarrier()
              val inputJson = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "TestLastName", "phoneNumber": "0191222x123"}""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Telephone number is not valid")
              }
            }


            "If edited contact address is valid, submit must redirect" in {

              implicit val hc: HeaderCarrier = HeaderCarrier()
              val inputJson = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "TestLastName", "phoneNumber": "9999999999" }""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(SEE_OTHER)
                  redirectLocation(result).get must include("/ated/company-details")
              }
            }

            "If contact address is valid but the save fails, throw a validation error" in {

              implicit val hc: HeaderCarrier = HeaderCarrier()
              val inputJson = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "TestLastName", "phoneNumber": "9999999999" }""")

              submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Something has gone wrong, try again later.")
              }
            }
          }
        }
      }

    }

  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestEditAtedContactController.edit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestEditAtedContactController.edit().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getWithAuthorisedUser(companyDetails: Option[Address] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(companyDetails))
    val result = TestEditAtedContactController.edit().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestEditAtedContactController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestEditAtedContactController.submit().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUserSuccess(testAddress: Option[EditContactDetails] = None)
                                     (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionDataService.editContactDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testAddress))
    val result = TestEditAtedContactController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }

}
