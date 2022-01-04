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

import builders._
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DisposeLiabilityReturnService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class DisposeLiabilityDeclarationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDisposeLiabilityReturnService: DisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.editLiability.disposeLiabilityDeclaration]

  val oldFormBundleNum = "123456789012"
  val newFormBundleNum = "123456789011"

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testDisposeLiabilityDeclarationController: DisposeLiabilityDeclarationController = new DisposeLiabilityDeclarationController (
    mockMcc,
    mockDisposeLiabilityReturnService,
    mockAuthAction,
    mockServiceInfoService,
    mockDataCacheConnector,
    mockBackLinkCacheConnector,
    injectedViewInstance
  )

  def viewWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    noDelegationModelAuthMocks(authMock)
    when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testDisposeLiabilityDeclarationController.view(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedDelegatedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Agent, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testDisposeLiabilityDeclarationController.view(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSessionDelegation(userId))
    test(result)
  }


  def submitWithAuthorisedUser(a: Option[DisposeLiabilityReturn], oldForBundle: String = oldFormBundleNum)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(ArgumentMatchers.eq(oldFormBundleNum))
    (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(a))
    val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = oldForBundle, formBundleNumber = Some
    (newFormBundleNum), liabilityAmount = BigDecimal(3500.00), amountDueOrRefund = BigDecimal(0.00), paymentReference = Some("payment-ref-1"))
    val response = EditLiabilityReturnsResponseModel(processingDate = DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
    when(mockDisposeLiabilityReturnService.submitDraftDisposeLiability(ArgumentMatchers.eq(oldFormBundleNum))
    (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))
    val result = testDisposeLiabilityDeclarationController.submit(oldFormBundleNum)
      .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
    test(result)
  }
}

  "DisposeLiabilityDeclarationController" must {

    "view" must {

      "take user to dispose declaration page" in new Setup {
        viewWithAuthorisedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title must be("Amended return declaration - GOV.UK")
          assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
          document.getElementById("dispose-liability-declaration-confirmation-header").text() must be("Amended return declaration")
          document.getElementById("dispose-liability-declaration-before-declaration-text")
            .text() must be("Before you can submit your return to HMRC you must read and agree to the following statement. " +
            "If you give false information you may have to pay financial penalties and face prosecution.")
          document.getElementById("declaration-confirmation-text")
            .text() must be("I declare that the information I have given on this return is correct and complete.")
          document.getElementById("submit").text() must be("Agree and submit amended return")
        }
      }

      "take a delegated user providing a delegation model to dispose declaration page" in new Setup {
        viewWithAuthorisedDelegatedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title must be("Amended return declaration - GOV.UK")
          document.getElementById("dispose-liability-declaration-confirmation-header").text() must be("Amended return declaration")
          document.getElementById("dispose-liability-declaration-before-declaration-text")
            .text() must be("Before your client’s return can be submitted to HMRC, you must read and agree to the following statement." +
            " Your client’s approval may be in electronic or non-electronic form. If your client gives false information, " +
            "they may have to pay financial penalties and face prosecution.")
          document.getElementById("declaration-confirmation-text")
            .text() must be("I confirm that my client has approved the information contained in this return as being correct and " +
            "complete to the best of their knowledge and belief.")
          document.getElementById("submit").text() must be("Agree and submit amended return")
        }
      }

    }

    "submit" must {

      "for valid data, redirect to respective return sent page, if form-bundle found in response" in new Setup {
        val cL1: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn(oldFormBundleNum)
        val calc1: DisposeCalculated = DisposeLiabilityReturnBuilder.generateCalculated
        val cL2: DisposeLiabilityReturn = cL1.copy(calculated = Some(calc1))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser(Some(cL2)) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/ated/liability/$oldFormBundleNum/dispose/sent")
        }
      }

      "for valid data, redirect to account summary page, if form-bundle not-found in response" in new Setup {
        val cL1: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn(oldFormBundleNum)
        val calc1: DisposeCalculated = DisposeLiabilityReturnBuilder.generateCalculated
        val cL2: DisposeLiabilityReturn = cL1.copy(calculated = Some(calc1))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser(Some(cL2), newFormBundleNum) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/ated/account-summary")
        }
      }

    }
  }
}
