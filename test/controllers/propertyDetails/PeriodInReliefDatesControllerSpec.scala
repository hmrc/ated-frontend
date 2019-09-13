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

import builders.{PropertyDetailsBuilder, SessionBuilder, TitleBuilder}
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models._
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, MockAuthUtil, PeriodUtils}

import scala.concurrent.Future

class PeriodInReliefDatesControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockService: PropertyDetailsService = mock[PropertyDetailsService]

  val periodKey: Int = PeriodUtils.calculatePeriod()
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]


  object TestPropertyDetailsPeriodController extends PeriodInReliefDatesController {
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


  "PeriodInReliefDatesController" should {

    "use correct delegationService" in {
      PeriodInReliefDatesController.delegationService mustBe DelegationService
    }
    "use correct propertyDetailsService" in {
      PeriodInReliefDatesController.propertyDetailsService mustBe PropertyDetailsService
    }
    "use correct backLinkCacheConnector" in {
      PeriodInReliefDatesController.backLinkCacheConnector mustBe BackLinkCacheConnector
    }
    "use correct dataCacheConnector" in {
      PeriodInReliefDatesController.dataCacheConnector mustBe DataCacheConnector
    }
    "use correct controllerId" in {
      PeriodInReliefDatesController.controllerId mustBe "PeriodInReliefDatesController"
    }

  }

  "PeriodInReliefDatesController" must {

    "use correct DelegationService" in {
      PeriodChooseReliefController.delegationService must be(DelegationService)
    }

    "add" must {

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

        "show the date selection for the choose reliefs" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)

          addDataWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be (TitleBuilder.buildTitle("Add the dates when the property was in relief and was not liable for an ATED charge"))
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
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          submitWithAuthorisedUser(Nil, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for valid data when adding a period return to the Periods Summary Page" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "6"),
            ("startDate.year", "2015"),
            ("endDate.day", "1"),
            ("endDate.month", "8"),
            ("endDate.year", "2015"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/periods-in-relief/view/1")
          }
        }
      }

    }
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val periodKey: Int = 2016
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestPropertyDetailsPeriodController.add("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def addDataWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val periodKey: Int = 2016
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = TestPropertyDetailsPeriodController.add(propertyDetails.id, periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
    val periodKey: Int = 2016
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestPropertyDetailsPeriodController.save("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(formBody: List[(String, String)], propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val periodKey: Int = 2015
    val userId = s"user-${UUID.randomUUID}"
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    when(mockService.addDraftPropertyDetailsDatesInRelief(Matchers.eq("1"), Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(OK))
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestPropertyDetailsPeriodController.save("1", periodKey)
      .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formBody: _*), userId))
    test(result)
  }

}
