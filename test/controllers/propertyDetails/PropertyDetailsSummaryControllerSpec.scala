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

import builders.{PropertyDetailsBuilder, SessionBuilder, _}
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.PropertyDetails
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService, SubscriptionDataService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class PropertyDetailsSummaryControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MockitoSugar with MockAuthUtil {
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]

  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val organisationName = "ACME Limited"

  object TestPropertyDetailsSummaryController extends PropertyDetailsSummaryController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService
    override val propertyDetailsService: PropertyDetailsService = mockPropertyDetailsService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
    override val subscriptionDataService: SubscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach: Unit = {
    reset(mockAuthConnector)
    reset(mockPropertyDetailsService)
    reset(mockDelegationService)
    reset(mockBackLinkCache)
    reset(mockSubscriptionDataService)
  }

  "PropertyDetailsSummaryController" must {

    "use correct property details service" in {
      PropertyDetailsSummaryController.propertyDetailsService must be(PropertyDetailsService)
    }

    "view" must {

      "unauthorised users" must {

        "respond with a redirect, and be redirected to unauthorised page" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "for authorised user" must {

        "status should be OK when we have a valid property details" in {

          val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
          getWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Check your details are correct"))
          }
        }

        "no periods should be displayed if we have none" in {

          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount =
            Some(BigDecimal(1000.20))).copy(period = None)
          getWithAuthorisedUser (propertyDetails){
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Check your details are correct"))
              document.getElementById("address-line-1").text() must be("addr1")
              document.getElementById("address-line-2").text() must be("addr2")
              document.getElementById("address-line-3").text() must be("addr3")
              document.getElementById("address-line-4").text() must be("addr4")
          }
        }
      }
    }

    "submit" must {

      "redirect to declaration page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/create/declaration/1"))
        }
      }
    }

    "print friendly view" must {

      "called for authorised user" must {

        "return status OK" in {
          val propertyDetails = PropertyDetailsBuilder.getFullPropertyDetails(id = "1", postCode = Some("123456"), liabilityAmount = Some(BigDecimal(1000.20)))
          getPrintFriendlyWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Check your details are correct")
              document.getElementById("property-details-summary-header").text() must be("Chargeable return for ACME Limited")
          }
        }
      }
    }


    "delete the draft redirect to delete confirmation page" in {
      getWithDeleteDraftLink { result =>
        status(result) must be(SEE_OTHER)
      }
    }

  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestPropertyDetailsSummaryController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockPropertyDetailsService.calculateDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(propertyDetails)))))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockPropertyDetailsService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestPropertyDetailsSummaryController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithDeleteDraftLink(test: Future[Result] => Any) {
    val periodKey: Int = 2017
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val result = TestPropertyDetailsSummaryController.deleteDraft("123456", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = TestPropertyDetailsSummaryController.submit("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getPrintFriendlyWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockPropertyDetailsService.calculateDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(propertyDetails)))))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    when(mockPropertyDetailsService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestPropertyDetailsSummaryController.viewPrintFriendlyLiabilityReturn("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
