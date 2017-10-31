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

import builders._
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DisposeLiabilityReturnService
import uk.gov.hmrc.play.frontend.auth.DummyDelegationData
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, UserId }

class DisposeLiabilityDeclarationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockDisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestDisposeLiabilityDeclarationController extends DisposeLiabilityDeclarationController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val disposeLiabilityReturnService: DisposeLiabilityReturnService = mockDisposeLiabilityReturnService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector

  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockDelegationConnector)
    reset(mockDisposeLiabilityReturnService)
    reset(mockBackLinkCache)
  }

  val oldFormBundleNum = "123456789012"
  val newFormBundleNum = "123456789011"

  "DisposeLiabilityDeclarationController" must {

    "use correct DelegationConnector" in {
      DisposeLiabilityDeclarationController.delegationConnector must be(FrontendDelegationConnector)
    }

    "use correct Service" in {
      DisposeLiabilityDeclarationController.disposeLiabilityReturnService must be(DisposeLiabilityReturnService)
    }


    "view" must {

      "not respond with Not_Found" in {
        val result = route(FakeRequest(GET, "/ated/liability/1234567890/dispose"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
      }

      "take user to dispose declaration page" in {
        viewWithAuthorisedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title must be("Amended return declaration")
          document.getElementById("dispose-liability-declaration-confirmation-header").text() must be("Amended return declaration")
          document.getElementById("dispose-liability-declaration-before-declaration-text").text() must be("Before you can submit your return to HMRC you must read and agree to the following statement . If you give false information you may have to pay financial penalties and face prosecution.")
          document.getElementById("declaration-confirmation-text").text() must be("I declare that the information I have given on this return is correct and complete.")
          document.getElementById("submit").text() must be("Agree and submit amended return")
        }
      }

      "take AGENT to dispose declaration page" in {
        viewWithAuthorisedDelegatedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title must be("Amended return declaration")
          document.getElementById("dispose-liability-declaration-confirmation-header").text() must be("Amended return declaration")
          document.getElementById("dispose-liability-declaration-before-declaration-text").text() must be("Before your client’s return can be submitted to HMRC, you must read and agree to the following statement. Your client’s approval may be in electronic or non-electronic form. If your client gives false information, they may have to pay financial penalties and face prosecution.")
          document.getElementById("declaration-confirmation-text").text() must be("I confirm that my client has approved the information contained in this return as being correct and complete to the best of their knowledge and belief.")
          document.getElementById("submit").text() must be("Agree and submit amended return")
        }
      }

    }

    "submit" must {

      "for valid data, redirect to respective return sent page, if form-bundle found in response" in {
        val cL1 = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn(oldFormBundleNum)
        val calc1 = DisposeLiabilityReturnBuilder.generateCalculated
        val cL2 = cL1.copy(calculated = Some(calc1))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser(Some(cL2)) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/ated/liability/$oldFormBundleNum/dispose/sent")
        }
      }

      "for valid data, redirect to account summary page, if form-bundle not-found in response" in {
        val cL1 = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn(oldFormBundleNum)
        val calc1 = DisposeLiabilityReturnBuilder.generateCalculated
        val cL2 = cL1.copy(calculated = Some(calc1))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser(Some(cL2), newFormBundleNum) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/ated/account-summary")
        }
      }

    }
  }

  implicit def atedContext2AuthContext(implicit atedContext: AtedContext) = atedContext.user.authContext

  def viewWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val result = TestDisposeLiabilityDeclarationController.view(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedDelegatedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createDelegatedAuthContext(userId, "company name|display name"))
    implicit val hc: HeaderCarrier = HeaderCarrier(userId = Some(UserId(userId)))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDelegationConnector.getDelegationData(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(DummyDelegationData.returnData)))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestDisposeLiabilityDeclarationController.view(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSessionDelegation(userId))
    test(result)
  }


  def submitWithAuthorisedUser(a: Option[DisposeLiabilityReturn], oldForBundle: String = oldFormBundleNum)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(Matchers.eq(oldFormBundleNum))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(a))
    val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = oldForBundle, formBundleNumber = Some(newFormBundleNum), liabilityAmount = BigDecimal(3500.00), amountDueOrRefund = BigDecimal(0.00), paymentReference = Some("payment-ref-1"))
    val response = EditLiabilityReturnsResponseModel(processingDate = DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
    when(mockDisposeLiabilityReturnService.submitDraftDisposeLiability(Matchers.eq(oldFormBundleNum))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(response))
    val result = TestDisposeLiabilityDeclarationController.submit(oldFormBundleNum).apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
    test(result)
  }


}
