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

package controllers.propertyDetails

import java.util.UUID
import builders.SessionBuilder
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.AuthAction
import models.{LiabilityReturnResponse, SubmitReturnsResponse}
import java.time.ZonedDateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants._
import views.html.BtaNavigationLinks
import views.html.propertyDetails.chargeableReturnsConfirmation

import scala.concurrent.Future

class ChargeableReturnConfirmationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MockitoSugar with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val organisationName: String = "ACME Limited"
  val injectedViewInstance: chargeableReturnsConfirmation = app.injector.instanceOf[views.html.propertyDetails.chargeableReturnsConfirmation]

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testChargeableReturnConfirmationController: ChargeableReturnConfirmationController = new ChargeableReturnConfirmationController (
    mockMcc,
    mockSubscriptionDataService,
    mockAuthAction,
    mockServiceInfoService,
    mockDataCacheConnector,
    injectedViewInstance
  )

  def confirmationWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val liabilityReturnResponse = LiabilityReturnResponse(mode = "Post", propertyKey = "1",
      liabilityAmount = BigDecimal("123"), paymentReference = Some("Payment-123"), formBundleNumber = "form-bundle-123")
    val submitReturnsResponse = SubmitReturnsResponse(processingDate = ZonedDateTime.now().toString, None, liabilityReturnResponse =
      Some(Seq(liabilityReturnResponse)))
    when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
    when(mockDataCacheConnector.fetchAndGetData[SubmitReturnsResponse](ArgumentMatchers.eq(SubmitReturnsResponseFormId))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(submitReturnsResponse)))

    val result = testChargeableReturnConfirmationController.confirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithAuthorisedUserNotFound(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAndGetData[SubmitReturnsResponse](ArgumentMatchers.eq(SubmitReturnsResponseFormId))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = testChargeableReturnConfirmationController.confirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = testChargeableReturnConfirmationController.confirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithUnAuthenticated(test: Future[Result] => Any): Unit = {
    val result = testChargeableReturnConfirmationController.confirmation.apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getPrintFriendlyWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val liabilityReturnResponse = LiabilityReturnResponse(mode = "Post", propertyKey = "1",
      liabilityAmount = BigDecimal("123"), paymentReference = Some("Payment-123"), formBundleNumber = "form-bundle-123")
    val submitReturnsResponse = SubmitReturnsResponse(processingDate = ZonedDateTime.now().toString, None, liabilityReturnResponse =
      Some(Seq(liabilityReturnResponse)))
    when(mockDataCacheConnector.fetchAndGetData[SubmitReturnsResponse](ArgumentMatchers.eq(SubmitReturnsResponseFormId))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(submitReturnsResponse)))
    when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = testChargeableReturnConfirmationController.viewPrintFriendlyChargeableConfirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}

  override def beforeEach(): Unit = {

    reset(mockSubscriptionDataService)
    reset(mockDelegationService)
    reset(mockDataCacheConnector)
  }

  "ChargeableReturnConfirmationController" must {

    "confirmation" must {

      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          confirmationWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in new Setup {
          confirmationWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "take user to Chargeable return confirmation page" in new Setup {
          confirmationWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-panel__title").text() must include("Your return has been successfully submitted")
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
              document.getElementById("completed-message")
                .text() must include("You can view your completed returns, payment references and ways to pay in the ATED online service.")
              document.getElementById("email-message").text() must include("You will not receive an email confirmation.")
              document.getElementById("print-friendly-relief-link").text() must include("Print confirmation")
              document.getElementById("print-friendly-relief-link").attr("href") must include("/ated/liability/create/create-confirmation-print")
              document.getElementById("receipt-message-title").text() must include("Charges for this return")
              document.getElementById("adjusted-amount")
                .text() must include("This amount does not reflect any payments you have already made or penalties that have been issued.")
              document.getElementById("owed-amount").text() must include("The charges for this return are")
              document.getElementById("reference-text").text() must include("The reference to make this payment is")
              document.getElementById("late-payment").text() must include("Late payment penalties can be issued when ATED is unpaid. Find out about how to pay and payment deadlines (opens in new tab).")
              document.getElementById("late-payment-link").attr("href") must include("https://www.gov.uk/guidance/pay-annual-tax-on-enveloped-dwellings")
              document.getElementById("not-receive-email")
                .text() must include("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")
              document.getElementById("submit").text() must include("Your ATED summary")
              document.getElementById("submit").attr("href") must include("/ated/account-summary")
          }
        }

        "take user to print friendly Chargeable return confirmation" in new Setup {
          getPrintFriendlyWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("header").text() must include("Your return has been successfully submitted")
              document.getElementById("receipt-message-title").text() must include("Charges for this return")
              document.getElementById("adjusted-amount")
                .text() must include("This amount does not reflect any payments you have already made or penalties that have been issued.")
              document.getElementById("owed-amount").text() must include("The charges for this return are")
              document.getElementById("reference-text").text() must include("The reference to make this payment is")
              document.getElementById("late-payment").text() must include("Late payment penalties can be issued when ATED is unpaid. Find out more by searching GOV.UK for ‘pay annual tax on enveloped dwellings’.")
              document.getElementById("not-receive-email")
                .text() must be("You can view your balance in your ATED online service. There can be a 24-hour delay before you see any updates.")
          }
        }

        "redirect to account summary, if return response not found in cache" in new Setup {
          confirmationWithAuthorisedUserNotFound {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some("/ated/account-summary"))
          }
        }

      }
    }
  }
}
