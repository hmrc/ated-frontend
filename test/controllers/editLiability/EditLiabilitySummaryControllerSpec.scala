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

import builders.{AuthBuilder, PropertyDetailsBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{PropertyDetails, PropertyDetailsCalculated}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{ChangeLiabilityReturnService, PropertyDetailsService, SubscriptionDataService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future

class EditLiabilitySummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockPropertyDetailsService = mock[PropertyDetailsService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService = mock[SubscriptionDataService]
  val organisationName = "ACME Limited"


  object TestChangeLiabilitySummaryController extends EditLiabilitySummaryController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val propertyDetailsService = mockPropertyDetailsService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
    override val subscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockDelegationConnector)
    reset(mockPropertyDetailsService)
    reset(mockBackLinkCache)
    reset(mockSubscriptionDataService)
  }

  "EditLiabilitySummaryController" must {

    "use correct DelegationConnector" in {
      EditLiabilitySummaryController.delegationConnector must be(FrontendDelegationConnector)
    }

    "use correct Service" in {
      EditLiabilitySummaryController.propertyDetailsService must be(PropertyDetailsService)
    }

    "view for authorised user" must {

      "show the edit liabilty summary page, if the return is found in cache is greater than zero" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getFullPropertyDetails("12345678901").
          copy(calculated = Some(PropertyDetailsCalculated(amountDueOrRefund = Some(BigDecimal(123.45)))))
        viewWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Check your details are correct")
        }
      }

      "Redirect to the Account Summary Page of calculated is None" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getFullPropertyDetails("12345678901").copy(calculated = None)
        viewWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/account-summary")
        }
      }
      "Redirect to the Bank Details Page if the Amount Due < 0" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getFullPropertyDetails("12345678901").
          copy(calculated = Some(PropertyDetailsCalculated(amountDueOrRefund = Some(BigDecimal(-123.34)))))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        viewWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/12345678901/change/has-bank-details")
        }
      }

      "return to edit liabilty summary page, if the return is found in cache but calculated is equal to zero" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getFullPropertyDetails("12345678901").
          copy(calculated = Some(PropertyDetailsCalculated(amountDueOrRefund = Some(BigDecimal(0.00)))))
        viewWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Check your details are correct")
            document.getElementById("address-line-1").text() must be("addr1")
            document.getElementById("address-line-2").text() must be("addr2")
            document.getElementById("address-line-3").text() must be("addr3")
            document.getElementById("address-line-4").text() must be("addr4")
        }
      }
    }


    "viewSummary for authorised user" must {

      "view the edit liabilty summary page, if the return is found in cache is greater than zero" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getFullPropertyDetails("12345678901")
        viewSummaryWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Check your details are correct")
        }
      }

      "view the edit liabilty summary page, if the return is found in cache is < 0" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getFullPropertyDetails("12345678901").
          copy(calculated = Some(PropertyDetailsCalculated(amountDueOrRefund = Some(BigDecimal(-123.34)))))
        viewSummaryWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Check your details are correct")
        }
      }
    }

    "submit - for authorised users" must {
      "redirect to return declaration page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/declaration"))
        }
      }
    }

    "print friendly view" when {

      "called for authorised user" must {

        "return status OK when liability is in cache" in {
          val changeLiabilityReturn = PropertyDetailsBuilder.getFullPropertyDetails("12345678901")
          getPrintFriendlyWithAuthorisedUser(changeLiabilityReturn) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Check your details are correct")
              document.getElementById("edit-liability-summary-header").text() must be("Further return for ACME Limited")
          }
        }

      }

    }

  }

  def viewWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockPropertyDetailsService.calculateDraftChangeLiability(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(propertyDetails))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestChangeLiabilitySummaryController.view("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def viewSummaryWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockPropertyDetailsService.calculateDraftChangeLiability(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(propertyDetails))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestChangeLiabilitySummaryController.viewSummary("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAndGetFormData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = TestChangeLiabilitySummaryController.submit("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getPrintFriendlyWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockPropertyDetailsService.calculateDraftChangeLiability(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(propertyDetails))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    val result = TestChangeLiabilitySummaryController.viewPrintFriendlyEditLiabilityReturn("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
