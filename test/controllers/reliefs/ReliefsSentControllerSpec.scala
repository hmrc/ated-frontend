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

package controllers.reliefs

import java.util.UUID

import builders.{SessionBuilder, TitleBuilder}
import connectors.DataCacheConnector
import models.{ReliefReturnResponse, SubmitReturnsResponse}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{DelegationService, ReliefsService, SubscriptionDataService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import utils.AtedConstants._
import utils.MockAuthUtil

import scala.concurrent.Future

class ReliefsSentControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val organisationName = "ACME Limited"

object TestReliefsSentController extends ReliefsSentController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val subscriptionDataService: SubscriptionDataService = mockSubscriptionDataService
    val reliefsService: ReliefsService = mockReliefsService
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
    override val delegationService: DelegationService = mockDelegationService
  }

  val periodKey = 2015
  val submittedDate: String = LocalDate.now().toString(DateTimeFormat.forPattern("d MMMM yyyy"))

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockReliefsService)
  }

  "ReliefsSentController" must {

    "use correct DelegationService" in {
      ReliefsSentController.delegationService must be(DelegationService)
    }

    "view" must {

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "return sent relief success view" in {
          getWithAuthorisedUser { result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Your returns have been successfully submitted - Annual Tax on enveloped dwellings"))
            document.getElementById("banner-text").text() must be(s"Your returns have been successfully submitted")
            document.getElementById("completed-returns")
              .text() must be("You can view your completed returns, payment references and ways to pay in the ATED online service.")
            document.getElementById("email-confirmation").text() must be("You will not receive an email confirmation.")
            document.getElementById("receipt-message").text() must be("The ATED charge for these returns is £0")
            document.getElementById("amount-message").text() must be
            "This amount does not reflect any payments you have already made or penalties that have been issued."

          }
        }

        "return print friendly sent relief success view" in {
          getPrintFriendlyWithAuthorisedUser { result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Your returns have been successfully submitted - Annual Tax on enveloped dwellings")
            document.getElementById("header").text() must be(s"Your returns have been successfully submitted")
            document.getElementById("completed-returns")
              .text() must be("You can view your completed returns, payment references and ways to pay in the ATED online service.")
            document.getElementById("email-confirmation").text() must be("You will not receive an email confirmation.")
            document.getElementById("receipt-message").text() must be("The ATED charge for these returns is £0")
            document.getElementById("amount-message").text() must be
            "This amount does not reflect any payments you have already made or penalties that have been issued."

          }
        }

        "contains Ated account summary link" in {
          getWithAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("account-link").text() must be("Your ATED online service")
            document.getElementById("account-link").attr("href") must be("/ated/account-summary")
          }
        }

        "redirect to relief declaration, if no reliefs found" in {
          getWithAuthorisedUserNoReliefs {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(s"/ated/reliefs/$periodKey/relief-declaration"))
          }
        }
      }

    }
  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val reliefReturnResponse = ReliefReturnResponse(reliefDescription = "Farmhouses",formBundleNumber = "form-bundle-123")
    val submitReturnsResponse = SubmitReturnsResponse(processingDate = DateTime.now().toString, reliefReturnResponse = Some(Seq(reliefReturnResponse)), None)
    when(mockDataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](Matchers.eq(SubmitReturnsResponseFormId))
      (Matchers.any(), Matchers.any(), Matchers.eq(SubmitReturnsResponse.formats))).thenReturn(Future.successful(Some(submitReturnsResponse)))
    val result = TestReliefsSentController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getPrintFriendlyWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val reliefReturnResponse = ReliefReturnResponse(reliefDescription = "Farmhouses",formBundleNumber = "form-bundle-123")
    val submitReturnsResponse = SubmitReturnsResponse(processingDate = DateTime.now().toString, reliefReturnResponse = Some(Seq(reliefReturnResponse)), None)
    when(mockDataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](Matchers.eq(SubmitReturnsResponseFormId))
      (Matchers.any(), Matchers.any(), Matchers.eq(SubmitReturnsResponse.formats))).thenReturn(Future.successful(Some(submitReturnsResponse)))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    val result = TestReliefsSentController.viewPrintFriendlyReliefSent(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }


  def getWithAuthorisedUserNoReliefs(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val reliefReturnResponse = ReliefReturnResponse(reliefDescription = "Farmhouses",formBundleNumber = "form-bundle-123")
    val submitReturnsResponse = SubmitReturnsResponse(processingDate = DateTime.now().toString, reliefReturnResponse = Some(Seq(reliefReturnResponse)), None)
    when(mockDataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](Matchers.eq(SubmitReturnsResponseFormId))
      (Matchers.any(), Matchers.any(), Matchers.eq(SubmitReturnsResponse.formats))).thenReturn(Future.successful(None))
    val result = TestReliefsSentController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestReliefsSentController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
