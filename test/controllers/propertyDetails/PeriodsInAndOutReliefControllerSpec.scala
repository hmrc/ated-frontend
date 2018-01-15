/*
 * Copyright 2018 HM Revenue & Customs
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
import utils.{AtedConstants, AtedUtils, PeriodUtils}

import scala.concurrent.Future

class PeriodsInAndOutReliefControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockService = mock[PropertyDetailsService]
  val mockDelegationConnector = mock[DelegationConnector]
  val periodKey = PeriodUtils.calculatePeriod()
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestPropertyDetailsPeriodController extends PeriodsInAndOutReliefController {
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


  "PeriodsInAndOutReliefController" must {

    "use correct DelegationConnector" in {
      PeriodsInAndOutReliefController.delegationConnector must be(FrontendDelegationConnector)
    }

    "propertyDetails" must {

      "not respond with NOT_FOUND when we dont pass an id" in {
        val result = route(FakeRequest(GET, "/ated/liability/create/periods-in-relief/view/1"))
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

          def formatDate(date: LocalDate): String = DateTimeFormat.forPattern("d MMMM yyyy").withZone(DateTimeZone.forID("Europe/London")).print(date)

          val startDate = periodStartDate(periodKey)
          val endDate = periodEndDate(periodKey)

          getDataWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Add periods when the property was in relief and when it was liable for an ATED charge"))

              document.getElementById("submit").text() must be("Save and continue")
          }
        }

      }
    }

    "delete" must {

      "for valid data delete the relevant period and forward to the view" in {
        deleteWithAuthorisedUser() {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Add periods when the property was in relief and when it was liable for an ATED charge"))

            document.getElementById("submit").text() must be("Save and continue")
        }
      }

    }

    "save" must {
      "unauthorised users" must {

        "be redirected to the login page" in {
          continueWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "for valid data forward to the TaxAvoidance Page" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          continueWithAuthorisedUser() {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/tax-avoidance/view")
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
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
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

  def deleteWithAuthorisedUser()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)
    when(mockService.deleteDraftPropertyDetailsPeriod(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(propertyDetails))
    val result = TestPropertyDetailsPeriodController.deletePeriod("1", new LocalDate(s"2015-5-1")).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsPeriodController.continue("1", 2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestPropertyDetailsPeriodController.continue("1", 2015).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def continueWithAuthorisedUser()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsPeriodController.continue("1", 2015).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }
}
