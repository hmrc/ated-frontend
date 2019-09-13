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

package controllers.propertyDetails

import java.util.UUID

import builders.SessionBuilder
import connectors.DataCacheConnector
import models.{LiabilityReturnResponse, SubmitReturnsResponse}
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{DelegationService, SubscriptionDataService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants._
import utils.MockAuthUtil

import scala.concurrent.Future

class ChargeableReturnConfirmationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MockitoSugar with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val organisationName: String = "ACME Limited"

  object TestChargeableReturnConfirmationController extends ChargeableReturnConfirmationController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val subscriptionDataService: SubscriptionDataService = mockSubscriptionDataService
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
    override val delegationService: DelegationService = mockDelegationService
  }


  override def beforeEach: Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheConnector)
    reset(mockDelegationService)
  }

  "ChargeableReturnConfirmationController" must {

    "use correct DelegationService" in {
      ChargeableReturnConfirmationController.delegationService must be(DelegationService)
    }

    "confirmation" must {

      "unauthorised users" must {

        "respond with a redirect" in {
          confirmationWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
          confirmationWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "take user to Chargeable return confirmation page" in {
          confirmationWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("banner").text() must include("Your return has been successfully submitted")
              document.getElementById("completed-message")
                .text() must be("You can view your completed returns, payment references and ways to pay in the ATED online service.")
              document.getElementById("email-message").text() must include("You will not receive an email confirmation.")
              document.getElementById("receipt-message-title").text() must include("Charges for this return")
              document.getElementById("adjusted-amount")
                .text() must include("This amount does not reflect any payments you have already made or penalties that have been issued.")
              document.getElementById("owed-amount").text() must include("The charges for this return are")
              document.getElementById("reference-text").text() must include("The reference to make this payment is")
              document.getElementById("not-receive-email")
                .text() must be("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")
              document.getElementById("submit").text() must be("Your ATED online service")
              document.getElementById("submit").attr("href") must be("/ated/account-summary")
          }
        }

        "take user to print friendly Chargeable return confirmation" in {
          getPrintFriendlyWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("header").text() must include("Your return has been successfully submitted")
              document.getElementById("completed-message")
                .text() must be("You can view your completed returns, payment references and ways to pay in the ATED online service.")
              document.getElementById("email-message").text() must include("You will not receive an email confirmation.")
              document.getElementById("receipt-message-title").text() must include("Charges for this return")
              document.getElementById("adjusted-amount")
                .text() must include("This amount does not reflect any payments you have already made or penalties that have been issued.")
              document.getElementById("owed-amount").text() must include("The charges for this return are")
              document.getElementById("reference-text").text() must include("The reference to make this payment is")
              document.getElementById("not-receive-email")
                .text() must be("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")

          }
        }

        "redirect to account summary, if return response not found in cache" in {
          confirmationWithAuthorisedUserNotFound {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some("/ated/account-summary"))
          }
        }

      }
    }
  }

  def confirmationWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val liabilityReturnResponse = LiabilityReturnResponse(mode = "Post", propertyKey = "1",
      liabilityAmount = BigDecimal("123"), paymentReference = Some("Payment-123"), formBundleNumber = "form-bundle-123")
    val submitReturnsResponse = SubmitReturnsResponse(processingDate = DateTime.now().toString, None, liabilityReturnResponse =
      Some(Seq(liabilityReturnResponse)))
    when(mockDataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](Matchers.eq(SubmitReturnsResponseFormId))
      (Matchers.any(), Matchers.any(), Matchers.eq(SubmitReturnsResponse.formats))).thenReturn(Future.successful(Some(submitReturnsResponse)))
    val result = TestChargeableReturnConfirmationController.confirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithAuthorisedUserNotFound(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](Matchers.eq(SubmitReturnsResponseFormId))
      (Matchers.any(), Matchers.any(), Matchers.eq(SubmitReturnsResponse.formats))).thenReturn(Future.successful(None))
    val result = TestChargeableReturnConfirmationController.confirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestChargeableReturnConfirmationController.confirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestChargeableReturnConfirmationController.confirmation.apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getPrintFriendlyWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val liabilityReturnResponse = LiabilityReturnResponse(mode = "Post", propertyKey = "1",
      liabilityAmount = BigDecimal("123"), paymentReference = Some("Payment-123"), formBundleNumber = "form-bundle-123")
    val submitReturnsResponse = SubmitReturnsResponse(processingDate = DateTime.now().toString, None, liabilityReturnResponse =
      Some(Seq(liabilityReturnResponse)))
    when(mockDataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](Matchers.eq(SubmitReturnsResponseFormId))
      (Matchers.any(), Matchers.any(), Matchers.eq(SubmitReturnsResponse.formats))).thenReturn(Future.successful(Some(submitReturnsResponse)))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = TestChargeableReturnConfirmationController.viewPrintFriendlyChargeableConfirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


}
