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

import builders._
import config.ApplicationConfig
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
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{BackLinkCacheService, ChangeLiabilityReturnService, DataCacheService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.propertyDetails.propertyDetailsAddress

import scala.concurrent.Future

class PropertyDetailsAddressControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsTitleController: PropertyDetailsTitleController = mock[PropertyDetailsTitleController]
  val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockChangeLiabilityReturnService: ChangeLiabilityReturnService = mock[ChangeLiabilityReturnService]
  val mockBackLinkCacheService: BackLinkCacheService = mock[BackLinkCacheService]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: propertyDetailsAddress = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsAddress]

  val periodKey: Int = 2015

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testPropertyDetailsAddressController: PropertyDetailsAddressController = new PropertyDetailsAddressController(
      mockMcc,
      mockAuditConnector,
      mockAuthAction,
      mockChangeLiabilityReturnService,
      mockServiceInfoService,
      mockPropertyDetailsService,
      mockDataCacheService,
      mockBackLinkCacheService,
      injectedViewInstance
    )

    def createWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsAddressController.createNewDraft(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def createWithAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheService.saveFormData[Boolean]
        (ArgumentMatchers.eq(AtedConstants.SelectedPreviousReturn), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))
      val result = testPropertyDetailsAddressController.createNewDraft(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewDataWithAuthorisedUser(id: String, propertyDetails: PropertyDetails, fromConfirmAddressPage: Boolean)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails
      (ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsAddressController.view(id, fromConfirmAddressPage, periodKey, None).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewDataWithAuthorisedUserChangeReturn(id: String, propertyDetails: PropertyDetails, fromConfirmAddressPage: Boolean)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails
      (ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsAddressController.view(
        id, fromConfirmAddressPage, periodKey, Some("editSubmitted")).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewSubmittedWithAuthorisedUser(id: String, propertyDetails: Option[PropertyDetails])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheService.fetchAndGetData[Boolean](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(propertyDetails))
      val result = testPropertyDetailsAddressController.editSubmittedReturn(id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def editFromSummary(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsAddressController.editFromSummary(id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def addressLookupRedirect(id: Option[String])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testPropertyDetailsAddressController.addressLookupRedirect(id, periodKey, None).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithUnAuthorisedUser(id: Option[String], fromConfirmAddressPage: Boolean)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsAddressController.save(id, periodKey, None, fromConfirmAddressPage).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(id: Option[String], inputJson: JsValue, fromConfirmAddressPage: Boolean)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      id match {
        case Some(x) =>
          when(mockPropertyDetailsService.saveDraftPropertyDetailsAddress(
            ArgumentMatchers.eq(x), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
            thenReturn(Future.successful(x))
        case None =>
          when(mockPropertyDetailsService.createDraftPropertyDetailsAddress(
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
            thenReturn(Future.successful("1"))
      }
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testPropertyDetailsAddressController.save(id, periodKey, None, fromConfirmAddressPage)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))

      test(result)
    }
  }

  "PropertyDetailsAddressController" must {
    "propertyDetails" must {

      "unauthorised users" must {
        "respond with a redirect" in new Setup {
          createWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in new Setup {
          createWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the chargeable property details view if we have no id" in new Setup {
          createWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Enter the address of the property manually"))

              document.getElementById("line_1").attr("value") must be("")
              document.getElementsByClass("govuk-button").text() must be("Save and continue")
          }
        }

        "show the chargeable property details view if we have id and data with fromConfirmAddressPage is false" in new Setup {
          viewDataWithAuthorisedUser("1", PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")), fromConfirmAddressPage = false) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Enter the address of the property manually"))
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")

              document.getElementById("line_1").attr("value") must be("addr1")
              document.getElementById("line_2").attr("value") must be("addr2")
              document.getElementById("line_3").attr("value") must be("addr3")
              document.getElementById("line_4").attr("value") must be("addr4")
              document.getElementById("postcode").attr("value") must be("postCode")
          }
        }

        "show the chargeable property details view if we have id and data with fromConfirmAddressPage is true" in new Setup {
          viewDataWithAuthorisedUser("1", PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")), fromConfirmAddressPage = true) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Edit address"))
              assert(document.getElementsByClass("govuk-heading-xl").text.contains("Edit address") === true)
              document.getElementsByClass("govuk-button").text must be("Save and continue")
              document.getElementById("line_1").attr("value") must be("addr1")
              document.getElementById("line_2").attr("value") must be("addr2")
              document.getElementById("line_3").attr("value") must be("addr3")
              document.getElementById("line_4").attr("value") must be("addr4")
              document.getElementById("postcode").attr("value") must be("postCode")
              document.getElementsByClass("govuk-back-link").text must be("Back")
              document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/liability/confirm-address/view")
          }
        }

        "show the chargeable property details view with back link to EditLiabilityType" in new Setup {
          viewDataWithAuthorisedUserChangeReturn("1",PropertyDetailsBuilder.getPropertyDetailsWithFormBundleReturn("1"), fromConfirmAddressPage = false) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Enter the address of the property manually"))
              document.getElementsByTag("h1").text() must include ("Enter the address of the property manually")
              document.getElementsByClass("govuk-button").text() must be("Save and continue")
              document.getElementById("line_1").attr("value") must be("addr1")
              document.getElementById("line_2").attr("value") must be("addr2")
              document.getElementById("line_3").attr("value") must be("addr3")
              document.getElementById("line_4").attr("value") must be("addr4")
              document.getElementsByClass("govuk-back-link").text must be("Back")
              document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/liability/1/edit/2015?editAllowed=true")
          }
        }
      }
    }
  }


    "view submitted" must {
      "show the details of a submitted return" in new Setup {
        viewSubmittedWithAuthorisedUser("1", Some(PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")))) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Enter the address of the property manually"))

            document.getElementById("line_1").attr("value") must be("addr1")
            document.getElementById("line_2").attr("value") must be("addr2")
            document.getElementById("line_3").attr("value") must be("addr3")
            document.getElementById("line_4").attr("value") must be("addr4")
            document.getElementById("postcode").attr("value") must be("postCode")
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

    "edit from summary" must {
      "show the details of a submitted return with a back link" in new Setup {
        editFromSummary("1", PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Enter the address of the property manually"))

            document.getElementsByClass("govuk-back-link").text must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/liability/create/summary")
        }
      }
    }

    "addressLookupRedirect" must {
      "redirect to the address lookup page" in new Setup {
        addressLookupRedirect(Some("1")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/address-lookup/view/2015?propertyKey=1")
        }
      }

      "redirect to the address lookup page if we have no id" in new Setup {
        addressLookupRedirect(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/address-lookup/view/2015")
        }
      }
    }
    "save" must {
      "unauthorised users" must {

        "be redirected to the login page with fromConfirmPage false" in new Setup {
          saveWithUnAuthorisedUser(None, fromConfirmAddressPage = false) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }

        "be redirected to the login page with fromConfirmPage true" in new Setup {
          saveWithUnAuthorisedUser(None, fromConfirmAddressPage = true) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "for invalid data, return BAD_REQUEST with fromConfirmAddress false" in new Setup {

          val inputJson: JsValue = Json.parse( """{"rentalBusiness": true, "isAvoidanceScheme": "true"}""")
          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(None, inputJson, fromConfirmAddressPage = false) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for invalid data, return BAD_REQUEST with fromConfirmAddress true" in new Setup {

          val inputJson: JsValue = Json.parse( """{"rentalBusiness": true, "isAvoidanceScheme": "true"}""")
          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(None, inputJson, fromConfirmAddressPage = true) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for valid data with no id, return OK with fromConfirmAddress false" in new Setup {
          val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(postCode = Some("XX1 1XX"))
          when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(None))
          submitWithAuthorisedUser(None, Json.toJson(propertyDetails), fromConfirmAddressPage = false) {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }

        "for valid data with no id, return OK with fromConfirmAddress true" in new Setup {
          val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(postCode = Some("XX1 1XX"))
          when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(None))
          submitWithAuthorisedUser(None, Json.toJson(propertyDetails), fromConfirmAddressPage = true) {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }

        "for valid data, return OK with fromConfirmAddress false" in new Setup {
          val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(postCode = Some("XX1 1XX"))
          when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Some("1"),Json.toJson(propertyDetails), fromConfirmAddressPage = false) {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }

        "for valid data, return OK with fromConfirmAddress true" in new Setup {
          val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(postCode = Some("XX1 1XX"))
          when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Some("1"),Json.toJson(propertyDetails), fromConfirmAddressPage = true) {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }
      }

}
}
