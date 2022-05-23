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

package controllers.editLiability

import java.util.UUID

import builders.{PropertyDetailsBuilder, SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.{PropertyDetails, PropertyDetailsCalculated}
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
import services.{PropertyDetailsService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class EditLiabilitySummaryControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.editLiability.editLiabilitySummary]

  val organisationName: String = "ACME Limited"

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testEditLiabilitySummaryController: EditLiabilitySummaryController = new EditLiabilitySummaryController (
   mockMcc,
   mockPropertyDetailsService,
   mockSubscriptionDataService,
   mockAuthAction,
   mockServiceInfoService,
   mockDataCacheConnector,
   mockBackLinkCacheConnector,
    injectedViewInstance
  )

  def viewWithAuthorisedUser(propertyDetails: Option[PropertyDetails])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockPropertyDetailsService.calculateDraftChangeLiability(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(propertyDetails))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testEditLiabilitySummaryController.view("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewSummaryWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockPropertyDetailsService.calculateDraftChangeLiability(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(propertyDetails)))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testEditLiabilitySummaryController.viewSummary("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAndGetFormData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = testEditLiabilitySummaryController.submit("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getPrintFriendlyWithAuthorisedUser(propertyDetails: Option[PropertyDetails])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockPropertyDetailsService.calculateDraftChangeLiability(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(propertyDetails))
    when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))
    val result = testEditLiabilitySummaryController.viewPrintFriendlyEditLiabilityReturn("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}

  "EditLiabilitySummaryController" must {
    "view for authorised user" must {

      "show the edit liability summary page, if the return is found in cache is greater than zero" in new Setup {
        val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails("12345678901").
          copy(calculated = Some(PropertyDetailsCalculated(amountDueOrRefund = Some(BigDecimal(123.45)))))
        viewWithAuthorisedUser(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Check your details are correct"))
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
        }
      }

      "Redirect to the Account Summary Page if calculated is None" in new Setup {
        val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails("12345678901").copy(calculated = None)
        viewWithAuthorisedUser(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/account-summary")
        }
      }

      "Redirect to the Bank Details Page if the Amount Due < 0" in new Setup {
        val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails("12345678901").
          copy(calculated = Some(PropertyDetailsCalculated(amountDueOrRefund = Some(BigDecimal(-123.34)))))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        viewWithAuthorisedUser(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/12345678901/change/has-bank-details")
        }
      }

      "return to edit liability summary page, if the return is found in cache but calculated is equal to zero" in new Setup {
        pending
        val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails("12345678901").
          copy(calculated = Some(PropertyDetailsCalculated(amountDueOrRefund = Some(BigDecimal(0.00)))))
        viewWithAuthorisedUser(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Check your details are correct"))
            document.getElementById("address-line-1").text() must be("addr1")
            document.getElementById("address-line-2").text() must be("addr2")
            document.getElementById("address-line-3").text() must be("addr3")
            document.getElementById("address-line-4").text() must be("addr4")
        }
      }

      "redirect to edit liability summary page if the return calculation fails (due to incomplete data)" in new Setup {
        viewWithAuthorisedUser(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be( Some("/ated/liability/create/summary/12345678901"))
        }
      }
    }

    "viewSummary for authorised user" must {

      "view the edit liability summary page, if the return is found in cache is greater than zero" in new Setup {
        val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails("12345678901")
        viewSummaryWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Check your details are correct"))
        }
      }

      "view the edit liability summary page, if the return is found in cache is < 0" in new Setup {
        val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails("12345678901").
          copy(calculated = Some(PropertyDetailsCalculated(amountDueOrRefund = Some(BigDecimal(-123.34)))))
        viewSummaryWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Check your details are correct"))
        }
      }
    }

    "submit - for authorised users" must {
      "redirect to return declaration page" in new Setup {
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/declaration"))
        }
      }
    }

    "print friendly view" when {

      "called for authorised user" must {

        "return status OK when liability is in cache" in new Setup {
          val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails("12345678901")
          getPrintFriendlyWithAuthorisedUser(Some(changeLiabilityReturn)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be ("Check your details are correct")
              document.getElementById("edit-liability-summary-header").text() must be("Further return for ACME Limited")
          }
        }
      }
    }
  }
}
