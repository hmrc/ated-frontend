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

import builders._
import config.ApplicationConfig
import controllers.auth.AuthAction
import models.{BankDetailsModel, DisposeLiabilityReturn}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.OK
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContentAsFormUrlEncoded, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{BackLinkCacheService, DataCacheService, DisposeLiabilityReturnService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class DisposeLiabilityHasUkBankAccountControllerSpec
  extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar
    with MockAuthUtil with BeforeAndAfterEach {

  given mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  given hc: HeaderCarrier = HeaderCarrier()
  private val mockMcc = app.injector.instanceOf[MessagesControllerComponents]
  private val mockDisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  private val mockDataCacheService = mock[DataCacheService]
  private val mockBackLinkCacheService = mock[BackLinkCacheService]
  private val mockUkBankController = mock[DisposeLiabilityUkBankDetailsController]
  private val mockNonUkBankController = mock[DisposeLiabilityNonUkBankDetailsController]
  private val mockServiceInfoService = mock[ServiceInfoService]
  private val messagesApi = app.injector.instanceOf[MessagesApi]
  private val btaNavigationLinksView = app.injector.instanceOf[BtaNavigationLinks]
  private val injectedView = app.injector.instanceOf[views.html.editLiability.disposeLiabilityHasUkBankAccount]
  private val oldFormBundleNum = "123456789012"
  given messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)

  override def beforeEach(): Unit = {
    reset(mockDisposeLiabilityReturnService)
  }

  class Setup {

    val mockAuthAction = new AuthAction(mockAppConfig, mockDelegationService, mockAuthConnector)
    val controller = new DisposeLiabilityHasUkBankAccountController(
      mockMcc,
      mockDisposeLiabilityReturnService,
      mockAuthAction,
      mockUkBankController,
      mockNonUkBankController,
      mockServiceInfoService,
      mockDataCacheService,
      mockBackLinkCacheService,
      injectedView
    )

    private def commonMocks(disposeReturn: Option[DisposeLiabilityReturn]): Unit = {
      when(mockServiceInfoService.getPartial(using any(), any(), any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))(using any(), any()))
        .thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(ArgumentMatchers.eq(oldFormBundleNum))(using any(), any()))
        .thenReturn(Future.successful(disposeReturn))
    }

    def viewWithAuthorisedUser(disposeReturn: Option[DisposeLiabilityReturn])(test: Future[Result] => Any): Unit = {
      setAuthMocks(authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet))
      commonMocks(disposeReturn)
      when(mockBackLinkCacheService.fetchAndGetBackLink(any())(using any()))
        .thenReturn(Future.successful(Some("http://backlink")))
      val result = controller.view(oldFormBundleNum)
        .apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def editFromSummary(disposeReturn: Option[DisposeLiabilityReturn])(test: Future[Result] => Any): Unit = {
      setAuthMocks(authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet))
      commonMocks(disposeReturn)
      val result = controller.editFromSummary(oldFormBundleNum)
        .apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithAuthorisedFormUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], disposeReturn: Option[DisposeLiabilityReturn] = None)
                              (test: Future[Result] => Any): Unit = {
      setAuthMocks(authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet))
      when(mockServiceInfoService.getPartial(using any(), any(), any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))(using any(), any()))
        .thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.cacheDisposeLiabilityReturnHasBankDetails(any(), any())(using any(), any()))
        .thenReturn(Future.successful(disposeReturn))
      when(mockDisposeLiabilityReturnService.cacheDisposeLiabilityReturnHasUkBankAccount(any(), any())(using any(), any()))
        .thenReturn(Future.successful(disposeReturn))
      when(mockDisposeLiabilityReturnService.calculateDraftDisposal(any())(using any(), any()))
        .thenReturn(Future.successful(disposeReturn))
      val request = SessionBuilder.updateRequestFormWithSession(fakeRequest, userId)
      val result = controller.save(oldFormBundleNum)(request)
      test(result)
    }
  }

  "DisposeLiabilityHasBankDetailsController" must {

    "view" should {

      "return OK when liability return is found" in new Setup {
        val returnData: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
          .generateDisposeLiabilityReturn("12345678901")
          .copy(bankDetails = Some(BankDetailsModel(hasBankDetails = false)))

        viewWithAuthorisedUser(Some(returnData)) { result =>
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include("Are you using a UK bank account?")
        }
      }

      "redirect to account summary if no liability return" in new Setup {
        viewWithAuthorisedUser(None) { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/ated/account-summary")
        }
      }

    }

    "editFromSummary" should {

      "return OK and set backlink to summary" in new Setup {
        val returnData: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
          .generateDisposeLiabilityReturn("12345678901")
          .copy(bankDetails = Some(BankDetailsModel(hasBankDetails = false)))

        editFromSummary(Some(returnData)) { result =>
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementsByClass("govuk-back-link").attr("href") must include("/summary")
        }
      }

      "redirect if no liability return found" in new Setup {
        editFromSummary(None) { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/ated/account-summary")
        }
      }
    }

    "save" should {

      "return BAD_REQUEST for invalid data" in new Setup {
        when(mockBackLinkCacheService.fetchAndGetBackLink(any())(using any())).thenReturn(Future.successful(None))
        saveWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "hasUkBankAccount" -> "0")
        ) { result =>
          status(result) mustBe BAD_REQUEST
          verify(mockDisposeLiabilityReturnService, never())
            .calculateDraftDisposal(any())(using any(), any())
        }
      }

      "redirect to UK bank details page if user has a UK bank account" in new Setup {
        val returnData: Some[DisposeLiabilityReturn] = Some(DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("123456789012"))
        when(mockBackLinkCacheService.saveBackLink(any(), any())(using any()))
          .thenReturn(Future.successful(None))
        saveWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "hasUkBankAccount" -> "true"),
          returnData
        ) { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/ated/liability/123456789012/dispose/uk-bank-details")
        }
      }

      "redirect to non-UK bank details page if user does not have a UK bank account" in new Setup {
        val returnData: Some[DisposeLiabilityReturn] = Some(DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("123456789012"))
        when(mockBackLinkCacheService.saveBackLink(any(), any())(using any()))
          .thenReturn(Future.successful(None))
        saveWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "hasUkBankAccount" -> "false"),
          returnData
        ) { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/ated/liability/123456789012/dispose/non-uk-bank-details")
        }
      }

      "redirect to UK bank account details page if dispose liability is not returned for a user with UK bank account" in new Setup {
        when(mockBackLinkCacheService.saveBackLink(any(), any())(using any()))
          .thenReturn(Future.successful(None))
        saveWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "hasUkBankAccount" -> "true")
        ) { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/ated/liability/123456789012/dispose/uk-bank-details")
        }
      }
    }
  }
}