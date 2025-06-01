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

package controllers.propertyDetails

import java.util.UUID

import builders.{PropertyDetailsBuilder, SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheService, DataCacheConnector}
import controllers.auth.AuthAction
import models.PropertyDetails
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services._
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import utils.AtedConstants
import views.html.propertyDetails.confirmAddress
import views.html.{BtaNavigationLinks, global_error}

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MockitoSugar with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
  val mockAddressLookupService: AddressLookupService = mock[AddressLookupService]
  val mockBackLinkCacheConnector: BackLinkCacheService = mock[BackLinkCacheService]
  val mockChangeLiabilityReturnService: ChangeLiabilityReturnService = mock[ChangeLiabilityReturnService]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: confirmAddress = app.injector.instanceOf[views.html.propertyDetails.confirmAddress]
  val injectedViewInstanceError: global_error = app.injector.instanceOf[views.html.global_error]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testConfirmAddressController: ConfirmAddressController = new ConfirmAddressController(
      mockMcc,
      mockAuthAction,
      mockChangeLiabilityReturnService,
      mockServiceInfoService,
      mockBackLinkCacheConnector,
      mockPropertyDetailsService,
      mockDataCacheConnector,
      injectedViewInstance,
      injectedViewInstanceError
    )

    val periodKey: Int = 2015

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Any = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testConfirmAddressController.view("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(test: Future[Result] => Any): Any = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      noDelegationModelAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
        Future.successful(PropertyDetailsCacheSuccessResponse(PropertyDetailsBuilder.getPropertyDetails("1")))
      }
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testConfirmAddressController.view("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUserChangeReturn(test: Future[Result] => Any): Any = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      noDelegationModelAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
        Future.successful(PropertyDetailsCacheSuccessResponse(PropertyDetailsBuilder.getPropertyDetailsWithFormBundleReturn("1")))
      }
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testConfirmAddressController.view("1", periodKey, Some("editSubmitted")).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUserEditSubmitted(test: Future[Result] => Any): Any = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      noDelegationModelAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
        Future.successful(PropertyDetailsCacheSuccessResponse(PropertyDetailsBuilder.getFullPropertyDetails("1")))
      }
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testConfirmAddressController.view("1", periodKey, Some("editPrevReturn")).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewSubmittedWithAuthorisedUser(id: String, propertyDetails: Option[PropertyDetails])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(propertyDetails))
      val result = testConfirmAddressController.editSubmittedReturn(id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUserNotFoundResponse(test: Future[Result] => Any): Any = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      noDelegationModelAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
        Future.successful(PropertyDetailsCacheNotFoundResponse)
      }
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testConfirmAddressController.view("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUserErrorResponse(test: Future[Result] => Any): Any = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      noDelegationModelAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
        Future.successful(PropertyDetailsCacheNotFoundResponse)
      }
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testConfirmAddressController.view("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = testConfirmAddressController.submit("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = testConfirmAddressController.submit("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }


  "ConfirmAddressController" must {

    "view" must {

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

      "Authorised users" must {

        "show correct property details with a back link to address lookup" in new Setup {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Confirm address"))
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
              document.getElementById("editAddress").text() must be("Edit address")
              document.getElementsByClass("govuk-button").text() must be("Confirm and continue")
              document.getElementById("address").text() must be("addr1 addr2 addr3 addr4")
              document.getElementsByClass("govuk-back-link").text must be("Back")
              document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/liability/address-lookup/view/2015")
          }
        }
          "show correct property details with a back link to enter address manually" in new Setup {
            getWithAuthorisedUserChangeReturn {
              result =>
                status(result) must be(OK)
                val document = Jsoup.parse(contentAsString(result))
                document.title() must be(TitleBuilder.buildTitle("Confirm address"))
                document.getElementById("editAddress").text() must be("Edit address")
                document.getElementsByClass("govuk-button").text() must be("Confirm and continue")
                document.getElementById("address").text() must be("addr1 addr2 addr3 addr4")
                document.getElementsByClass("govuk-back-link").text must be("Back")
                document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/liability/create/address/view/1/false/2015?mode=editSubmitted")
            }
          }
            "show correct property details with a back link to select property from previous year" in new Setup {
              getWithAuthorisedUserEditSubmitted {
                result =>
                  status(result) must be(OK)
                  val document = Jsoup.parse(contentAsString(result))
                  document.title() must be(TitleBuilder.buildTitle("Confirm address"))
                  document.getElementById("editAddress").text() must be("Edit address")
                  document.getElementsByClass("govuk-button").text() must be("Confirm and continue")
                  document.getElementById("address").text() must be("addr1 addr2 addr3 addr4")
                  document.getElementsByClass("govuk-back-link").text must be("Back")
                  document.getElementsByClass("govuk-back-link").text must be("Back")
                  document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/existing-return/select/2015/charge")
              }
            }

        "show error" in new Setup {
          getWithAuthorisedUserNotFoundResponse {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Sorry, there is a problem with the service")
              document.getElementById("header").text() must be("Sorry, there is a problem with the service")
          }
        }

        "return to error page where Internal Error thrown" in new Setup {
          getWithAuthorisedUserErrorResponse {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Sorry, there is a problem with the service")
              document.getElementsByTag("h1").text() must be("Sorry, there is a problem with the service")
              document.getElementById("message1").text() must be("You will be able to use the service later.")
              document.getElementById("message2").text must be ("Your saved returns and drafts are not affected by this problem.")

          }
        }
      }
    }

    "editSubmittedReturn" must {
      "show the address of a previously used property" in new Setup {
        viewSubmittedWithAuthorisedUser("1", Some(PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")))) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Confirm address"))
            document.getElementById("editAddress").text() must be("Edit address")
            document.getElementsByClass("govuk-button").text() must be("Confirm and continue")
            document.getElementById("address").text() must be("addr1 addr2 addr3 addr4 postCode")
        }
      }

      "Return to the account summary if we have no details" in new Setup {
        viewSubmittedWithAuthorisedUser("1", None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/account-summary")
        }
      }
    }

    "submit" must {

      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          submitWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "authorised users" must {

        "redirect to declaration page" in new Setup {
          when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(None))
          submitWithAuthorisedUser {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some("/ated/liability/create/title/view/1"))
          }
        }
      }
    }
  }
}
