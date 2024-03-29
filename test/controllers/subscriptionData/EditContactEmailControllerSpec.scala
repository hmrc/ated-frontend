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
import models._
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
import play.api.test.Helpers.{contentAsString, _}
import services.{ServiceInfoService, SubscriptionDataService}
import testhelpers.{MockAuthUtil, TestUtil}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class EditContactEmailControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach
  with MockAuthUtil with TestUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.subcriptionData.editContactEmail]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testEditContactEmailController: EditContactEmailController = new EditContactEmailController(
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
      val result = testEditContactEmailController.edit().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(contactDetailsEmail: Option[EditContactDetailsEmail] = None, emailConsent: Boolean)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockSubscriptionDataService.getEmailWithConsent(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(contactDetailsEmail))
      val result = testEditContactEmailController.edit().apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def submitWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)

      val result = testEditContactEmailController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUserSuccess(testAddress: Option[EditContactDetailsEmail] = None)
                                       (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockSubscriptionDataService.editEmailWithConsent(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(testAddress))
      val result = testEditContactEmailController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

      test(result)

    }
  }


  override def beforeEach(): Unit = {
  }

  "EditContactEmailController " must {
    "unauthorised users" must {

      "respond with a redirect" in new Setup {
        getWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }
    "edit" must {
      "return show the correct page if a client and we have empty data" in new Setup {
        getWithAuthorisedUser(None, emailConsent = true) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Edit your ATED email address"))
            document.getElementById("emailConsent").attributes.hasKey("checked") mustBe false
            document.getElementById("emailConsent-2").attributes.hasKey("checked") mustBe false
            document.getElementById("emailAddress").attr("value") must be("")

            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated/company-details")

            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
        }
      }

      "show email consent and address with populated data" in new Setup {

        val testContactEmail = EditContactDetailsEmail(emailConsent = true, emailAddress = "hrmc@hmrc.com")

        getWithAuthorisedUser(Some(testContactEmail), emailConsent = true) {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("emailAddress").attr("value") must be("hrmc@hmrc.com")
            document.getElementById("emailConsent").attributes.hasKey("checked") mustBe true
            document.getElementById("emailConsent-2").attributes.hasKey("checked") mustBe false
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

          "Email address must not be more than 241 characters if entered" in new Setup {
            val emailTest: String = "a" * 240 + "@gmail.com"
            val inputJson: JsValue = Json.parse( s"""{"emailAddress": "$emailTest", "emailConsent": true }""")

            submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Email address must not be more than 241 characters")
            }
          }

          "Email address must be a valid email address format" in new Setup {
            val inputJson: JsValue = Json.parse( s"""{ "emailAddress": "abcdef@com", "emailConsent": true }""")

            submitWithAuthorisedUserSuccess(None)(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Email address is not valid")
            }

          }

          "If edited contact address is valid, submit must redirect" in new Setup {
            val inputJson: JsValue = Json.parse( s"""{ "emailAddress": "aa@mail.com", "emailConsent": true }""")
            val contactAddress: EditContactDetailsEmail = inputJson.as[EditContactDetailsEmail]

            submitWithAuthorisedUserSuccess(Some(contactAddress))(FakeRequest().withJsonBody(inputJson)) {
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
