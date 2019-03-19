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

import builders.{AuthBuilder, PropertyDetailsBuilder, SessionBuilder, TitleBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models._
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
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future

class PropertyDetailsAcquisitionControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockService = mock[PropertyDetailsService]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]


  object TestPropertyDetailsController extends PropertyDetailsAcquisitionController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val propertyDetailsService = mockService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockService)
    reset(mockDelegationConnector)
    reset(mockBackLinkCache)
  }


  "PropertyDetailsAcquisitionController" must {

    "use correct DelegationConnector" in {
      PropertyDetailsAcquisitionController.delegationConnector must be(FrontendDelegationConnector)
    }

    "propertyDetails" must {

      "not respond with NOT_FOUND when we try to view an id" in {
        val result = route(FakeRequest(GET, "/ated/liability/create/acquisition/view/1"))
        result.isDefined must be(true)
        status(result.get) must not be(NOT_FOUND)
      }

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

        "show the chargeable property details view if we have id and data" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(value = None)
          getDataWithAuthorisedUser("1", propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("property-details-header").text() must be("In this chargeable period, have you bought or sold land, or extended an existing lease on the property, for £40,000 or more?")
              document.getElementById("anAcquisition-reveal").text() must be("Why is the £40,000 level important?")
              document.getElementById("anAcquisition").text() contains ("Yes No")
              document.getElementById("anAcquisition-true").attr("checked") must be("")
              document.getElementById("anAcquisition-false").attr("checked") must be("")

          }
        }
      }
    }

    "editFromSummary" must {

      "Authorised users" must {

        "show the owned before view if we id and data and add the summary back link" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(value = None)
          editFromSummary("1", propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("In this chargeable period, have you bought or sold land, or extended an existing lease on the property, for £40,000 or more? - GOV.UK")
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

          val inputJson = Json.parse( """{"anAcquisition": "2"}""")
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "When the acquisition is true forward to the Revalued Page" in {
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Json.toJson(PropertyDetailsAcquisition(Some(true)))) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/revalued/view")
          }
        }
        "When the acquisition is false forward to the Owned Before Page" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Json.toJson(PropertyDetailsAcquisition(Some(false)))) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/full-tax-period/view")
          }
        }
      }

    }
  }


  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestPropertyDetailsController.view("1").apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPropertyDetailsController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getDataWithAuthorisedUser(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestPropertyDetailsController.view(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editFromSummary(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestPropertyDetailsController.editFromSummary(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsController.save("1", 2015, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestPropertyDetailsController.save("1", 2015, None).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUser(inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockService.saveDraftPropertyDetailsAcquisition(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(OK))

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))

    test(result)
  }
}
