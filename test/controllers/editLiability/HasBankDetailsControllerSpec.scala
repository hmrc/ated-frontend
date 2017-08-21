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

import builders.{AuthBuilder, ChangeLiabilityReturnBuilder, PropertyDetailsBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{BankDetailsModel, HasBankDetails, PropertyDetails, SortCode}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ChangeLiabilityReturnService
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future

class HasBankDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockChangeLiabilityReturnService = mock[ChangeLiabilityReturnService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestBankDetailsController extends HasBankDetailsController {

    override val authConnector: AuthConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val changeLiabilityReturnService = mockChangeLiabilityReturnService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector

  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockDelegationConnector)
    reset(mockChangeLiabilityReturnService)
    reset(mockBackLinkCache)
  }

  "HasBankDetailsController" must {

    "use correct DelegationConnector" in {
      HasBankDetailsController.delegationConnector must be(FrontendDelegationConnector)
    }

    "use correct Service" in {
      HasBankDetailsController.changeLiabilityReturnService must be(ChangeLiabilityReturnService)
    }

    "view - for authorised users" must {
      "not respond with Not_Found" in {
        val result = route(FakeRequest(GET, "/ated/liability/1234567890/change/has-bank-details"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
      }


      "navigate to bank details page, if liablity is retrieved" in {
        val bankDetails = BankDetailsModel()
        val changeLiabilityReturn = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("12345678901").copy(bankDetails = Some(bankDetails))
        viewWithAuthorisedUser(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Do you have the bank details for a repayment?")
            document.getElementById("pre-heading").text() must be("This section is: Change return")

        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        viewWithAuthorisedUser(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "editFromSummary - for authorised users" must {

      "navigate to bank details page, if liablity is retrieved, show the summary back link" in {
        val bankDetails = BankDetailsModel()
        val changeLiabilityReturn = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("12345678901").copy(bankDetails = Some(bankDetails))
        editFromSummary(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Do you have the bank details for a repayment?")

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/liability/12345678901/change/summary")
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        editFromSummary(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "save - for authorised user" must {
      "for invalid data, return BAD_REQUEST" in {
        val inputJson = Json.parse( """{"hasBankDetails": "2"}""")
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(inputJson) {
          result =>
             status(result) must be(BAD_REQUEST)
            verify(mockChangeLiabilityReturnService, times(0)).cacheChangeLiabilityReturnHasBankDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "for valid, redirect to change in value page" in {
        val inputJson = Json.toJson(HasBankDetails(Some(true)))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/bank-details"))
            verify(mockChangeLiabilityReturnService, times(1)).cacheChangeLiabilityReturnHasBankDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "for valid, redirect to change in value page if we have no bank details" in {
        val inputJson = Json.toJson(HasBankDetails(Some(false)))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/view-summary"))
            verify(mockChangeLiabilityReturnService, times(1)).cacheChangeLiabilityReturnHasBankDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }

  }


  def viewWithAuthorisedUser(changeLiabilityReturnOpt: Option[PropertyDetails])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(Matchers.eq("12345678901"), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(changeLiabilityReturnOpt))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestBankDetailsController.view("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editFromSummary(changeLiabilityReturnOpt: Option[PropertyDetails])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(Matchers.eq("12345678901"), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(changeLiabilityReturnOpt))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestBankDetailsController.editFromSummary("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithAuthorisedUser(inputJson: JsValue)(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val changeLiabilityReturn = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("123456789012")
    when(mockChangeLiabilityReturnService.cacheChangeLiabilityReturnHasBankDetails(Matchers.eq("12345678901"), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(changeLiabilityReturn)))
    val result = TestBankDetailsController.save("12345678901").apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }

}
