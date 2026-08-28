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

package controllers.editLiability

import java.util.UUID
import builders._
import config.ApplicationConfig
import controllers.auth.AuthAction
import models.{BankDetailsModel, PropertyDetails}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContentAsFormUrlEncoded, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{BackLinkCacheService, ChangeLiabilityReturnService, DataCacheService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.editLiability.hasBankDetails

import scala.concurrent.Future

class HasBankDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  given mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  given hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockChangeLiabilityReturnService: ChangeLiabilityReturnService = mock[ChangeLiabilityReturnService]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockBackLinkCacheService: BackLinkCacheService = mock[BackLinkCacheService]
  val mockBankDetailsController: HasUkBankAccountController = mock[HasUkBankAccountController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  given messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: hasBankDetails = app.injector.instanceOf[views.html.editLiability.hasBankDetails]

  class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testHasBankDetailsController: HasBankDetailsController = new HasBankDetailsController (
    mockMcc,
    mockChangeLiabilityReturnService,
    mockAuthAction,
    mockBankDetailsController,
    mockServiceInfoService,
    mockDataCacheService,
    mockBackLinkCacheService,
    injectedViewInstance
  )

  def viewWithAuthorisedUser(changeLiabilityReturnOpt: Option[PropertyDetails])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockServiceInfoService.getPartial(using ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
    when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
    (ArgumentMatchers.eq("12345678901"), ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(changeLiabilityReturnOpt))
    when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(using ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some("http://backlink")))
    val result = testHasBankDetailsController.view("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editFromSummary(changeLiabilityReturnOpt: Option[PropertyDetails])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockServiceInfoService.getPartial(using ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
    when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
    (ArgumentMatchers.eq("12345678901"), ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(changeLiabilityReturnOpt))
    when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(using ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testHasBankDetailsController.editFromSummary("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithAuthorisedFormUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockServiceInfoService.getPartial(using ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
    val changeLiabilityReturn = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("123456789012")
    when(mockChangeLiabilityReturnService.cacheChangeLiabilityReturnHasBankDetails
    (ArgumentMatchers.eq("12345678901"), ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(changeLiabilityReturn)))
    val result = testHasBankDetailsController.save("12345678901").apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))
    test(result)
  }
}

  override def beforeEach(): Unit = {
    reset(mockChangeLiabilityReturnService)
  }

  "HasBankDetailsController" must {
    "view - for authorised users" must {

      "navigate to bank details page, if liability is retrieved" in new Setup {
        val bankDetails: BankDetailsModel = BankDetailsModel(hasBankDetails = false)
        val changeLiabilityReturn: PropertyDetails = ChangeLiabilityReturnBuilder
          .generateChangeLiabilityReturn("12345678901").copy(bankDetails = Some(bankDetails))
        viewWithAuthorisedUser(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Do you have a bank account where we could pay a refund? - Submit and view your ATED returns - GOV.UK")
            document.getElementsByClass("govuk-caption-xl").text() must include("This section is: Change return")
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
            document.getElementsByClass("govuk-back-link").text must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must include("http://backlink")
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in new Setup {
        when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        viewWithAuthorisedUser(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "editFromSummary - for authorised users" must {

      "navigate to bank details page, if liability is retrieved, show the summary back link" in new Setup {
        val bankDetails: BankDetailsModel = BankDetailsModel(hasBankDetails = false)
        val changeLiabilityReturn: PropertyDetails = ChangeLiabilityReturnBuilder
          .generateChangeLiabilityReturn("12345678901").copy(bankDetails = Some(bankDetails))
        editFromSummary(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Do you have a bank account where we could pay a refund? - Submit and view your ATED returns - GOV.UK")

            document.getElementsByClass("govuk-back-link").text must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/liability/12345678901/change/summary")
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in new Setup {
        when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        editFromSummary(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "save - for authorised user" must {
      "for invalid data, return BAD_REQUEST" in new Setup {
        when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(using ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "hasBankDetails" -> "2")
        ) { result =>
             status(result) must be(BAD_REQUEST)
             verify(mockChangeLiabilityReturnService, times(0))
              .cacheChangeLiabilityReturnHasBankDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "for valid, redirect to change in value page" in new Setup {
        when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        saveWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "hasBankDetails" -> "true")
        ) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/has-uk-bank-account"))
            verify(mockChangeLiabilityReturnService, times(1))
              .cacheChangeLiabilityReturnHasBankDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "for valid, redirect to change in value page if we have no bank details" in new Setup {
        when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        saveWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "hasBankDetails" -> "false")
        ) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/view-summary"))
            verify(mockChangeLiabilityReturnService, times(1))
              .cacheChangeLiabilityReturnHasBankDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }
  }
}