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

import builders._
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models._
import org.joda.time.LocalDate
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
import utils.{AtedConstants, AtedUtils, MockAuthUtil}

import scala.concurrent.Future

class PropertyDetailsInReliefControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockService: PropertyDetailsService = mock[PropertyDetailsService]

  val periodKey: Int = 2015
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  object TestPropertyDetailsPeriodController extends PropertyDetailsInReliefController {
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


  "PropertyDetailsInReliefController" must {

    "use correct DelegationService" in {
      PropertyDetailsInReliefController.delegationService must be(DelegationService)
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

        "show the chargeable property details value view with no data" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)

          getDataWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
          }
        }

        "show the chargeable property details value view with existing data" in {
          val propertyDetailsPeriod = PropertyDetailsBuilder
            .getPropertyDetailsPeriodDatesLiable(new LocalDate("970-12-01"), new LocalDate("1999-03-02")).map(_.copy(isInRelief = Some(false)))
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = propertyDetailsPeriod)
          getDataWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("isInRelief-true").attr("checked") must be("")
              document.getElementById("isInRelief-false").attr("checked") must be("checked")
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

          val inputJson = Json.parse( """{"isInRelief": "2"}""")
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }
        "for valid data set to true forward to the Periods In Relief Page" in {
          val inputJson = Json.toJson(PropertyDetailsInRelief(Some(true)))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/periods-in-relief/view")
          }
        }
        "for valid data set to false forward to the Dates Liable Page" in {
          val inputJson = Json.toJson(PropertyDetailsInRelief(Some(false)))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/dates-liable/view")
          }
        }

        "for valid Edit Liability Data set to false forward to the Edit Liability Dates Liable Page" in {
          val inputJson = Json.toJson(PropertyDetailsInRelief(Some(false)))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson, Some(AtedUtils.EDIT_SUBMITTED)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/ated/liability/1/change/dates-liable")
          }
        }
      }

    }
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestPropertyDetailsPeriodController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPropertyDetailsPeriodController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getDataWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPropertyDetailsPeriodController.view(propertyDetails.id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestPropertyDetailsPeriodController.save("1", periodKey, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(inputJson: JsValue, mode: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockService.saveDraftPropertyDetailsInRelief(Matchers.eq("1"), Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(OK))
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestPropertyDetailsPeriodController.save("1", periodKey, mode)
      .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }
}
