/*
 * Copyright 2025 HM Revenue & Customs
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
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.OK
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{BackLinkCacheService, DataCacheService, DisposeLiabilityReturnService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class DisposeLiabilityUkBankDetailsControllerSpec
  extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar
    with MockAuthUtil with BeforeAndAfterEach {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  private val mockMcc = app.injector.instanceOf[MessagesControllerComponents]
  private val mockDisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  private val mockDataCacheService = mock[DataCacheService]
  private val mockBackLinkCacheService = mock[BackLinkCacheService]
  val mockDisposeLiabilitySummaryController: DisposeLiabilitySummaryController = mock[DisposeLiabilitySummaryController]
  private val mockServiceInfoService = mock[ServiceInfoService]
  private val messagesApi = app.injector.instanceOf[MessagesApi]
  private val btaNavigationLinksView = app.injector.instanceOf[BtaNavigationLinks]
  private val injectedView = app.injector.instanceOf[views.html.editLiability.disposeLiabilityUkBankDetails]
  private val oldFormBundleNum = "123456789012"
  implicit lazy val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)

  override def beforeEach(): Unit = {
    reset(mockDisposeLiabilityReturnService)
  }

  class Setup {

    val mockAuthAction = new AuthAction(mockAppConfig, mockDelegationService, mockAuthConnector)
    val controller = new DisposeLiabilityUkBankDetailsController(
      mockMcc,
      mockDisposeLiabilityReturnService,
      mockAuthAction,
      mockDisposeLiabilitySummaryController,
      mockServiceInfoService,
      mockDataCacheService,
      mockBackLinkCacheService,
      injectedView
    )

    private def commonMocks(disposeReturn: Option[DisposeLiabilityReturn]): Unit = {
      when(mockServiceInfoService.getPartial(any(), any(), any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))(any(), any()))
        .thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(ArgumentMatchers.eq(oldFormBundleNum))(any(), any()))
        .thenReturn(Future.successful(disposeReturn))
    }

    def viewWithAuthorisedUser(disposeReturn: Option[DisposeLiabilityReturn])(test: Future[Result] => Any): Unit = {
      setAuthMocks(authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet))
      commonMocks(disposeReturn)
      when(mockBackLinkCacheService.fetchAndGetBackLink(any())(any()))
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

    def saveWithAuthorisedUser(inputJson: JsValue, disposeReturn: Option[DisposeLiabilityReturn] = None)
                              (test: Future[Result] => Any): Unit = {
      setAuthMocks(authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet))
      when(mockServiceInfoService.getPartial(any(), any(), any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))(any(), any()))
        .thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.cacheDisposeLiabilityReturnBank(any(), any())(any(), any()))
        .thenReturn(Future.successful(disposeReturn))
      when(mockDisposeLiabilityReturnService.calculateDraftDisposal(any())(any(), any()))
        .thenReturn(Future.successful(disposeReturn))
      val request = SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId)
      val result = controller.save(oldFormBundleNum)(request)
      test(result)
    }
  }

  "DisposeLiabilityUkBankDetailsController" must {
    "view" should {

      "return OK when liability return is found" in new Setup {
        val returnData: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
          .generateDisposeLiabilityReturn("12345678901")
          .copy(bankDetails = Some(BankDetailsModel(hasBankDetails = false)))

        viewWithAuthorisedUser(Some(returnData)) { result =>
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include("Enter your bank or building society account details")
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
        val invalidJson: JsValue = Json.parse("""{"hasUkBankAccount": "0"}""")
        when(mockBackLinkCacheService.fetchAndGetBackLink(any())(any())).thenReturn(Future.successful(None))

        saveWithAuthorisedUser(invalidJson) { result =>
          status(result) mustBe BAD_REQUEST
          verify(mockDisposeLiabilityReturnService, never())
            .calculateDraftDisposal(any())(any(), any())
        }
      }

      "redirect to summary page on successful UK bank form submission" in new Setup {

        val inputJson: JsValue = Json.parse(
          """{
            |"hasUKBankAccount": true,
            |"accountName": "ACCOUNTNAME",
            |"accountNumber": "123456567890",
            |"sortCode": "112233"
            |}""".stripMargin)
        val returnData: Some[DisposeLiabilityReturn] = Some(DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("123456789012"))
        when(mockBackLinkCacheService.saveBackLink(any(), any())(any()))
          .thenReturn(Future.successful(None))

        saveWithAuthorisedUser(inputJson, returnData) { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/ated/liability/123456789012/dispose/summary")
        }
      }
    }
  }

}
