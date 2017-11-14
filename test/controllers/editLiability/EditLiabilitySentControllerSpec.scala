/*
 * Copyright 2017 HM Revenue & Customs
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

import builders.{AuthBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.DataCacheConnector
import models.{EditLiabilityReturnsResponse, EditLiabilityReturnsResponseModel, LiabilityReturnResponse}
import org.joda.time.DateTime
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

class EditLiabilitySentControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService = mock[SubscriptionDataService]
  val organisationName = "ACME Limited"
  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789011"

  object TestEditLiabilitySentController extends EditLiabilitySentController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
    override val subscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockDelegationConnector)
    reset(mockDataCacheConnector)
  }

  "EditLiabilitySentController" must {

    "use correct connector" in {
      EditLiabilitySentController.dataCacheConnector must be(DataCacheConnector)
      EditLiabilitySentController.delegationConnector must be(FrontendDelegationConnector)
    }

    "view" must {

      "return amended return sent page, if response found in cache and amountDueOrRefund is Negative" in {
        val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(-500.00), paymentReference = Some("payment-ref-01"))
        val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
        viewWithAuthorisedUser(Some(resp)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Amended return confirmation")
            document.getElementById("header").text() must be("Your amended return has been successfully submitted")
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
            document.getElementById("late-payment").text() must be("Late payment penalties can be issued when ATED is unpaid. Find out about how to pay and payment deadlines.")
            document.getElementById("view-balance").text() must be("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")
        }
      }

      "return further return sent page, if response found in cache and amountDueOrRefund is Negative" in {
        val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(500.00), paymentReference = Some("payment-ref-01"))
        val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
        viewWithAuthorisedUser(Some(resp)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Further return confirmation")
            document.getElementById("header").text() must be("Your further return has been successfully submitted")
            document.getElementById("view-message").text() must be("You can view your completed returns, overall balance, payment references and ways to pay in the ATED online service.")
            document.getElementById("email-message").text() must be("You will not receive an email confirmation.")
            document.getElementById("charges-heading").text() must be("Charges for this return")
            document.getElementById("new-amount").text() must be("Your new adjusted amount does not reflect any payments you have already made or penalties that have been issued.")
            document.getElementById("already-paid-title-further").text() must be("Your previous ATED liability")
            document.getElementById("already-paid-text").text() must be("If you have already paid you owe HMRC £500 for this further return")
            document.getElementById("not-paid-title-further").text() must be("If you have not already paid you owe HMRC £1,235 for this further return")
            document.getElementById("payment-reference").text() must be("The reference to make this payment is payment-ref-01.")
            document.getElementById("late-payment").text() must be("Late payment penalties can be issued when ATED is unpaid. Find out about how to pay and payment deadlines.")
            document.getElementById("view-balance").text() must be("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")
        }
      }

      "return edit details return sent page, if response found in cache and amountDueOrRefund is Negative" in {
        val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(0.0), paymentReference = Some("payment-ref-01"))
        val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
        viewWithAuthorisedUser(Some(resp)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Change in details return confirmation")
            document.getElementById("header").text() must be("Your change in details has been successfully submitted")
            document.getElementById("view-message").text() must be("You can view your completed returns, overall balance, payment references and ways to pay in the ATED online service.")
            document.getElementById("email-message").text() must be("You will not receive an email confirmation.")
            document.getElementById("charges-heading").text() must be("Charges for this return")
            document.getElementById("new-amount").text() must be("Your new adjusted amount does not reflect any payments you have already made or penalties that have been issued.")
            document.getElementById("already-paid-title").text() must be("If you have already paid your previous ATED liability")
            document.getElementById("owe-you").text() must be("You owe HMRC £0 for this changed return.")
            document.getElementById("not-paid-title").text() must be("If you have not paid your previous ATED liability")
            document.getElementById("you-owe").text() must be("You owe HMRC £1,235 for this changed return.")
            document.getElementById("payment-reference").text() must be("The reference to make this payment is payment-ref-01.")
            document.getElementById("late-payment").text() must be("Late payment penalties can be issued when ATED is unpaid. Find out about how to pay and payment deadlines.")
            document.getElementById("view-balance").text() must be("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")
        }
      }


      "take user to print friendly edit liability confirmation" in {
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
            document.getElementById("payment-reference").text() must be("The reference to make this payment is payment-ref-01.")
            document.getElementById("you-owe").text() must be("You owe HMRC £1,235 for this amended return.")

        }
      }


      "take user to print friendly edit liability confirmation bigger than zero" in {
        val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(500.00), paymentReference = Some("payment-ref-01"))
        val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
        getPrintFriendlyWithAuthorisedUser(Some(resp)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("header").text() must include("Your further return has been successfully submitted")

        }
      }
      "take user to print friendly edit liability confirmation exactly zero" in {
        val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = formBundleNo1, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(1234.56), amountDueOrRefund = BigDecimal(0.00), paymentReference = Some("payment-ref-01"))
        val resp = EditLiabilityReturnsResponseModel(DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
        getPrintFriendlyWithAuthorisedUser(Some(resp)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("header").text() must include("Your change in details has been successfully submitted")
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

  }

  def viewWithAuthorisedUser(x: Option[EditLiabilityReturnsResponseModel] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockDataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel](Matchers.eq(SubmitEditedLiabilityReturnsResponseFormId))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(x))
    val result = TestEditLiabilitySentController.view(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getPrintFriendlyWithAuthorisedUser(x: Option[EditLiabilityReturnsResponseModel] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val liabilityReturnResponse = LiabilityReturnResponse(mode = "Post", propertyKey = "1",
      liabilityAmount = BigDecimal("123"), paymentReference = Some("Payment-123"), formBundleNumber = "form-bundle-123")
    when(mockDataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel](Matchers.eq(SubmitEditedLiabilityReturnsResponseFormId))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(x))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = TestEditLiabilitySentController.viewPrintFriendlyEditLilabilitySent(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
