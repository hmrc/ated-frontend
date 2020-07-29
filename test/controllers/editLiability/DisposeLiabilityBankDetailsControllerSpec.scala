/*
 * Copyright 2020 HM Revenue & Customs
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
import services.{DisposeLiabilityReturnService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class DisposeLiabilityBankDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDisposeLiabilityReturnService: DisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDisposeLiabilitySummaryController: DisposeLiabilitySummaryController = mock[DisposeLiabilitySummaryController]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.editLiability.disposeLiabilityBankDetails]

  override def beforeEach: Unit = {
  }

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testDisposeLiabilityBankDetailsController : DisposeLiabilityBankDetailsController = new DisposeLiabilityBankDetailsController(
      mockMcc,
      mockDisposeLiabilityReturnService,
      mockAuthAction,
      mockDisposeLiabilitySummaryController,
      mockServiceInfoService,
      mockDataCacheConnector,
      mockBackLinkCache,
      injectedViewInstance
    )


    def viewWithAuthorisedUser(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn
      (ArgumentMatchers.eq(oldFormBundleNum))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
      when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testDisposeLiabilityBankDetailsController.view(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithAuthorisedUser(oldFormBundleNum: String, inputJson: JsValue)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val disposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("123456789012")
      when(mockDisposeLiabilityReturnService.cacheDisposeLiabilityReturnBank
      (ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(disposeLiabilityReturn)))
      val result = testDisposeLiabilityBankDetailsController.save(oldFormBundleNum)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }

    def editFromSummary(disposeLiabilityReturn: Option[DisposeLiabilityReturn])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn
      (ArgumentMatchers.eq(oldFormBundleNum))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
      when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testDisposeLiabilityBankDetailsController.editFromSummary(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

  }

  val oldFormBundleNum = "123456789012"

  "DisposeLiabilityBankDetailsController" must {

    "view" must {

      "return a status of OK, when that liability return is found in cache or ETMP" in new Setup {
        val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
          .generateDisposeLiabilityReturn("12345678901")
          .copy(bankDetails = Some(BankDetailsModel(hasBankDetails = false)))
        viewWithAuthorisedUser(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val doc = Jsoup.parse(contentAsString(result))
            doc.title() must be("Is the bank account in the UK? - GOV.UK")
            doc.getElementById("pre-heading").text() must be("This section is: Change return")
            assert(doc.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
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

    "editFromSummary - for authorised users" must {

      "navigate to bank details page, if liablity is retrieved, show the summary back link" in new Setup {
        val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
          .generateDisposeLiabilityReturn("12345678901")
          .copy(bankDetails = Some(BankDetailsModel(hasBankDetails = false)))
        editFromSummary(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Is the bank account in the UK?"))

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/liability/123456789012/dispose/summary")
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        editFromSummary(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "save" must {

      "for invalid data, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "30"}""".stripMargin)
        when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0)).cacheDisposeLiabilityReturnBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "for valid, redirect to liability summary page" in new Setup {
        val bankDetails = BankDetails(Some(true), Some("ACCOUNTNAME"), Some("123456567890"), Some(SortCode("11", "22", "33")))
        val inputJson: JsValue = Json.toJson(bankDetails)
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/123456789012/dispose/summary"))
            verify(mockDisposeLiabilityReturnService, times(1)).cacheDisposeLiabilityReturnBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

    }
  }
}
