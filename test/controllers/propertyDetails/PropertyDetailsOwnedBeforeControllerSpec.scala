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

import builders.{PropertyDetailsBuilder, SessionBuilder}
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models._
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
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.PeriodUtils.calculatePeriod
import utils.{AtedConstants, MockAuthUtil, PeriodUtils}

import scala.concurrent.Future

class PropertyDetailsOwnedBeforeControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val periodKey: Int = calculatePeriod()
  val mockService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val valuationPeriod: String = PeriodUtils.getValuationYear(periodKey)


  object TestPropertyDetailsController extends PropertyDetailsOwnedBeforeController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService
    override val propertyDetailsService: PropertyDetailsService = mockService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockService)
    reset(mockDelegationService)
    reset(mockBackLinkCache)
  }


  "PropertyDetailsOwnedBeforeController" must {

    "use correct DelegationService" in {
      PropertyDetailsAcquisitionController.delegationService must be(DelegationService)
    }

    "propertyDetails" must {

      "unauthorised users" must {

        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the chargeable property details view if we id and data" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          getDataWithAuthorisedUser("1", propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("property-details-header").text() must be (s"Did the company own this property on or before 1 April $valuationPeriod?")
          }
        }
      }
    }

    "editFromSummary" must {

      "Authorised users" must {

        "show the chargeable property details view if we id and data and add the summary back link" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          editFromSummary("1", propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("property-details-header").text() must be (s"Did the company own this property on or before 1 April $valuationPeriod?")

              document.getElementById("backLinkHref").text must be("Back")
              document.getElementById("backLinkHref").attr("href") must include("/ated/liability/create/summary")

          }
        }
      }
    }

    "save" must {
      "unauthorised users" must {

        "be redirected to the login page" in {
          saveWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "for invalid data, return BAD_REQUEST" in {
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Json.toJson(PropertyDetailsOwnedBefore(Some(true)))) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "When the owned before is true forward to the Professionally Valued Page" in {
          val bdValue: Int = 1500000
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Json.toJson(PropertyDetailsOwnedBefore(Some(true), Some(BigDecimal(bdValue))))) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/valued/view")
          }
        }
        "When the owned before is false forward to the New Build Page" in {
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Json.toJson(PropertyDetailsOwnedBefore(Some(false)))) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/new-build/view")
          }
        }
      }

    }
  }


  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestPropertyDetailsController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getDataWithAuthorisedUser(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestPropertyDetailsController.view(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editFromSummary(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestPropertyDetailsController.editFromSummary(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
    val periodKey: Int = 2015
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestPropertyDetailsController.save("1", periodKey, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(inputJson: JsValue)(test: Future[Result] => Any) {
    val periodKey: Int = 2015
    val userId = s"user-${UUID.randomUUID}"
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockService.saveDraftPropertyDetailsOwnedBefore(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(OK))

    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestPropertyDetailsController.save("1", periodKey, None)
      .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))

    test(result)
  }
}
