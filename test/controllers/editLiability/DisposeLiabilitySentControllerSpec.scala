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
import connectors.DataCacheConnector
import controllers.auth.AuthAction
import models.{EditLiabilityReturnsResponse, EditLiabilityReturnsResponseModel}
import java.time.format.DateTimeFormatter
import java.time.{ZonedDateTime, LocalDate}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{DisposeLiabilityReturnService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants._
import views.html.BtaNavigationLinks
import views.html.editLiability.disposeLiabilitySent

import scala.concurrent.Future

class DisposeLiabilitySentControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockDisposeLiabilityReturnService: DisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: disposeLiabilitySent = app.injector.instanceOf[views.html.editLiability.disposeLiabilitySent]

  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789011"
  val organisationName: String = "ACME Limited"

  val date: String = LocalDate.now.format(DateTimeFormatter.ofPattern("d LLLL yyyy"))

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testDisposeLiabilitySentController: DisposeLiabilitySentController = new DisposeLiabilitySentController(
      mockMcc,
      mockSubscriptionDataService,
      mockAuthAction,
      mockServiceInfoService,
      mockDataCacheConnector,
      injectedViewInstance
    )

     def viewWithAuthorisedUser(x: Option[EditLiabilityReturnsResponseModel] = None)(test: Future[Result] => Any): Any = {
       val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
       when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel]
        (ArgumentMatchers.eq(SubmitEditedLiabilityReturnsResponseFormId))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(x))
      val result = testDisposeLiabilitySentController.view(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

     def getPrintFriendlyWithAuthorisedUser(x: Option[EditLiabilityReturnsResponseModel] = None)(test: Future[Result] => Any): Any = {
       val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel]
        (ArgumentMatchers.eq(SubmitEditedLiabilityReturnsResponseFormId))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(x))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))
      val result = testDisposeLiabilitySentController.viewPrintFriendlyDisposeLiabilitySent(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {

    reset(mockSubscriptionDataService)
    reset(mockDelegationService)
    reset(mockDataCacheConnector)
  }

  val oldFormBundleNum = "123456789012"

  "DisposeLiabilitySentController.view" should {
    "return amended return sent page, if response found in cache and amountDueOrRefund is Negative" in new Setup {

      val r1: EditLiabilityReturnsResponse = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber =
        Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(-500.00), paymentReference = Some("payment-ref-01"))
      val resp: EditLiabilityReturnsResponseModel = EditLiabilityReturnsResponseModel(ZonedDateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      viewWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include(TitleBuilder.buildTitle(s"Your amended return has been successfully submitted - Annual Tax on enveloped dwellings"))
          assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
          document.getElementsByTag("h1").text() must be(s"Your amended return has been successfully submitted")
          document.getElementById("view-message")
            .text() must be("You can view your completed returns, overall balance, payment references and ways to pay in the ATED online service.")
          document.getElementById("email-message").text() must be("You will not receive an email confirmation.")
          document.getElementById("charges-heading").text() must be("Charges for this return")
          document.getElementById("new-amount")
            .text() must be("Your new adjusted amount does not reflect any payments you have already made or penalties that have been issued.")
          document.getElementById("already-paid-title").text() must be("If you have already paid your previous ATED liability")
          document.getElementById("owe-you")
            .text() must be("HMRC owe you £500 for this amended return.")
          document.getElementById("repayments")
            .text() must be("Any repayments will be paid into your nominated bank account. We will contact you if you have a non-UK bank account.")
          document.getElementById("not-paid-title")
            .text() must be("If you have not paid your previous ATED liability")
          document.getElementById("you-owe")
            .text() must be("You owe HMRC £1,235 for this amended return.")
          document.getElementById("payment-reference")
            .text() must be("The reference to make this payment is payment-ref-01.")
          document.getElementById("liable-for")
            .text() must be("If you have sold the property you may be liable for ATED-related Capital Gains Tax (opens in new tab).")
          document.getElementById("view-balance")
            .text() must be("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")
          document.getElementById("submit")
            .text() must be("Your ATED summary")
      }
    }

    "take user to print friendly dispose liability confirmation" in new Setup {
      val r1: EditLiabilityReturnsResponse = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber =
        Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(-500.00), paymentReference = Some("payment-ref-01"))
      val resp: EditLiabilityReturnsResponseModel = EditLiabilityReturnsResponseModel(ZonedDateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      getPrintFriendlyWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("header").text() must include("Your amended return has been successfully submitted")
          document.getElementById("charges-heading").text() must be("Charges for this return")
          document.getElementById("new-amount")
            .text() must be("Your new adjusted amount does not reflect any payments you have already made or penalties that have been issued.")
          document.getElementById("already-paid-title").text() must be("If you have already paid your previous ATED liability")
          document.getElementById("owe-you").text() must be("HMRC owe you £500 for this amended return.")
          document.getElementById("repayments")
            .text() must be("Any repayments will be paid into your nominated bank account. We will contact you if you have a non-UK bank account.")
          document.getElementById("not-paid-title").text() must be("If you have not paid your previous ATED liability")
          document.getElementById("you-owe")
            .text() must be("You owe HMRC £1,235 for this amended return.")
          document.getElementById("payment-reference").text() must be("The reference to make this payment is payment-ref-01.")
          document.getElementById("liable-for")
            .text() must be("If you have sold the property you may be liable for ATED-related Capital Gains Tax. Find out more by searching GOV.UK for ‘high value residential property’.")
          document.getElementById("view-balance")
            .text() must be("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")
      }
    }

    "take user to print friendly dispose liability confirmation bigger than zero" in new Setup {
      val r1: EditLiabilityReturnsResponse = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber =
        Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(500.00), paymentReference = Some("payment-ref-01"))
      val resp: EditLiabilityReturnsResponseModel = EditLiabilityReturnsResponseModel(ZonedDateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      getPrintFriendlyWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("header").text() must include("Your amended return has been successfully submitted")
      }
    }
    "take user to print friendly dispose liability confirmation exactly zero" in new Setup {
      val r1: EditLiabilityReturnsResponse = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber =
        Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(0.00), paymentReference = Some("payment-ref-01"))
      val resp: EditLiabilityReturnsResponseModel = EditLiabilityReturnsResponseModel(ZonedDateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      getPrintFriendlyWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("header").text() must include("Your amended return has been successfully submitted")
      }
    }

    "return further return sent page, if response found in cache and amountDueOrRefund is Negative" in new Setup {
      val r1: EditLiabilityReturnsResponse = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber =
        Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(500.00), paymentReference = Some("payment-ref-01"))
      val resp: EditLiabilityReturnsResponseModel = EditLiabilityReturnsResponseModel(ZonedDateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      viewWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle(s"Your amended return has been successfully submitted - Annual Tax on enveloped dwellings"))
      }
    }

    "return edit details return sent page, if response found in cache and amountDueOrRefund is Negative" in new Setup {
      val r1: EditLiabilityReturnsResponse = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber =
        Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(0.0), paymentReference = Some("payment-ref-01"))
      val resp: EditLiabilityReturnsResponseModel = EditLiabilityReturnsResponseModel(ZonedDateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      viewWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle(s"Your amended return has been successfully submitted - Annual Tax on enveloped dwellings"))
      }
    }

    "redirect to account summary, if response found in cache but formbundle doesn't match" in new Setup {
      val r1: EditLiabilityReturnsResponse = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo2, formBundleNumber =
        Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(0.0), paymentReference = Some("payment-ref-01"))
      val resp: EditLiabilityReturnsResponseModel = EditLiabilityReturnsResponseModel(ZonedDateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      viewWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/ated/account-summary"))
      }
    }

    "throw exception, if response not-found in cache" in new Setup {
      viewWithAuthorisedUser() {
        result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("Return Response not found in cache")
      }
    }
  }
}
