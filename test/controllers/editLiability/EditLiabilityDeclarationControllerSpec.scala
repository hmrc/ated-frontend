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
import services.ChangeLiabilityReturnService
import uk.gov.hmrc.play.frontend.auth.DummyDelegationData
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, UserId }

class EditLiabilityDeclarationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockChangeLiabilityReturnService = mock[ChangeLiabilityReturnService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestEditLiabilityDeclarationController extends EditLiabilityDeclarationController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val changeLiabilityReturnService: ChangeLiabilityReturnService = mockChangeLiabilityReturnService
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

  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789011"

  "EditLiabilityDeclarationController" must {

    "use correct DelegationConnector" in {
      EditLiabilityTypeController.delegationConnector must be(FrontendDelegationConnector)
    }

    "view" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, s"/ated/liability/$formBundleNo1/change/declaration"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
      }

      "unauthorised users" must {

        "respond with a redirect" in {
          viewWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }
        "be redirected to the login page" in {
          viewWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "authorised users" must {

        "view amended return declaration, if amountDueOrRefund is negative" in {
          val cL1 = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
          val calc1 = ChangeLiabilityReturnBuilder.generateCalculated
          val cL2 = cL1.copy(calculated = Some(calc1))
          viewWithAuthorisedUser(Some(cL2)) { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Amended return declaration")
            document.getElementById("relief-declaration-confirmation-header").text() must be("Amended return declaration")
            document.getElementById("relief-declaration-before-declaration-text").text() must be("Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
            document.getElementById("declaration-confirmation-text").text() must be("I declare that the information I have given on this return is correct and complete.")
            document.getElementById("submit").text() must be("Agree and submit amended return")
          }
        }

        "view further return declaration, if amountDueOrRefund is positive" in {
          val cL1 = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
          val calc1 = ChangeLiabilityReturnBuilder.generateCalculated.copy(amountDueOrRefund = Some(BigDecimal(500.00)))
          val cL2 = cL1.copy(calculated = Some(calc1))
          viewWithAuthorisedUser(Some(cL2)) { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Further return declaration")
            document.getElementById("relief-declaration-confirmation-header").text() must be("Further return declaration")
            document.getElementById("relief-declaration-before-declaration-text").text() must be("Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
            document.getElementById("declaration-confirmation-text").text() must be("I declare that the information I have given on this return is correct and complete.")
            document.getElementById("submit").text() must be("Agree and submit further return")
          }
        }

        "view change in details return declaration, if amountDueOrRefund is zero" in {
          val cL1 = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
          val calc1 = ChangeLiabilityReturnBuilder.generateCalculated.copy(amountDueOrRefund = Some(BigDecimal(0.00)))
          val cL2 = cL1.copy(calculated = Some(calc1))
          viewWithAuthorisedUser(Some(cL2)) { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Change in details declaration")
            document.getElementById("relief-declaration-confirmation-header").text() must be("Change in details declaration")
            document.getElementById("relief-declaration-before-declaration-text").text() must be("Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
            document.getElementById("declaration-confirmation-text").text() must be("I declare that the information I have given on this return is correct and complete.")
            document.getElementById("submit").text() must be("Agree and submit")
          }
        }

        "view change in details return declaration, if amountDueOrRefund is not-defined" in {
          val cL1 = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
          val calc1 = ChangeLiabilityReturnBuilder.generateCalculated.copy(amountDueOrRefund = None)
          val cL2 = cL1.copy(calculated = Some(calc1))
          viewWithAuthorisedUser(Some(cL2)) { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Change in details declaration")
          }
        }

        "view return declaration, if AGENT is acting on behalf of client" in {
          val cL1 = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
          val calc1 = ChangeLiabilityReturnBuilder.generateCalculated.copy(amountDueOrRefund = Some(BigDecimal(0.00)))
          val cL2 = cL1.copy(calculated = Some(calc1))
          viewWithAuthorisedDelegatedUser(Some(cL2)) { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("relief-declaration-before-declaration-text").text() must be("Before your client’s return can be submitted to HMRC, you must read and agree to the following statement. Your client’s approval may be in electronic or non-electronic form. If your client gives false information, they may have to pay financial penalties and face prosecution.")
            document.getElementById("declaration-confirmation-text").text() must be("I confirm that my client has approved the information contained in this return as being correct and complete to the best of their knowledge and belief.")
            document.getElementById("submit").text() must be("Agree and submit")
          }
        }

        "redirected to account summary page, if service doesn't return liability model" in {
          viewWithAuthorisedUser(None) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
          }
        }
      }

    }

    "submit" must {
      "unauthorised users" must {
        "respond with a redirect" in {
          submitWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }
        "be redirected to the login page" in {
          submitWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {
        "for valid data, redirect to respective return sent page, if form-bundle found in response" in {
          val cL1 = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
          val calc1 = ChangeLiabilityReturnBuilder.generateCalculated
          val cL2 = cL1.copy(calculated = Some(calc1))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Some(cL2)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include(s"/ated/liability/$formBundleNo1/change/sent")
          }
        }

        "for valid data, redirect to account summary page, if form-bundle not-found in response" in {
          val cL1 = PropertyDetailsBuilder.getFullPropertyDetails(formBundleNo1)
          val calc1 = ChangeLiabilityReturnBuilder.generateCalculated
          val cL2 = cL1.copy(calculated = Some(calc1))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Some(cL2), formBundleNo2) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include(s"/ated/account-summary")
          }
        }
      }
    }
  }


  def viewWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestEditLiabilityDeclarationController.view(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedUser(x: Option[PropertyDetails] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(Matchers.eq(formBundleNo1), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(x))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestEditLiabilityDeclarationController.view(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  implicit def atedContext2AuthContext(implicit atedContext: AtedContext) = atedContext.user.authContext

  def viewWithAuthorisedDelegatedUser(x: Option[PropertyDetails] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createDelegatedAuthContext(userId, "company name|display name"))
    implicit val hc: HeaderCarrier = HeaderCarrier(userId = Some(UserId(userId)))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDelegationConnector.getDelegationData(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(DummyDelegationData.returnData)))
    when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(Matchers.eq(formBundleNo1), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(x))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestEditLiabilityDeclarationController.view(formBundleNo1).apply(SessionBuilder.buildRequestWithSessionDelegation(userId))
    test(result)
  }

  def viewWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestEditLiabilityDeclarationController.view(formBundleNo1).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }


  def submitWithAuthorisedUser(a: Option[PropertyDetails], oldForBundle: String = formBundleNo1)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(Matchers.eq(formBundleNo1), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(a))
    val r1 = EditLiabilityReturnsResponse(mode = "Post", oldFormBundleNumber = oldForBundle, formBundleNumber = Some(formBundleNo2), liabilityAmount = BigDecimal(3500.00), amountDueOrRefund = BigDecimal(0.00), paymentReference = Some("payment-ref-1"))
    val response = EditLiabilityReturnsResponseModel(processingDate = DateTime.now(), liabilityReturnResponse = Seq(r1), BigDecimal(0.00))
    when(mockChangeLiabilityReturnService.submitDraftChangeLiability(Matchers.eq(formBundleNo1))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(response))
    val result = TestEditLiabilityDeclarationController.submit(formBundleNo1).apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestEditLiabilityDeclarationController.submit(formBundleNo1).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestEditLiabilityDeclarationController.submit(formBundleNo1).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }
}
