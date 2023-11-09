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

import builders.{SessionBuilder, TitleBuilder, _}
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
import services.{ChangeLiabilityReturnService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class EditLiabilityDeclarationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockChangeLiabilityReturnService: ChangeLiabilityReturnService = mock[ChangeLiabilityReturnService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val injectedViewInstance = app.injector.instanceOf[views.html.editLiability.editLiabilityDeclaration]

  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]

  val formBundleNo2: String = "123456789011"
  val formBundleNo1: String = "123456789012"

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testEditLiabilityDeclarationController: EditLiabilityDeclarationController = new EditLiabilityDeclarationController(
      mockMcc,
      mockChangeLiabilityReturnService,
      mockAuthAction,
      mockServiceInfoService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def viewWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testEditLiabilityDeclarationController.view(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedUser(x: Option[PropertyDetails] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      noDelegationModelAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
      (ArgumentMatchers.eq(formBundleNo1), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(x))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testEditLiabilityDeclarationController.view(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedDelegatedUser(x: Option[PropertyDetails] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Agent, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
      (ArgumentMatchers.eq(formBundleNo1), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(x))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testEditLiabilityDeclarationController.view(formBundleNo1).apply(SessionBuilder.buildRequestWithSessionDelegation(userId))
      test(result)
    }

    def submitWithAuthorisedUser(a: Option[PropertyDetails], oldForBundle: String = formBundleNo1)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
      (ArgumentMatchers.eq(formBundleNo1), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(a))
      val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = oldForBundle, formBundleNumber =
        Some(formBundleNo2), liabilityAmount = BigDecimal(3500.00), amountDueOrRefund = BigDecimal(0.00), paymentReference = Some("payment-ref-1"))
      val response = EditLiabilityReturnsResponseModel(processingDate = DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      when(mockChangeLiabilityReturnService.submitDraftChangeLiability
      (ArgumentMatchers.eq(formBundleNo1))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))
      val result = testEditLiabilityDeclarationController.submit(formBundleNo1)
        .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
      test(result)
    }

    def submitWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testEditLiabilityDeclarationController.submit(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {

    reset(mockChangeLiabilityReturnService)
    reset(mockDelegationService)
    reset(mockDataCacheConnector)
    reset(mockBackLinkCacheConnector)
  }

  "view" must {
    "unauthorised users" must {

      "respond with a redirect" in new Setup {
        viewWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }
      "be redirected to the login page" in new Setup {
        viewWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }

    "authorised users" must {

      "view amended return declaration, if amountDueOrRefund is negative" in new Setup {
        val cL1: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
        val calc1: PropertyDetailsCalculated = ChangeLiabilityReturnBuilder.generateCalculated
        val cL2: PropertyDetails = cL1.copy(calculated = Some(calc1))
        viewWithAuthorisedUser(Some(cL2)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("Amended return declaration"))
          document.getElementsByClass("govuk-heading-xl").text() must include("Amended return declaration")
          document.getElementById("edit-declaration-before-declaration-text")
            .text() must be("! Warning Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
          document.getElementById("edit-liability-client")
            .text() must be("I declare that the information I have given on this return is correct and complete.")
          document.getElementById("submit").text() must be("Agree and submit amended return")
        }
      }

      "view further return declaration, if amountDueOrRefund is positive" in new Setup {
        val cL1: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
        val calc1: PropertyDetailsCalculated = ChangeLiabilityReturnBuilder.generateCalculated.copy(amountDueOrRefund = Some(BigDecimal(500.00)))
        val cL2: PropertyDetails = cL1.copy(calculated = Some(calc1))
        viewWithAuthorisedUser(Some(cL2)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("Further return declaration"))
          document.getElementsByClass("govuk-heading-xl").text() must include("Further return declaration")
          document.getElementById("edit-declaration-before-declaration-text")
            .text() must be("! Warning Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
          document.getElementById("edit-liability-client")
            .text() must be("I declare that the information I have given on this return is correct and complete.")
          document.getElementById("submit").text() must be("Agree and submit further return")
        }
      }

      "view change in details return declaration, if amountDueOrRefund is zero" in new Setup {
        val cL1: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
        val calc1: PropertyDetailsCalculated = ChangeLiabilityReturnBuilder.generateCalculated.copy(amountDueOrRefund = Some(BigDecimal(0.00)))
        val cL2: PropertyDetails = cL1.copy(calculated = Some(calc1))
        viewWithAuthorisedUser(Some(cL2)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("Change in details declaration"))
          document.getElementsByClass("govuk-heading-xl").text() must include("Change in details declaration")
          document.getElementById("edit-declaration-before-declaration-text")
            .text() must be("! Warning Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
          document.getElementById("edit-liability-client")
            .text() must be("I declare that the information I have given on this return is correct and complete.")
          document.getElementById("submit").text() must be("Agree and submit")
        }
      }

      "view change in details return declaration, if amountDueOrRefund is not-defined" in new Setup {
        val cL1: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
        val calc1: PropertyDetailsCalculated = ChangeLiabilityReturnBuilder.generateCalculated.copy(amountDueOrRefund = None)
        val cL2: PropertyDetails = cL1.copy(calculated = Some(calc1))
        viewWithAuthorisedUser(Some(cL2)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("Change in details declaration"))
        }
      }

      "view return declaration, with delegated user providing a delegation model" in new Setup {
        val cL1: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
        val calc1: PropertyDetailsCalculated = ChangeLiabilityReturnBuilder.generateCalculated.copy(amountDueOrRefund = Some(BigDecimal(0.00)))
        val cL2: PropertyDetails = cL1.copy(calculated = Some(calc1))
        viewWithAuthorisedDelegatedUser(Some(cL2)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("edit-declaration-before-declaration-text")
            .text() must be("! Warning Before your client’s return can be submitted to HMRC, you must read and agree to the following statement. Your client’s approval may be in electronic or non-electronic form. If your client gives false information, they may have to pay financial penalties and face prosecution.")
          document.getElementById("edit-liability-agent")
            .text() must be("I confirm that my client has approved the information contained in this return as being correct and complete to the best of their knowledge and belief.")
          document.getElementById("submit").text() must be("Agree and submit")
        }
      }

      "redirected to account summary page, if service doesn't return liability model" in new Setup {
        viewWithAuthorisedUser(None) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }
  }

  "submit" must {
    "unauthorised users" must {
      "respond with a redirect" in new Setup {
        submitWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }
      "be redirected to the login page" in new Setup {
        submitWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }

    "Authorised users" must {
      "for valid data, redirect to respective return sent page, if form-bundle found in response" in new Setup {
        val cL1: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
        val calc1: PropertyDetailsCalculated = ChangeLiabilityReturnBuilder.generateCalculated
        val cL2: PropertyDetails = cL1.copy(calculated = Some(calc1))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser(Some(cL2)) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/ated/liability/$formBundleNo1/change/sent")
        }
      }

      "for valid data, redirect to account summary page, if form-bundle not-found in response" in new Setup {
        val cL1: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
        val calc1: PropertyDetailsCalculated = ChangeLiabilityReturnBuilder.generateCalculated
        val cL2: PropertyDetails = cL1.copy(calculated = Some(calc1))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser(Some(cL2), formBundleNo2) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/ated/account-summary")
        }
      }
    }
  }
}
