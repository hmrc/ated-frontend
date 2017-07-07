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

package controllers.propertyDetails

import java.util.UUID

import builders.{AuthBuilder, ChangeLiabilityReturnBuilder, PropertyDetailsBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTimeZone, LocalDate}
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
import uk.gov.hmrc.play.http.HttpResponse
import utils.{AtedConstants, AtedUtils, PeriodUtils}

import scala.concurrent.Future

class PropertyDetailsSupportingInfoControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockService = mock[PropertyDetailsService]
  val mockDelegationConnector = mock[DelegationConnector]
  val periodKey = PeriodUtils.calculatePeriod()
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestPropertyDetailsPeriodController extends PropertyDetailsSupportingInfoController {
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

  "PropertyDetailsSupportingInfoController" must {

    "use correct DelegationConnector" in {
      PropertyDetailsSupportingInfoController.delegationConnector must be(FrontendDelegationConnector)
    }

    "propertyDetails" must {

      "not respond with NOT_FOUND when we dont pass an id" in {
        val result = route(FakeRequest(GET, "/ated/liability/create/supporting-info/view/1"))
        status(result.get) must not be (NOT_FOUND)
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

        "show the chargeable property details value view with no data" in {
          import utils.PeriodUtils._

          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)

          def formatDate(date: LocalDate): String = DateTimeFormat.forPattern("d MMMM y").withZone(DateTimeZone.forID("Europe/London")).print(date)

          val startDate = periodStartDate(periodKey)
          val endDate = periodEndDate(periodKey)

          getDataWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Do you have any supporting information to add?")
              
              document.getElementById("supportingInfo").attr("value") must be("")

              document.getElementById("submit").text() must be("Save and continue")
          }
        }

        "show the chargeable property details value view with existing data" in {
          val propertyDetailsPeriod = PropertyDetailsBuilder.getPropertyDetailsPeriodDatesLiable(new LocalDate("970-12-01"), new LocalDate("1999-03-02")).
            map(_.copy(supportingInfo = Some("supportingInfoTextAreaData")))

          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = propertyDetailsPeriod)
          getDataWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("supportingInfo").toString must include("supportingInfoTextAreaData")

              document.getElementById("submit").text() must be("Save and continue")
          }
        }
      }
    }

    "editFromSummary" must {

      "Authorised users" must {

        "show the chargeable property details value view with no data and add a back link to the summary page" in {
          import utils.PeriodUtils._

          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)

          def formatDate(date: LocalDate): String = DateTimeFormat.forPattern("d MMMM y").withZone(DateTimeZone.forID("Europe/London")).print(date)

          val startDate = periodStartDate(periodKey)
          val endDate = periodEndDate(periodKey)

          editFromSummary(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Do you have any supporting information to add?")

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

          val invalidData = "a" * 201
          val inputJson = Json.toJson(PropertyDetailsSupportingInfo(invalidData))
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson, None) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }
        "for valid data, return Forward to the summary page" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val inputJson = Json.toJson(PropertyDetailsSupportingInfo(""))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson, Some(propertyDetails)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/summary/")
          }
        }

        "for valid edit liability data forward to the Edit Liability Summary Page" in {
          val propertyDetails = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("1")
          val inputJson = Json.toJson(PropertyDetailsSupportingInfo(""))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUserEdit(inputJson, Some(propertyDetails), Some(AtedUtils.EDIT_SUBMITTED)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/1/change/summary")
          }
        }

        "for invalid agent, throw BAD_REQUEST"  in {
          val propertyDetails = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("1")
          val inputJson = Json.toJson(PropertyDetailsSupportingInfo(""))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithInvalidAgent(inputJson, Some(propertyDetails)) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }
      }

    }
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsPeriodController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestPropertyDetailsPeriodController.view("1").apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPropertyDetailsPeriodController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getDataWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestPropertyDetailsPeriodController.view(propertyDetails.id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getAuthorisedUserNone(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPropertyDetailsPeriodController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def editFromSummary(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestPropertyDetailsPeriodController.editFromSummary(propertyDetails.id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
  def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsPeriodController.save("1", 2015, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestPropertyDetailsPeriodController.save("1", 2015, None).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithInvalidAgent(inputJson: JsValue, propertyDetails: Option[PropertyDetails], mode: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockService.validateCalculateDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(true))
    when(mockService.saveDraftPropertyDetailsSupportingInfo(Matchers.eq("1"), Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(OK))
    when(mockService.calculateDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(Json.parse("""{"Reason":"Agent not Valid"}""")))))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsPeriodController.save("1", 2015, mode).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }

  def submitWithAuthorisedUser(inputJson: JsValue, propertyDetails: Option[PropertyDetails], mode: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.eq(AtedConstants.SelectedPreviousReturn))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(true)))
    when(mockService.validateCalculateDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(true))
    when(mockService.saveDraftPropertyDetailsSupportingInfo(Matchers.eq("1"), Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(OK))
    when(mockService.calculateDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(propertyDetails)))))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsPeriodController.save("1", 2015, mode).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }


  def submitWithAuthorisedUserEdit(inputJson: JsValue, propertyDetails: Option[PropertyDetails], mode: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.eq(AtedConstants.SelectedPreviousReturn))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.validateCalculateDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(true))
    when(mockService.saveDraftPropertyDetailsSupportingInfo(Matchers.eq("1"), Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(OK))
    when(mockService.calculateDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(propertyDetails)))))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsPeriodController.save("1", 2015, mode).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }

}
