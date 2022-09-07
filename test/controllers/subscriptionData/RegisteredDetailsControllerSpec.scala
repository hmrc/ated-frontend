/*
 * Copyright 2022 HM Revenue & Customs
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
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Environment
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DetailsService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import views.html.BtaNavigationLinks
import views.html.subcriptionData.registeredDetails

import scala.concurrent.Future

class RegisteredDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockEnvironment: Environment = app.injector.instanceOf[Environment]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockDetailsService: DetailsService = mock[DetailsService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: registeredDetails = app.injector.instanceOf[views.html.subcriptionData.registeredDetails]

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testRegisteredDetailsController: RegisteredDetailsController = new RegisteredDetailsController(
      mockMcc,
      mockAuthAction,
      mockSubscriptionDataService,
      mockServiceInfoService,
      mockEnvironment,
      injectedViewInstance
  )

  def getWithAuthorisedUser(registeredDetails: Option[RegisteredDetails]=None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockSubscriptionDataService.getRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(registeredDetails))
    when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))

    val result = testRegisteredDetailsController.edit().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser: Future[Result] =  {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    testRegisteredDetailsController.edit()(SessionBuilder.buildRequestWithSession(userId))
  }

  def submitWithAuthorisedUserSuccess(updatedDetails: Option[RegisteredDetails]=None)
                                     (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockSubscriptionDataService.updateRegisteredDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(updatedDetails))
    val result = testRegisteredDetailsController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = testRegisteredDetailsController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}

  override def beforeEach(): Unit = {
  }

  "RegisteredDetailsController" must {
    "editAddress" must {

      "unauthorised users" must {
        "respond with a redirect" in new Setup {
            status(getWithUnAuthorisedUser) must be(SEE_OTHER)
        }

        "be redirected to the login page" in new Setup {
            redirectLocation(getWithUnAuthorisedUser).get must include("/ated/unauthorised")

        }
      }

      "Authorised users" must {

        "respond with OK" in new Setup {
          getWithAuthorisedUser() {
            result =>
              status(result) must be(OK)
          }
        }

        "show the Registered details view with empty data" in new Setup {
          getWithAuthorisedUser(None) {
            result =>
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be (TitleBuilder.buildTitle("Edit business name and address"))
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
              document.select("h1").text() must include("Edit business name and address")
              document.getElementById("addressDetails.addressLine1").attr("value") must be("")
              document.getElementById("addressDetails.addressLine2").attr("value") must be("")
              document.getElementById("addressDetails.addressLine3").attr("value") must be("")
              document.getElementById("addressDetails.addressLine4").attr("value") must be("")
              document.getElementById("addressDetails.postalCode").attr("value") must be("")
              document.getElementsByAttributeValue("for", "addressDetails.countryCode").text() must include("Country")
              document.getElementsByTag("button").text() must be("Save")
          }
        }

        "show the Registered details view with populated data" in new Setup {
          val businessPartnerDetails: RegisteredDetails = RegisteredDetails(isEditable = false, "testName",
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
              document.select("h1").text() must include("Edit business name and address")
              document.getElementById("addressDetails.addressLine1").attr("value") must be("bpline1")
              document.getElementById("addressDetails.addressLine2").attr("value") must be("bpline2")
              document.getElementById("addressDetails.addressLine3").attr("value") must be("bpline3")
              document.getElementById("addressDetails.addressLine4").attr("value") must be("bpline4")
              document.getElementById("addressDetails.postalCode").attr("value") must be("postCode")
              document.getElementsByAttributeValue("for", "addressDetails.countryCode").text() must include("Country")
              document.getElementsByTag("button").text() must be("Save")
          }
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

        "be redirected to the unauthorised page" in new Setup {
            redirectLocation(getWithUnAuthorisedUser).get must include("/ated/unauthorised")
        }
      }

      "Authorised users" must {
        "validate form" must {

          "If registration details entered are valid, save and continue button must redirect to contact details page if the save worked" in new Setup {
            val inputJson: JsValue = Json.parse(
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

          "If registration details entered are valid but the save fails, throw a validation error" in new Setup {
            val inputJson: JsValue = Json.parse(
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

          "not be empty" in new Setup {
            val inputJson: JsValue = Json.parse(
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
                contentAsString(result) must include("You must enter a business name")
                contentAsString(result) must include("You must enter address line 1")
                contentAsString(result) must include("You must enter address line 2")
                contentAsString(result) must include("You must enter a country")
            }
          }

          "Details entered contains spaces" in new Setup {
            val inputJson: JsValue = Json.parse(
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
                contentAsString(result) must include("You must enter a business name")
                contentAsString(result) must include("You must enter address line 1")
                contentAsString(result) must include("You must enter address line 2")
                contentAsString(result) must include("You must enter a country")
            }
          }

          "If entered, data must not be too long" in new Setup {
            val inputJson: JsValue = Json.parse(
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
                contentAsString(result) must include("You must enter a country")
            }
          }
        }
      }
    }
  }
}
