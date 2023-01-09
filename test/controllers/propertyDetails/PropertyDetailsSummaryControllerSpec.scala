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
import builders.{PropertyDetailsBuilder, SessionBuilder, _}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.PropertyDetails
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.propertyDetails.propertyDetailsSummary

import scala.concurrent.Future

class PropertyDetailsSummaryControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MockitoSugar with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockPropertyDetailsDeclarationController: PropertyDetailsDeclarationController = mock[PropertyDetailsDeclarationController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: propertyDetailsSummary = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsSummary]

  val organisationName = "ACME Limited"

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testPropertyDetailsSummaryController: PropertyDetailsSummaryController = new PropertyDetailsSummaryController(
      mockMcc,
      mockAuthAction,
      mockSubscriptionDataService,
      mockPropertyDetailsDeclarationController,
      mockServiceInfoService,
      mockPropertyDetailsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsSummaryController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.calculateDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(HttpResponse(OK, propertyDetails.toString)))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsSummaryController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithDeleteDraftLink(test: Future[Result] => Any) {
      val periodKey: Int = 2017
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(organisationName)))

      val result = testPropertyDetailsSummaryController.deleteDraft("123456", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def submitWithAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = testPropertyDetailsSummaryController.submit("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getPrintFriendlyWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.calculateDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(HttpResponse(OK, propertyDetails.toString)))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsSummaryController.viewPrintFriendlyLiabilityReturn("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  override def beforeEach: Unit = {
    reset(mockDelegationService, mockDelegationService,
      mockDataCacheConnector, mockSubscriptionDataService, mockPropertyDetailsService, mockServiceInfoService
    )
  }

  "PropertyDetailsSummaryController" must {
    "view" must {
      "unauthorised users" must {

        "respond with a redirect, and be redirected to unauthorised page" in new Setup {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "for authorised user" must {

        "status should be OK when we have a valid property details" in new Setup {

          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
          getWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Check your details are correct"))
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
          }
        }

        "no periods should be displayed with incomplete sections" in new Setup {

          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount =
            Some(BigDecimal(1000.20))).copy(period = None)
          getWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Check your details are correct"))
              document.getElementById("address-line-1").text() must be("addr1")
              document.getElementById("address-line-2").text() must be("addr2")
              document.getElementById("address-line-3").text() must be("addr3")
              document.getElementById("address-line-4").text() must be("addr4")
              document.getElementById("address-postcode").text() must be("123456")
              document.select("#property-details > div:nth-child(1) > dd.govuk-summary-list__actions > a").attr("href") must include("/ated/liability/create/address/edit-summary/1")
              document.select("#property-details > div:nth-child(2) > dd.govuk-summary-list__actions > a").attr("href") must include("/ated/liability/create/title/edit/1")
              document.select("#value-purpose-ated-0 > div > dd.govuk-summary-list__actions > a").attr("href") must include("/ated/liability/create/owned-before/edit-summary/1")
              document.select("#professionally-valued-incomplete > div > dd.govuk-summary-list__actions > a").attr("href") must include("/ated/liability/create/valued/edit/1")
              document.select("#dates-of-liability-incomplete > div > dd.govuk-summary-list__actions > a").attr("href") must include("/ated/liability/create/full-tax-period/edit-summary/1")
              document.select("#avoidance-scheme-incomplete > div:nth-child(1) > dd.govuk-summary-list__actions > a").attr("href") must include("/ated/liability/create/tax-avoidance/edit-summary/1")
              document.select("#supporting-information > div > dd.govuk-summary-list__actions > a").attr("href") must include("/ated/liability/create/supporting-info/edit-summary/1")
              document.getElementById("print-friendly-liability-link").attr("href") must include("/ated/liability/create/summary/1/print")
              document.getElementById("save-as-draft").attr("href") must include("/ated/account-summary")
              document.getElementById("delete-draft").attr("href") must include("/ated/liability/delete/draft/1/2019")
          }
        }
      }
    }

    "submit" must {
      "redirect to declaration page" in new Setup {
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/create/declaration/1"))
        }
      }
    }

    "print friendly view" must {
      "called for authorised user" must {

        "return status OK" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
          getPrintFriendlyWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Check your details are correct")
              document.getElementById("property-details-summary-header").text() must be("Chargeable return for ACME Limited")
          }
        }
      }
    }

    "delete the draft redirect to delete confirmation page" in new Setup {
      getWithDeleteDraftLink { result =>
        status(result) must be(SEE_OTHER)
      }
    }
  }
}
