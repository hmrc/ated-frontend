/*
 * Copyright 2019 HM Revenue & Customs
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
import config.FrontendDelegationConnector
import connectors.DataCacheConnector
import models.{EditLiabilityReturnsResponse, EditLiabilityReturnsResponseModel, LiabilityReturnResponse, SubmitReturnsResponse}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.Helpers._
import services.SubscriptionDataService
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class DisposeLiabilitySentControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService = mock[SubscriptionDataService]

  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789011"
  val organisationName = "ACME Limited"

  val date = DateTimeFormat.forPattern("d MMMM yyyy").print(new LocalDate())


  object TestDisposeLiabilitySentController extends DisposeLiabilitySentController {
    override val authConnector = mockAuthConnector
    override val subscriptionDataService = mockSubscriptionDataService
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockDelegationConnector)
    reset(mockDataCacheConnector)
  }

  val oldFormBundleNum = "123456789012"

  "DisposeLiabilitySentController" should {

    "use correct DelegationConnector" in {
      DisposeLiabilitySentController.delegationConnector must be(FrontendDelegationConnector)
    }

    "use correct data cache connector" in {
      DisposeLiabilitySentController.dataCacheConnector must be(DataCacheConnector)
    }

  }

  "DisposeLiabilitySentController.view" should {
    "return amended return sent page, if response found in cache and amountDueOrRefund is Negative" in {

      val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(-500.00), paymentReference = Some("payment-ref-01"))
      val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      viewWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include(TitleBuilder.buildTitle(s"Your amended return has been successfully submitted - Annual Tax on enveloped dwellings"))
          document.getElementById("header").text() must be(s"Your amended return has been successfully submitted")
          document.getElementById("view-message").text() must be("You can view your completed returns, overall balance, payment references and ways to pay in the ATED online service.")
          document.getElementById("email-message").text() must be("You will not receive an email confirmation.")
          document.getElementById("charges-heading").text() must be("Charges for this return")
          document.getElementById("new-amount").text() must be("Your new adjusted amount does not reflect any payments you have already made or penalties that have been issued.")
          document.getElementById("already-paid-title").text() must be("If you have already paid your previous ATED liability")
          document.getElementById("owe-you").text() must be("HMRC owe you £500 for this amended return.")
          document.getElementById("repayments").text() must be("Any repayments will be paid into your nominated bank account. We will contact you if you have a non-UK bank account.")
          document.getElementById("not-paid-title").text() must be("If you have not paid your previous ATED liability")
          document.getElementById("you-owe").text() must be("You owe HMRC £1,235 for this amended return.")
          document.getElementById("payment-reference").text() must be("The reference to make this payment is payment-ref-01.")
          document.getElementById("liable-for").text() must be("If you have sold the property you may be liable for ATED-related Capital Gains Tax.")
          document.getElementById("view-balance").text() must be("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")
          document.getElementById("submit").text() must be("Your ATED online service")
      }
    }


    "take user to print friendly dispose liability confirmation" in {
      val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(-500.00), paymentReference = Some("payment-ref-01"))
      val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      getPrintFriendlyWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("header").text() must include("Your amended return has been successfully submitted")
          document.getElementById("view-message").text() must be("You can view your completed returns, overall balance, payment references and ways to pay in the ATED online service.")
          document.getElementById("email-message").text() must be("You will not receive an email confirmation.")
          document.getElementById("charges-heading").text() must be("Charges for this return")
          document.getElementById("new-amount").text() must be("Your new adjusted amount does not reflect any payments you have already made or penalties that have been issued.")
          document.getElementById("already-paid-title").text() must be("If you have already paid your previous ATED liability")
          document.getElementById("owe-you").text() must be("HMRC owe you £500 for this amended return.")
          document.getElementById("repayments").text() must be("Any repayments will be paid into your nominated bank account. We will contact you if you have a non-UK bank account.")
          document.getElementById("not-paid-title").text() must be("If you have not paid your previous ATED liability")
          document.getElementById("you-owe").text() must be("You owe HMRC £1,235 for this amended return.")
          document.getElementById("payment-reference").text() must be("The reference to make this payment is payment-ref-01.")
          document.getElementById("liable-for").text() must be("If you have sold the property you may be liable for ATED-related Capital Gains Tax")
          document.getElementById("view-balance").text() must be("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")
      }
    }


    "take user to print friendly dispose liability confirmation bigger than zero" in {
      val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(500.00), paymentReference = Some("payment-ref-01"))
      val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      getPrintFriendlyWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("header").text() must include("Your amended return has been successfully submitted")
      }
    }
    "take user to print friendly dispose liability confirmation exactly zero" in {
      val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(0.00), paymentReference = Some("payment-ref-01"))
      val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      getPrintFriendlyWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("header").text() must include("Your amended return has been successfully submitted")
      }
    }



    "return further return sent page, if response found in cache and amountDueOrRefund is Negative" in {
      val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(500.00), paymentReference = Some("payment-ref-01"))
      val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      viewWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle(s"Your amended return has been successfully submitted - Annual Tax on enveloped dwellings"))
      }
    }

    "return edit details return sent page, if response found in cache and amountDueOrRefund is Negative" in {
      val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(0.0), paymentReference = Some("payment-ref-01"))
      val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      viewWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle(s"Your amended return has been successfully submitted - Annual Tax on enveloped dwellings"))
      }
    }

    "redirect to account summary, if response found in cache but formbundle doesn't match" in {
      val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo2, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(0.0), paymentReference = Some("payment-ref-01"))
      val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
      viewWithAuthorisedUser(Some(resp)) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/ated/account-summary"))
      }
    }

    "throw exception, if response not-found in cache" in {
      viewWithAuthorisedUser() {
        result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("Return Response not found in cache")
      }
    }
  }

  private def viewWithAuthorisedUser(x: Option[EditLiabilityReturnsResponseModel] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockDataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel](Matchers.eq(SubmitEditedLiabilityReturnsResponseFormId))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(x))
    val result = TestDisposeLiabilitySentController.view(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def getPrintFriendlyWithAuthorisedUser(x: Option[EditLiabilityReturnsResponseModel] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val liabilityReturnResponse = LiabilityReturnResponse(mode = "Post", propertyKey = "1",
      liabilityAmount = BigDecimal("123"), paymentReference = Some("Payment-123"), formBundleNumber = "form-bundle-123")
    when(mockDataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel](Matchers.eq(SubmitEditedLiabilityReturnsResponseFormId))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(x))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = TestDisposeLiabilitySentController.viewPrintFriendlyDisposeliabilitySent(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
  
}
