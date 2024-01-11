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

package controllers.editLiability

import java.util.UUID
import builders._
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.{BankDetailsModel, DisposeLiabilityReturn, HasBankDetails}
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
import services.{DisposeLiabilityReturnService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.editLiability.disposeLiabilityHasBankDetails

import scala.concurrent.Future

class DisposeLiabilityHasBankDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite
  with MockitoSugar with MockAuthUtil with BeforeAndAfterEach {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDisposeLiabilityReturnService: DisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDisposeLiabilityBankDetailsController: DisposeLiabilityBankDetailsController = mock[DisposeLiabilityBankDetailsController]
  val mockDisposeLiabilitySummaryController: DisposeLiabilitySummaryController = mock[DisposeLiabilitySummaryController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: disposeLiabilityHasBankDetails = app.injector.instanceOf[views.html.editLiability.disposeLiabilityHasBankDetails]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testDisposeLiabilityHasBankDetailsController: DisposeLiabilityHasBankDetailsController = new DisposeLiabilityHasBankDetailsController (
      mockMcc,
      mockDisposeLiabilityReturnService,
      mockAuthAction,
      mockDisposeLiabilityBankDetailsController,
      mockDisposeLiabilitySummaryController,
      mockServiceInfoService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def viewWithAuthorisedUser(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any): Unit = {
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(ArgumentMatchers.eq(oldFormBundleNum))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("http://backlink")))
      val result = testDisposeLiabilityHasBankDetailsController.view(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def editFromSummary(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(ArgumentMatchers.eq(oldFormBundleNum))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
      val result = testDisposeLiabilityHasBankDetailsController.editFromSummary(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithAuthorisedUser(oldFormBundleNum: String, inputJson: JsValue, disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)
                              (test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.cacheDisposeLiabilityReturnHasBankDetails(
        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(disposeLiabilityReturn))
      when(mockDisposeLiabilityReturnService.calculateDraftDisposal(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(disposeLiabilityReturn))
      val result = testDisposeLiabilityHasBankDetailsController.save(oldFormBundleNum)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockDisposeLiabilityReturnService)
  }

  val oldFormBundleNum: String = "123456789012"

  "DisposeLiabilityHasBankDetailsController" must {

    "view" must {

      "return a status of OK, when that liability return is found in cache or ETMP" in new Setup {
        val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
          .generateDisposeLiabilityReturn("12345678901")
          .copy(bankDetails = Some(BankDetailsModel(hasBankDetails = false)))
        viewWithAuthorisedUser(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val doc = Jsoup.parse(contentAsString(result))
            doc.title() must be("Do you have a bank account where we could pay a refund? - Submit and view your ATED returns - GOV.UK")
            doc.getElementsByClass("govuk-caption-xl").text() must be("This section is Change return")
            doc.getElementsByTag("h1").text() must include("Do you have a bank account where we could pay a refund?")
            assert(doc.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
            doc.getElementsByClass("govuk-back-link").text must be("Back")
            doc.getElementsByClass("govuk-back-link").attr("href") must include("http://backlink")
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in new Setup {
        viewWithAuthorisedUser(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "editFromSummary" must {
      "return a status of OK and have the back link set to the summary page" in new Setup {
        val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
          .generateDisposeLiabilityReturn("12345678901")
          .copy(bankDetails = Some(BankDetailsModel(hasBankDetails = false)))
        editFromSummary(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Do you have a bank account where we could pay a refund?"))

            document.getElementsByClass("govuk-back-link").text must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/liability/123456789012/dispose/summary")
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in new Setup {
        editFromSummary(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "save" must {

      "for invalid data, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse( """{"hasBankDetails": "2"}""")
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnHasBankDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "for valid, redirect to liability summary page if we have bank details" in new Setup {
        val bankDetails: HasBankDetails = HasBankDetails(Some(true))
        val inputJson: JsValue = Json.toJson(bankDetails)
        val disposeLiabilityReturn: Option[DisposeLiabilityReturn] = Some(DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("123456789012"))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson, disposeLiabilityReturn) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/123456789012/dispose/bank-details"))
            verify(mockDisposeLiabilityReturnService, times(1))
              .cacheDisposeLiabilityReturnHasBankDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "for valid, redirect to liability summary page if we have no bank details" in new Setup {
        val bankDetails: HasBankDetails = HasBankDetails(Some(false))
        val inputJson: JsValue = Json.toJson(bankDetails)
        val disposeLiabilityReturn: Option[DisposeLiabilityReturn] = Some(DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("123456789012"))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson, disposeLiabilityReturn) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/123456789012/dispose/summary"))
            verify(mockDisposeLiabilityReturnService, times(1))
              .cacheDisposeLiabilityReturnHasBankDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
      "for invalid, redirect to account summary page" in new Setup {
        val bankDetails: HasBankDetails = HasBankDetails(Some(true))
        val inputJson: JsValue = Json.toJson(bankDetails)
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson, None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
            verify(mockDisposeLiabilityReturnService, times(1))
              .cacheDisposeLiabilityReturnHasBankDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }
  }
}