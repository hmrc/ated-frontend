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
import connectors.DataCacheConnector
import controllers.auth.AuthAction
import testhelpers.MockAuthUtil
import models._
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
import services.{DetailsService, SubscriptionDataService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CorrespondenceAddressControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockDetailsService: DetailsService = mock[DetailsService]
  val mockEnvironment: Environment = app.injector.instanceOf[Environment]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testCorrespondenceAddressController: CorrespondenceAddressController = new CorrespondenceAddressController(
      mockMcc,
      mockAuthAction,
      mockSubscriptionDataService,
      mockEnvironment
    )

    def getWithAuthorisedUser(companyDetails: Option[Address]=None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockSubscriptionDataService.getCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(companyDetails))
      val result = testCorrespondenceAddressController.editAddress().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testCorrespondenceAddressController.editAddress().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUserSuccess(testAddress: Option[AddressDetails] = None)
                                       (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockSubscriptionDataService.updateCorrespondenceAddressDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(testAddress))
      val result = testCorrespondenceAddressController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

      test(result)
    }

    def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testCorrespondenceAddressController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }
  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSubscriptionDataService)
  }

  "CorrespondenceAddressController" must {
    "editAddress" must {
      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in new Setup {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "respond with OK" in new Setup {
          getWithAuthorisedUser() {
            result =>
              status(result) must be(OK)
          }
        }

        "show the correspondence address view with empty data" in new Setup {
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


        "show the correspondence address view with populated space in data" in new Setup {
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

        "show the correspondence address view with populated data" in new Setup {
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

          "unauthorised users" must {

            "respond with a redirect" in new Setup {
              submitWithUnAuthorisedUser { result =>
                status(result) must be(SEE_OTHER)
              }
            }

            "be redirected to the login page" in new Setup {
              getWithUnAuthorisedUser { result =>
                redirectLocation(result).get must include("/ated/unauthorised")
              }
            }
          }

          "Authorised users" must {
            "validate form" must {
              "If registration details entered are valid, save and continue button must redirect to contact details page if the save worked" in new Setup {
                val inputJson: JsValue = Json.parse(
                  """{
                    |"addressType": "sadsdf",
                    |"addressLine1": "sdfsdf",
                    |"addressLine2": "asd",
                    |"addressLine3": "asd",
                    |"addressLine4": "asd",
                    |"postalCode": "XX1 1XX",
                    |"countryCode": "GB"}""".stripMargin)
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(SEE_OTHER)
                    redirectLocation(result).get must include("/ated/company-details")
                }
              }

              "If registration details entered are valid but the save fails, throw a validation error" in new Setup {
                val inputJson: JsValue = Json.parse(
                  """{
                    |"addressType": "sadsdf",
                    |"addressLine1": "sdfsdf",
                    |"addressLine2": "asd",
                    |"addressLine3": "asd",
                    |"addressLine4": "asd",
                    |"postalCode": "XX11XX",
                    |"countryCode": "GB"}""".stripMargin)

                submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                }
              }


              "If address line 1 in correspondence details entered is with spaces  but the save fails, throw a validation error" in new Setup {
                val inputJson: JsValue = Json.parse(
                  """{
                    |"addressType": "sadsdf",
                    |"addressLine1": " ",
                    |"addressLine2": " ",
                    |"addressLine3": "asd",
                    |"addressLine4": "asd",
                    |"postalCode": "XX11XX",
                    |"countryCode": "GB"}""".stripMargin)

                submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                }
              }

              "not be empty" in new Setup {
                val inputJson: JsValue = Json.parse(
                  """{
                    |"addressType": "",
                    |"addressLine1": "",
                    |"addressLine2": "",
                    |"addressLine3": "",
                    |"addressLine4": "",
                    |"postalCode": "",
                    |"countryCode": ""}""".stripMargin)

                submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                      contentAsString(result) must include("You must enter Address line 1")
                      contentAsString(result) must include("You must enter Address line 2")
                      contentAsString(result) must include("You must enter Country")
                }
              }

            "If entered, Address line 1 must be maximum of 35 characters" in new Setup {
              val inputJson: JsValue = Json.parse(
                """{
                  |"addressType": "",
                  |"addressLine1": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                  |"addressLine2": "",
                  |"addressLine3": "",
                  |"addressLine4": "",
                  |"postalCode": "",
                  |"countryCode": ""}""".stripMargin)
              val addressDetails: AddressDetails = inputJson.as[AddressDetails]
              submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Address line 1 cannot be more than 35 characters")
              }
            }


            "If entered, Address line 2 must be maximum of 35 characters" in new Setup {
              val inputJson: JsValue = Json.parse(
                """{
                  |"addressType": "",
                  |"addressLine1": "",
                  |"addressLine2": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                  |"addressLine3": "",
                  |"addressLine4": "",
                  |"postalCode": "",
                  |"countryCode": ""}""".stripMargin)
              val addressDetails: AddressDetails = inputJson.as[AddressDetails]
              submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Address line 2 cannot be more than 35 characters")
              }
            }
              "If entered, Address line 3 must be maximum of 35 characters" in new Setup {
                val inputJson: JsValue = Json.parse(
                  """{
                    |"addressType": "",
                    |"addressLine1": "",
                    |"addressLine2": "",
                    |"addressLine3": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                    |"addressLine4": "",
                    |"postalCode": "",
                    |"countryCode": ""}""".stripMargin)
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Address line 3 cannot be more than 35 characters")
                }
              }
              "If entered, Address line 4 must be maximum of 35 characters" in new Setup {
                val inputJson: JsValue = Json.parse(
                  """{
                    |"addressType": "",
                    |"addressLine1": "",
                    |"addressLine2": "",
                    |"addressLine3": "",
                    |"addressLine4": "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDWWWWWWWWWWWWWWW",
                    |"postalCode": "",
                    |"countryCode": ""}""".stripMargin)
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Address line 4 cannot be more than 35 characters")
                }
              }

               "Postcode must not be more than 9 characters" in new Setup {
                val inputJson: JsValue = Json.parse(
                  """{
                    |"addressType": "",
                    |"addressLine1": "",
                    |"addressLine2": "",
                    |"addressLine3": "",
                    |"addressLine4": "",
                    |"postalCode": "asssaa34aaaaaa",
                    |"countryCode": ""}""".stripMargin)
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("The postcode cannot be more than 9 characters")
                }
              }


              "Postcode must not contain special characters" in new Setup {
                val inputJson: JsValue = Json.parse(
                  """{
                    |"addressType": "",
                    |"addressLine1": "",
                    |"addressLine2": "",
                    |"addressLine3": "",
                    |"addressLine4": "",
                    |"postalCode": "aaa,uu",
                    |"countryCode": ""}""".stripMargin)
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("You must enter a valid postcode")
                }
              }


              "Country Code must be selected" in new Setup {
                val inputJson: JsValue = Json.parse(
                  """{
                    |"addressType": "",
                    |"addressLine1": "",
                    |"addressLine2": "",
                    |"addressLine3": "",
                    |"addressLine4": "",
                    |"postalCode": "",
                    |"countryCode": ""}""".stripMargin)
                val addressDetails: AddressDetails = inputJson.as[AddressDetails]
                submitWithAuthorisedUserSuccess(Some(addressDetails))(FakeRequest().withJsonBody(inputJson)) {
                  result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("You must enter Country")
                }
               }
              "throw a validation error, if save fails" in new Setup {
                val inputJson: JsValue = Json.parse(
                  """{
                    |"addressType": "Correspondence",
                    |"addressLine1": "sdfsdf",
                    |"addressLine2": "asd",
                    |"addressLine3": "asd",
                    |"addressLine4": "asd",
                    |"postalCode": "XX1 1XX",
                    |"countryCode": "GB"}""".stripMargin)
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
    }
}
