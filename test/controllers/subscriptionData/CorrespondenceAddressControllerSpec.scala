/*
 * Copyright 2026 HM Revenue & Customs
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
import models.*
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Environment
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContentAsFormUrlEncoded, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{DataCacheService, DetailsService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import views.html.{BtaNavigationLinks, global_error}
import views.html.subcriptionData.correspondenceAddress

import scala.concurrent.Future

class CorrespondenceAddressControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  given hc: HeaderCarrier = HeaderCarrier()
  given mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockDetailsService: DetailsService = mock[DetailsService]
  val mockEnvironment: Environment = app.injector.instanceOf[Environment]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  given messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: correspondenceAddress = app.injector.instanceOf[views.html.subcriptionData.correspondenceAddress]
  val injectedViewInstanceError: global_error = app.injector.instanceOf[views.html.global_error]


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
      mockServiceInfoService,
      mockEnvironment,
      injectedViewInstance,
      injectedViewInstanceError
    )

    def getWithAuthorisedUser(companyDetails: Option[Address]=None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(using ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockSubscriptionDataService.getCorrespondenceAddress(using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(companyDetails))
      val result = testCorrespondenceAddressController.editAddress.apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testCorrespondenceAddressController.editAddress.apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUserSuccess(testAddress: Option[AddressDetails] = None)
                                       (fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockSubscriptionDataService.updateCorrespondenceAddressDetails(ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(testAddress))
      val result = testCorrespondenceAddressController.submit.apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

      test(result)
    }

    def submitWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testCorrespondenceAddressController.submit.apply(SessionBuilder.buildRequestWithSession(userId))
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
              document.select("h1").text() must include("Edit your correspondence address")
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
              document.getElementById("addressLine1").attr("value") must be("")
              document.getElementById("addressLine2").attr("value") must be("")
              document.getElementById("addressLine3").attr("value") must be("")
              document.getElementById("addressLine4").attr("value") must be("")
              document.getElementById("postalCode").attr("value") must be("")
              document.getElementsByAttributeValue("for", "countryCode").text() must include("Country")
              document.getElementById("addressType").attr("value") must be("")
              document.getElementsByTag("button").text() must not be None
          }
        }


        "show the correspondence address view with populated space in data" in new Setup {
          val testAddressDetails: AddressDetails = AddressDetails("Correspondence", "  ", "  ", Some("line_3"), Some("line_4"), Some("postCode"), "GB")
          val testAddress: Address = Address(Some("name1"), Some("name2"), addressDetails = testAddressDetails, contactDetails = None)

          getWithAuthorisedUser(Some(testAddress)) {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be (TitleBuilder.buildTitle("Edit your correspondence address"))
              document.select("h1").text() must include("Edit your correspondence address")
              document.getElementById("addressType").attr("value") must be("Correspondence")
              document.getElementById("addressLine1").attr("value") must be("  ")
              document.getElementById("addressLine2").attr("value") must be("  ")
              document.getElementById("addressLine3").attr("value") must be("line_3")
              document.getElementById("addressLine4").attr("value") must be("line_4")
              document.getElementById("postalCode").attr("value") must be("postCode")
              document.getElementsByTag("button").text() must not be None
              document.getElementsByAttributeValue("for", "countryCode").text() must include("Country")
          }
        }

        "show the correspondence address view with populated data" in new Setup {
          val testAddressDetails: AddressDetails = AddressDetails("Correspondence", "line_1", "line_2", Some("line_3"), Some("line_4"), Some("postCode"), "GB")
          val testAddress: Address = Address(Some("name1"), Some("name2"), addressDetails = testAddressDetails, contactDetails = None)

          getWithAuthorisedUser(Some(testAddress)) {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be (TitleBuilder.buildTitle("Edit your correspondence address"))
              document.select("h1").text() must include("Edit your correspondence address")
              document.getElementById("addressType").attr("value") must be("Correspondence")
              document.getElementById("addressLine1").attr("value") must be("line_1")
              document.getElementById("addressLine2").attr("value") must be("line_2")
              document.getElementById("addressLine3").attr("value") must be("line_3")
              document.getElementById("addressLine4").attr("value") must be("line_4")
              document.getElementById("postalCode").attr("value") must be("postCode")
              document.getElementsByTag("button").text() must not be None
              document.getElementsByAttributeValue("for", "countryCode").text() must include("Country")
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
                val addressDetails = AddressDetails(
                  addressType = "Permanent",
                  addressLine1 = "Line One",
                  addressLine2 = "Line Two",
                  addressLine3 = Some("Line Three"),
                  addressLine4 = Some("Line Four"),
                  postalCode = Some("XX1 1XX"),
                  countryCode = "GB"
                )

                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> "Line One",
                    "addressLine2" -> "Line Two",
                    "addressLine3" -> "Line Three",
                    "addressLine4" -> "Line Four",
                    "postalCode" -> "XX1 1XX",
                    "countryCode" -> "GB"
                  )

                submitWithAuthorisedUserSuccess(Some(addressDetails))(fakeRequest) { result =>
                  status(result) mustBe SEE_OTHER
                  redirectLocation(result).get must include("/ated/company-details")
                }
              }

              "If registration details entered are valid but the save fails, throw a global error" in new Setup {

                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> "Line One",
                    "addressLine2" -> "Line Two",
                    "addressLine3" -> "Line Three",
                    "addressLine4" -> "Line Four",
                    "postalCode" -> "XX1 1XX",
                    "countryCode" -> "GB"
                  )

                submitWithAuthorisedUserSuccess(None)(fakeRequest) {
                  result =>
                    val document = Jsoup.parse(contentAsString(result))
                    status(result) must be(OK)
                    document.title() must be ("Sorry, there is a problem with the service")
                    contentAsString(result) must include("Sorry, there is a problem with the service")
                    contentAsString(result) must include("You will be able to use the service later.")
                    contentAsString(result) must include("Your saved returns and drafts are not affected by this problem.")
                }

              }


              "If address line 1 in correspondence details entered is with spaces  but the save fails, throw a validation error" in new Setup {
                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> " ",
                    "addressLine2" -> " ",
                    "addressLine3" -> "Line Three",
                    "addressLine4" -> "Line Four",
                    "postalCode" -> "XX11XX",
                    "countryCode" -> "GB"
                  )

                submitWithAuthorisedUserSuccess(None)(fakeRequest) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                }
              }

              "not be empty" in new Setup {
                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> "",
                    "addressLine2" -> "",
                    "addressLine3" -> "",
                    "addressLine4" -> "",
                    "postalCode" -> "",
                    "countryCode" -> ""
                  )

                submitWithAuthorisedUserSuccess(None)(fakeRequest) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Enter address line 1")
                    contentAsString(result) must include("Enter address line 2")
                    contentAsString(result) must include("Enter a country")
                }
              }

              "If entered, Address line 1 must be maximum of 35 characters" in new Setup {

                val addressDetails = AddressDetails(
                  addressType = "Permanent",
                  addressLine1 = "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                  addressLine2 = "Line Two",
                  addressLine3 = Some("Line Three"),
                  addressLine4 = Some("Line Four"),
                  postalCode = Some("XX1 1XX"),
                  countryCode = "GB"
                )
  
                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                    "addressLine2" -> "Line Two",
                    "addressLine3" -> "Line Three",
                    "addressLine4" -> "Line Four",
                    "postalCode" -> "XX1 1XX",
                    "countryCode" -> "GB"
                  )
  
                submitWithAuthorisedUserSuccess(Some(addressDetails))(fakeRequest) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Address line 1 cannot be more than 35 characters")
                }
              }

              "If entered, Address line 2 must be maximum of 35 characters" in new Setup {
                val addressDetails = AddressDetails(
                  addressType = "Permanent",
                  addressLine1 = "Line One",
                  addressLine2 = "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                  addressLine3 = Some("Line Three"),
                  addressLine4 = Some("Line Four"),
                  postalCode = Some("XX1 1XX"),
                  countryCode = "GB"
                )
  
                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> "Line One",
                    "addressLine2" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                    "addressLine3" -> "Line Three",
                    "addressLine4" -> "Line Four",
                    "postalCode" -> "XX1 1XX",
                    "countryCode" -> "GB"
                  )
  
                submitWithAuthorisedUserSuccess(Some(addressDetails))(fakeRequest) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Address line 2 cannot be more than 35 characters")
                }
              }

              "If entered, Address line 3 must be maximum of 35 characters" in new Setup {
                val addressDetails = AddressDetails(
                  addressType = "Permanent",
                  addressLine1 = "Line One",
                  addressLine2 = "Line Two",
                  addressLine3 = Some("AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD"),
                  addressLine4 = Some("Line Four"),
                  postalCode = Some("XX1 1XX"),
                  countryCode = "GB"
                )

                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> "Line One",
                    "addressLine2" -> "Line Two",
                    "addressLine3" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                    "addressLine4" -> "Line Four",
                    "postalCode" -> "XX1 1XX",
                    "countryCode" -> "GB"
                  )

                submitWithAuthorisedUserSuccess(Some(addressDetails))(fakeRequest) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Address line 3 cannot be more than 35 characters")
                }
              }

              "If entered, Address line 4 must be maximum of 35 characters" in new Setup {
                val addressDetails = AddressDetails(
                  addressType = "Permanent",
                  addressLine1 = "Line One",
                  addressLine2 = "Line Two",
                  addressLine3 = Some("Line Three"),
                  addressLine4 = Some("AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD"),
                  postalCode = Some("XX1 1XX"),
                  countryCode = "GB"
                )

                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> "Line One",
                    "addressLine2" -> "Line Two",
                    "addressLine3" -> "Line Three",
                    "addressLine4" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDD",
                    "postalCode" -> "XX1 1XX",
                    "countryCode" -> "GB"
                  )

                submitWithAuthorisedUserSuccess(Some(addressDetails))(fakeRequest) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Address line 4 cannot be more than 35 characters")
                }
              }

               "Postcode must not be more than 9 characters" in new Setup {
                 val addressDetails = AddressDetails(
                   addressType = "Permanent",
                   addressLine1 = "Line One",
                   addressLine2 = "Line Two",
                   addressLine3 = Some("Line Three"),
                   addressLine4 = Some("Line Four"),
                   postalCode = Some("asssaa34aaaaaa"),
                   countryCode = "GB"
                 )

                 val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                   .withMethod("POST")
                   .withFormUrlEncodedBody(
                     "addressType" -> "Permanent",
                     "addressLine1" -> "Line One",
                     "addressLine2" -> "Line Two",
                     "addressLine3" -> "Line Three",
                     "addressLine4" -> "Line Four",
                     "postalCode" -> "asssaa34aaaaaa",
                     "countryCode" -> "GB"
                   )

                 submitWithAuthorisedUserSuccess(Some(addressDetails))(fakeRequest) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("The postcode cannot be more than 9 characters")
                }
              }

              "Postcode must not contain special characters" in new Setup {
                val addressDetails = AddressDetails(
                  addressType = "Permanent",
                  addressLine1 = "Line One",
                  addressLine2 = "Line Two",
                  addressLine3 = Some("Line Three"),
                  addressLine4 = Some("Line Four"),
                  postalCode = Some("aaa,uu"),
                  countryCode = "GB"
                )

                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> "Line One",
                    "addressLine2" -> "Line Two",
                    "addressLine3" -> "Line Three",
                    "addressLine4" -> "Line Four",
                    "postalCode" -> "aaa,uu",
                    "countryCode" -> "GB"
                  )

                submitWithAuthorisedUserSuccess(Some(addressDetails))(fakeRequest) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Enter a valid postcode")
                }
              }

              "Postcode must entered if country code is UK" in new Setup {
                val addressDetails = AddressDetails(
                  addressType = "Permanent",
                  addressLine1 = "Line One",
                  addressLine2 = "Line Two",
                  addressLine3 = Some("Line Three"),
                  addressLine4 = Some("Line Four"),
                  postalCode = Some(""),
                  countryCode = "GB"
                )

                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> "Line One",
                    "addressLine2" -> "Line Two",
                    "addressLine3" -> "Line Three",
                    "addressLine4" -> "Line Four",
                    "postalCode" -> "",
                    "countryCode" -> "GB"
                  )

                submitWithAuthorisedUserSuccess(Some(addressDetails))(fakeRequest) {
                  result =>
                    status(result) must be(BAD_REQUEST)
                    contentAsString(result) must include("Enter a UK postcode if UK is selected in the country field.")
                }
              }

              "Country Code must be selected" in new Setup {
                val addressDetails = AddressDetails(
                  addressType = "Permanent",
                  addressLine1 = "Line One",
                  addressLine2 = "Line Two",
                  addressLine3 = Some("Line Three"),
                  addressLine4 = Some("Line Four"),
                  postalCode = Some("aaa,uu"),
                  countryCode = ""
                )

                val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
                  .withMethod("POST")
                  .withFormUrlEncodedBody(
                    "addressType" -> "Permanent",
                    "addressLine1" -> "Line One",
                    "addressLine2" -> "Line Two",
                    "addressLine3" -> "Line Three",
                    "addressLine4" -> "Line Four",
                    "postalCode" -> "aaa,uu",
                    "countryCode" -> ""
                  )

                submitWithAuthorisedUserSuccess(Some(addressDetails))(fakeRequest) {
                  result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Enter a country")
                }
              }
            }
          }
        }
      }
    }
  }
}
