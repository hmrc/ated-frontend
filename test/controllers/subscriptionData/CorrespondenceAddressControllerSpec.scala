/*
 * Copyright 2018 HM Revenue & Customs
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
import services.SubscriptionDataService
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class CorrespondenceAddressControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {


  val mockAuthConnector = mock[AuthConnector]
  val mockSubscriptionDataService = mock[SubscriptionDataService]
  val mockDelegationConnector = mock[DelegationConnector]

  object TestCorrespondenceAddressController extends CorrespondenceAddressController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val subscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSubscriptionDataService)
  }

  "CorrespondenceAddressController" must {

    "editAddress" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/correspondence-address"))
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

        "show the correspondence address view with empty data" in {
          getWithAuthorisedUser(None) {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be (TitleBuilder.buildTitle("Edit your correspondence address"))
              document.getElementById("correspondence-address-header").text() must include("Edit your correspondence address")
              document.getElementById("addressLine1").attr("value") must be("")
              document.getElementById("addressLine2").attr("value") must be("")
              document.getElementById("addressLine3").attr("value") must be("")
              document.getElementById("addressLine4").attr("value") must be("")
              document.getElementById("postalCode").attr("value") must be("")
              document.getElementById("countryCode_field").text() must include("Country")
              document.getElementById("addressType").attr("value") must be("")
              document.getElementById("submit").text() must be("Save changes")
          }
        }


        "show the correspondence address view with populated space in data" in {
          val testAddressDetails = AddressDetails("Correspondence", "  ", "  ", Some("line_3"), Some("line_4"), Some("postCode"), "GB")
          val testAddress = Address(Some("name1"), Some("name2"), addressDetails = testAddressDetails, contactDetails = None)

          getWithAuthorisedUser(Some(testAddress)) {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be (TitleBuilder.buildTitle("Edit your correspondence address"))
              document.getElementById("correspondence-address-header").text() must include("Edit your correspondence address")
              document.getElementById("addressType").attr("value") must be("Correspondence")
              document.getElementById("addressLine1").attr("value") must be("  ")
              document.getElementById("addressLine2").attr("value") must be("  ")
              document.getElementById("addressLine3").attr("value") must be("line_3")
              document.getElementById("addressLine4").attr("value") must be("line_4")
              document.getElementById("postalCode").attr("value") must be("postCode")
              document.getElementById("submit").text() must be("Save changes")
              document.getElementById("countryCode_field").text() must include("Country")
          }
        }

        "show the correspondence address view with populated data" in {
          val testAddressDetails = AddressDetails("Correspondence", "line_1", "line_2", Some("line_3"), Some("line_4"), Some("postCode"), "GB")
          val testAddress = Address(Some("name1"), Some("name2"), addressDetails = testAddressDetails, contactDetails = None)

          getWithAuthorisedUser(Some(testAddress)) {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be (TitleBuilder.buildTitle("Edit your correspondence address"))
              document.getElementById("correspondence-address-header").text() must include("Edit your correspondence address")
              document.getElementById("addressType").attr("value") must be("Correspondence")
              document.getElementById("addressLine1").attr("value") must be("line_1")
              document.getElementById("addressLine2").attr("value") must be("line_2")
              document.getElementById("addressLine3").attr("value") must be("line_3")
              document.getElementById("addressLine4").attr("value") must be("line_4")
              document.getElementById("postalCode").attr("value") must be("postCode")
              document.getElementById("submit").text() must be("Save changes")
              document.getElementById("countryCode_field").text() must include("Country")
          }
        }

        "submit" must {

          "not respond with NOT_FOUND" in {
            val result = route(FakeRequest(POST, "/ated/correspondence-address"))
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
                val inputJson = Json.parse( """{ "addressType": "sadsdf", "addressLine1": "sdfsdf", "addressLine2": "asd", "addressLine3": "asd","addressLine4": "asd", "postalCode": "XX1 1XX", "countryCode": "GB"}""")
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(SEE_OTHER)
                    redirectLocation(result).get must include("/ated/company-details")
                }
              }

              "If registration details entered are valid but the save fails, throw a validation error" in {
                implicit val hc: HeaderCarrier = HeaderCarrier()
                val inputJson = Json.parse( """{ "addressType": "sadsdf", "addressLine1": "sdfsdf", "addressLine2": "asd", "addressLine3": "asd","addressLine4": "asd", "postalCode": "XX11XX", "countryCode": "GB"}""")

                submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                }
              }


              "If address line 1 in correspondence details entered is with spaces  but the save fails, throw a validation error" in {
                implicit val hc: HeaderCarrier = HeaderCarrier()
                val inputJson = Json.parse( """{ "addressType": "sadsdf", "addressLine1": " ", "addressLine2": " ", "addressLine3": "asd","addressLine4": "asd", "postalCode": "XX11XX", "countryCode": "GB"}""")

                submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                }
              }

              "not be empty" in {
                implicit val hc: HeaderCarrier = HeaderCarrier()
                val inputJson = Json.parse( """{ "addressType": "", "addressLine1": "", "addressLine2": "", "addressLine3": "", "addressLine4": "", "postalCode": "", "countryCode": ""}""")

                submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                      contentAsString(result) must include("You must enter Address line 1")
                      contentAsString(result) must include("You must enter Address line 2")
                      contentAsString(result) must include("You must enter Country")
                }
              }

            "If entered, Address line 1 must be maximum of 35 characters" in {
              implicit val hc: HeaderCarrier = HeaderCarrier()
              val inputJson = Json.parse( """{ "addressType": "", "addressLine1": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD", "addressLine2": "", "addressLine3": "", "addressLine4": "", "postalCode": "", "countryCode": ""}""")
              val addressDetails: AddressDetails = inputJson.as[AddressDetails]
              submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Address line 1 cannot be more than 35 characters")
              }
            }


            "If entered, Address line 2 must be maximum of 35 characters" in {
              implicit val hc: HeaderCarrier = HeaderCarrier()
              val inputJson = Json.parse( """{ "addressType": "", "addressLine1": "", "addressLine2": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD", "addressLine3": "", "addressLine4": "", "postalCode": "", "countryCode": ""}""")
              val addressDetails: AddressDetails = inputJson.as[AddressDetails]
              submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Address line 2 cannot be more than 35 characters")
              }
            }
              "If entered, Address line 3 must be maximum of 35 characters" in {
                implicit val hc: HeaderCarrier = HeaderCarrier()
                val inputJson = Json.parse( """{ "addressType": "", "addressLine1": "", "addressLine2": "", "addressLine3": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD", "addressLine4": "", "postalCode": "", "countryCode": ""}""")
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Address line 3 cannot be more than 35 characters")
                }
              }
              "If entered, Address line 4 must be maximum of 35 characters" in {
                implicit val hc: HeaderCarrier = HeaderCarrier()
                val inputJson = Json.parse( """{ "addressType": "", "addressLine1": "", "addressLine2": "", "addressLine3": "", "addressLine4": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDWWWWWWWWWWWWWWW", "postalCode": "", "countryCode": ""}""")
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Address line 4 cannot be more than 35 characters")
                }
              }

                "Postcode must not be more than 10 characters" in {
                implicit val hc: HeaderCarrier = HeaderCarrier()
                val inputJson = Json.parse( """{ "addressType": "", "addressLine1": "", "addressLine2": "", "addressLine3": "", "addressLine4": "", "postalCode": "asssaa34aaaaaa", "countryCode": ""}""")
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("You must enter a valid postcode")
                }
              }

               "Country Code must be selected" in {
                implicit val hc: HeaderCarrier = HeaderCarrier()
                val inputJson = Json.parse(  """{ "addressType": "", "addressLine1": "", "addressLine2": "", "addressLine3": "", "addressLine4": "", "postalCode": "", "countryCode": ""}""")
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("You must enter Country")
                }
               }
              "throw a validation error, if save fails" in {
                implicit val hc: HeaderCarrier = HeaderCarrier()
                val inputJson = Json.parse( """{ "addressType": "Correspondence", "addressLine1": "sdfsdf", "addressLine2": "asd", "addressLine3": "asd","addressLine4": "asd", "postalCode": "XX1 1XX", "countryCode": "GB"}""")
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess()(FakeRequest().withJsonBody(inputJson)) {
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
    }

  def getWithAuthorisedUser(companyDetails: Option[Address]=None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(companyDetails))
    val result = TestCorrespondenceAddressController.editAddress().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestCorrespondenceAddressController.editAddress().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestCorrespondenceAddressController.editAddress().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUserSuccess(testAddress: Option[AddressDetails] = None)
                                     (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionDataService.updateCorrespondenceAddressDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testAddress))
    val result = TestCorrespondenceAddressController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestCorrespondenceAddressController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestCorrespondenceAddressController.submit().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }
}
