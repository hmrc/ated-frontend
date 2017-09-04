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
import models.DisposeLiabilityReturn
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DisposeLiabilityReturnService, SubscriptionDataService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AtedConstants

import scala.concurrent.Future

class DisposeLiabilitySummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockDisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService = mock[SubscriptionDataService]
  val organisationName = "ACME Limited"

  object TestDisposeLiabilitySummaryController extends DisposeLiabilitySummaryController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val disposeLiabilityReturnService: DisposeLiabilityReturnService = mockDisposeLiabilityReturnService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
    override val subscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockDelegationConnector)
    reset(mockDisposeLiabilityReturnService)
    reset(mockBackLinkCache)
    reset(mockSubscriptionDataService)
  }

  val oldFormBundleNum = "123456789012"

  "DisposeLiabilitySummaryController" must {

    "use correct DelegationConnector" in {
      DisposeLiabilitySummaryController.delegationConnector must be(FrontendDelegationConnector)
    }

    "use correct Service" in {
      DisposeLiabilitySummaryController.disposeLiabilityReturnService must be(DisposeLiabilityReturnService)
    }


    "view" must {

      "not respond with Not_Found" in {
        val result = route(FakeRequest(GET, "/ated/liability/1234567890/dispose"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
      }

      "return a status of OK, when that liability return is found in cache or ETMP" in {
        val disposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("12345678901")
        viewWithAuthorisedUser(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title must be("Check your details are correct")
            document.getElementById("edit-liability-summary-header").text() must be("Check your details are correct")
            document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
            document.getElementById("property-summary").text() must include("Bank details Supply bank details INCOMPLETE Edit")
            document.getElementById("edit-liability-header").text() must be("Property details")
            document.getElementById("edit-property-disposal-date").text() must be("Edit Date disposed property")
            document.getElementById("property-address-label").text() must be("Address")
            document.getElementById("property-address-value").text() must be("line1, line2")
            document.getElementById("address-line-1").text() must be("line1")
            document.getElementById("address-line-2").text() must be("line2")
            document.getElementById("property-title-disposal-date-label").text() must be("Date disposed property")
            document.getElementById("property-title-disposal-date").text() must be("2 April 2015")
            document.getElementById("ated-charge-text").text() must be("Based on the information you have given us your ATED charge is")
            document.getElementById("ated-charge-value").text() must be("")
            document.getElementById("print-friendly-edit-liability-link").text() must be("Print this return")
            document.getElementById("saved-returns-link").text() must be("Save as draft")
            document.getElementById("submit").text() must be("Confirm and continue")
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

    "submit" must {
      "redirect to dispose-property declaration page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/123456789012/dispose/declaration"))
        }
      }
    }

    "print friendly view" when {

      "called for authorised user" must {

        "return status OK" in {
          val disposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("12345678901")
          getPrintFriendlyWithAuthorisedUser(Some(disposeLiabilityReturn)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title must be("Check your details are correct")
              document.getElementById("edit-liability-summary-header").text() must be("Amended return for ACME Limited")
          }
        }

        "redirect to account summary page, when that liability return is not-found in cache or ETMP" in {
          getPrintFriendlyWithAuthorisedUser(None) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some("/ated/account-summary"))
          }
        }
      }
    }
  }

  def viewWithAuthorisedUser(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(Matchers.eq(oldFormBundleNum))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
    val result = TestDisposeLiabilitySummaryController.view(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = TestDisposeLiabilitySummaryController.submit(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getPrintFriendlyWithAuthorisedUser(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(Matchers.eq(oldFormBundleNum))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    val result = TestDisposeLiabilitySummaryController.viewPrintFriendlyDisposeLiabilityReturn(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
