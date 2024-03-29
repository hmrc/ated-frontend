/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{Address, AddressDetails, ContactDetails, EditContactDetails}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class EditContactDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.subcriptionData.editContactDetails]

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testEditContactDetailsController: EditContactDetailsController = new EditContactDetailsController(
    mockMcc,
    mockAuthAction,
    mockServiceInfoService,
    mockSubscriptionDataService,
    injectedViewInstance
  )

  def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = testEditContactDetailsController.edit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(companyDetails: Option[Address] = None)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
    when(mockSubscriptionDataService.getCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(companyDetails))
    val result = testEditContactDetailsController.edit().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = testEditContactDetailsController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUserSuccess(testAddress: Option[EditContactDetails] = None)
                                     (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockSubscriptionDataService.editContactDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(testAddress))
    val result = testEditContactDetailsController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }
}

  override def beforeEach(): Unit = {
  }

  "EditContactDetailsController " must {
    "unauthorised users" must {

      "respond with a redirect" in new Setup {
        getWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page" in new Setup {
        getWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }

    "Authorised Users" must {
      "return business ATED contact details view with empty data" in new Setup {

        getWithAuthorisedUser(None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Edit your ATED contact details"))
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")

            document.getElementById("phoneNumber").attr("value") must be("")

            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated/company-details")

        }
      }

      "show contact address with populated data" in new Setup {

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

      "if contact details in not present in correspondence address, phonenum and emailAddress are empty" in new Setup {

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

      "If contact address is not pre-populated, have empty fields on the page" in new Setup {

        getWithAuthorisedUser(None) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            document.getElementById("phoneNumber").attr("value") must be("")
            document.getElementById("firstName").attr("value") must be("")
            document.getElementById("lastName").attr("value") must be("")
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

            "First name is not valid when entered spaces" in new Setup {
              val phoneNum: String = "a" * 24
              val inputJson: JsValue = Json.parse( s"""{ "firstName": " ", "lastName": "TestLastName", "phoneNumber": "$phoneNum"}""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Enter a first name")
              }
            }

            "First name is not valid when exceeds 35 chars" in new Setup {
              val phoneNum: String = "a" * 24
              val firstNameMax = "n" * 36
              val inputJson: JsValue = Json.parse( s"""{ "firstName": "$firstNameMax", "lastName": "TestLastName", "phoneNumber": "$phoneNum"}""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("First name cannot be more than 35 characters")
              }
            }

            "Last name is not valid when exceeds 35 chars" in new Setup {
              val phoneNum: String = "a" * 24
              val lastNameMax = "n" * 36
              val inputJson: JsValue = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "$lastNameMax", "phoneNumber": "$phoneNum"}""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Last name cannot be more than 35 characters")
              }
            }

            "Telephone number must not be more than 24 characters" in new Setup {
              val phoneNum: String = "a" * 25
              val inputJson: JsValue = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "TestLastName", "phoneNumber": "$phoneNum"}""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Telephone number must not be more than 24 characters")
              }
            }

            "Telephone number must not have invalid characters" in new Setup {
              val inputJson: JsValue = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "TestLastName", "phoneNumber": "@@@@@@@@"}""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Telephone number is not valid")
              }
            }

            "Telephone number must not have lower case characters" in new Setup {
              val inputJson: JsValue = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "TestLastName", "phoneNumber": "0191222x123"}""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include("Telephone number is not valid")
              }
            }


            "If edited contact address is valid, submit must redirect" in new Setup {

              val inputJson: JsValue = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "TestLastName", "phoneNumber": "9999999999" }""")
              val contactAddress: EditContactDetails = inputJson.as[EditContactDetails]

              submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
                result =>
                  status(result) must be(SEE_OTHER)
                  redirectLocation(result).get must include("/ated/company-details")
              }
            }

            "If contact address is valid but the save fails, throw a validation error" in new Setup {

              val inputJson: JsValue = Json.parse( s"""{ "firstName": "TestFirstName", "lastName": "TestLastName", "phoneNumber": "9999999999" }""")

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
