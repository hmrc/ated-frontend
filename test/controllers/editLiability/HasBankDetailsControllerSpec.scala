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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{BankDetailsModel, HasBankDetails, PropertyDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ChangeLiabilityReturnService, DelegationService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class HasBankDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {


  val mockChangeLiabilityReturnService: ChangeLiabilityReturnService = mock[ChangeLiabilityReturnService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  object TestBankDetailsController extends HasBankDetailsController {

    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService
    override val changeLiabilityReturnService: ChangeLiabilityReturnService = mockChangeLiabilityReturnService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector

  }

  override def beforeEach: Unit = {
    reset(mockAuthConnector)
    reset(mockDelegationService)
    reset(mockChangeLiabilityReturnService)
    reset(mockBackLinkCache)
  }

  "HasBankDetailsController" must {

    "use correct DelegationService" in {
      HasBankDetailsController.delegationService must be(DelegationService)
    }

    "use correct Service" in {
      HasBankDetailsController.changeLiabilityReturnService must be(ChangeLiabilityReturnService)
    }

    "view - for authorised users" must {

      "navigate to bank details page, if liablity is retrieved" in {
        val bankDetails = BankDetailsModel()
        val changeLiabilityReturn = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("12345678901").copy(bankDetails = Some(bankDetails))
        viewWithAuthorisedUser(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Do you have a bank account where we could pay a refund? - GOV.UK")
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
            document.title() must be("Do you have a bank account where we could pay a refund? - GOV.UK")

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
            verify(mockChangeLiabilityReturnService, times(0))
              .cacheChangeLiabilityReturnHasBankDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "for valid, redirect to change in value page" in {
        val inputJson = Json.toJson(HasBankDetails(Some(true)))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/bank-details"))
            verify(mockChangeLiabilityReturnService, times(1))
              .cacheChangeLiabilityReturnHasBankDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "for valid, redirect to change in value page if we have no bank details" in {
        val inputJson = Json.toJson(HasBankDetails(Some(false)))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/view-summary"))
            verify(mockChangeLiabilityReturnService, times(1))
              .cacheChangeLiabilityReturnHasBankDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }

  }


  def viewWithAuthorisedUser(changeLiabilityReturnOpt: Option[PropertyDetails])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
    (Matchers.eq("12345678901"), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(changeLiabilityReturnOpt))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestBankDetailsController.view("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editFromSummary(changeLiabilityReturnOpt: Option[PropertyDetails])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
    (Matchers.eq("12345678901"), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(changeLiabilityReturnOpt))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestBankDetailsController.editFromSummary("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithAuthorisedUser(inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val changeLiabilityReturn = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("123456789012")
    when(mockChangeLiabilityReturnService.cacheChangeLiabilityReturnHasBankDetails
    (Matchers.eq("12345678901"), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(changeLiabilityReturn)))
    val result = TestBankDetailsController.save("12345678901").apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }

}
