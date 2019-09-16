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
import models.{BankDetails, BankDetailsModel, PropertyDetails, SortCode}
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

class BankDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {


  val mockChangeLiabilityReturnService: ChangeLiabilityReturnService = mock[ChangeLiabilityReturnService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  object TestBankDetailsController extends BankDetailsController {

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

  "BankController" must {

    "use correct DelegationService" in {
      BankDetailsController.delegationService must be(DelegationService)
    }

    "use correct Service" in {
      BankDetailsController.changeLiabilityReturnService must be(ChangeLiabilityReturnService)
    }

    "view - for authorised users" must {

      "navigate to bank details page, if liablity is retrieved" in {
        val bankDetails = BankDetailsModel()
        val changeLiabilityReturn = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("12345678901").copy(bankDetails = Some(bankDetails))
        viewWithAuthorisedUser(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Is the bank account in the UK?"))
            document.getElementById("pre-heading").text() must be("This section is: Change return")
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in {
        viewWithAuthorisedUser(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "save - for authorised user" must {
      "for invalid data, return BAD_REQUEST" in {
        val bankDetails = BankDetailsModel()
        val inputJson = Json.toJson(bankDetails)
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockChangeLiabilityReturnService, times(0)).cacheChangeLiabilityReturnBank(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "for valid, redirect to change in value page" in {
        val bankDetails = BankDetails(Some(true), Some("ACCOUNTNAME"), Some("123456567890"), Some(SortCode("11", "22", "33")))
        val inputJson = Json.toJson(bankDetails)
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/view-summary"))
            verify(mockChangeLiabilityReturnService, times(1)).cacheChangeLiabilityReturnBank(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
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

  def saveWithAuthorisedUser(inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val changeLiabilityReturn = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("123456789012")
    when(mockChangeLiabilityReturnService.cacheChangeLiabilityReturnBank
    (Matchers.eq("12345678901"), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(changeLiabilityReturn)))
    val result = TestBankDetailsController.save("12345678901").apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }

}
