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

import builders.{PropertyDetailsBuilder, SessionBuilder}
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{HasValueChanged, PropertyDetails}
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
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class EditLiabilityHasValueChangedControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {


  val mockService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  object TestChangeLiabilityValueController extends EditLiabilityHasValueChangedController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService
    override val propertyDetailsService: PropertyDetailsService = mockService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach: Unit = {
    reset(mockAuthConnector)
    reset(mockDelegationService)
    reset(mockService)
    reset(mockBackLinkCache)
  }

  "EditLiabilityHasValueChangedController" must {

    "use correct DelegationService" in {
      EditLiabilityHasValueChangedController.delegationService must be(DelegationService)
    }

    "use correct Service" in {
      EditLiabilityHasValueChangedController.propertyDetailsService must be(PropertyDetailsService)
    }

    "view - for authorised users" must {

      "return a status of OK, when that liability return is found in cache" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getPropertyDetailsWithFormBundleReturn("12345678901")
        viewWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be ("Has the value of your property changed for the purposes of ATED? - GOV.UK")
        }
      }
    }

    "editFromSummary - for authorised users" must {

      "return a status of OK and set the back link to the summary page" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getPropertyDetailsWithFormBundleReturn("12345678901")
        editFromSummary(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be ("Has the value of your property changed for the purposes of ATED? - GOV.UK")

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/liability/create/summary/12345678901")
        }
      }
    }


    "save - for authorised user" must {

      "for invalid data, return BAD_REQUEST" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getPropertyDetailsWithFormBundleReturn("12345678901")
        val inputJson = Json.parse("""{"startDate.day": "31", "startDate.month": "6", "startDate.year": "2015", "periodKey": 2015}""")
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(Some(changeLiabilityReturn), inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
        }
      }

      "for valid date when we have indicated that the value has changed, save and redirect to change in acquisition page" in {
        val value1 = HasValueChanged(Some(true))
        val inputJson = Json.toJson(value1)
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(None, inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/create/owned-before/view/12345678901"))
        }
      }

      "for valid date when we have indicated that the value has NOT changed, save and redirect to change in period page" in {
        val value1 = HasValueChanged(Some(false))
        val inputJson = Json.toJson(value1)
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(None, inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/create/full-tax-period/view/12345678901"))
        }
      }
    }

  }

  def viewWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
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
    val result = TestChangeLiabilityValueController.view("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editFromSummary(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestChangeLiabilityValueController.editFromSummary("12345678901", Some(true)).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def saveWithAuthorisedUser(propertyDetails: Option[PropertyDetails], inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    propertyDetails.map(propVal =>
      when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propVal)))
    )
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.saveDraftHasValueChanged(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(OK))
    val result = TestChangeLiabilityValueController.save("12345678901").apply(SessionBuilder.updateRequestWithSession(FakeRequest()
      .withJsonBody(inputJson), userId))
    test(result)
  }

}
