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
import models.{HasValueChanged, PropertyDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ChangeLiabilityReturnService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future

class EditLiabilityHasValueChangedControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockService = mock[PropertyDetailsService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestChangeLiabilityValueController extends EditLiabilityHasValueChangedController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val propertyDetailsService = mockService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockDelegationConnector)
    reset(mockService)
    reset(mockBackLinkCache)
  }

  "EditLiabilityHasValueChangedController" must {

    "use correct DelegationConnector" in {
      EditLiabilityHasValueChangedController.delegationConnector must be(FrontendDelegationConnector)
    }

    "use correct Service" in {
      EditLiabilityHasValueChangedController.propertyDetailsService must be(PropertyDetailsService)
    }

    "view - for authorised users" must {

      "return a status of OK, when that liability return is found in cache" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getFullPropertyDetails("12345678901")
        viewWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be ("Has the value of your property changed for the purposes of ATED?")
        }
      }
    }

    "editFromSummary - for authorised users" must {

      "return a status of OK and set the back link to the summary page" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getFullPropertyDetails("12345678901")
        editFromSummary(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be ("Has the value of your property changed for the purposes of ATED?")

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/liability/create/summary/12345678901")
        }
      }
    }


    "save - for authorised user" must {

      "for invalid data, return BAD_REQUEST" in {
        val changeLiabilityReturn = PropertyDetailsBuilder.getFullPropertyDetails("12345678901")
        val inputJson = Json.parse("""{"startDate.day": "31", "startDate.month": "6", "startDate.year": "2015", "periodKey": 2015}""")
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(Some(changeLiabilityReturn), inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
        }
      }

      "for valid date when we have indicated that the value has changed, save and redirect to change in aquisition page" in {
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
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestChangeLiabilityValueController.view("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editFromSummary(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestChangeLiabilityValueController.editFromSummary("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def saveWithAuthorisedUser(propertyDetails: Option[PropertyDetails], inputJson: JsValue)(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    propertyDetails.map(propVal =>
      when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propVal)))
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
