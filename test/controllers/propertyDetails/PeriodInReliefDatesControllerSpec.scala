/*
 * Copyright 2020 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import testhelpers.MockAuthUtil
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, PeriodUtils}

import scala.concurrent.Future

class PeriodInReliefDatesControllerSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]

  val periodKey: Int = PeriodUtils.calculatePeriod()

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testPeriodInReliefDatesController: PeriodInReliefDatesController = new PeriodInReliefDatesController(
      mockMcc,
      mockAuthAction,
      mockDataCacheConnector,
      mockPropertyDetailsService,
      mockBackLinkCacheConnector
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val periodKey: Int = 2016
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPeriodInReliefDatesController.add("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def addDataWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
      val periodKey: Int = 2016
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = testPeriodInReliefDatesController.add(propertyDetails.id, periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
      val periodKey: Int = 2016
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPeriodInReliefDatesController.save("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(formBody: List[(String, String)], propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      when(mockPropertyDetailsService.addDraftPropertyDetailsDatesInRelief(ArgumentMatchers.eq("1"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(OK))
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testPeriodInReliefDatesController.save("1", periodKey)
        .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formBody: _*), userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {

reset(mockDelegationService)
    reset(mockDataCacheConnector)
    reset(mockPropertyDetailsService)
    reset(mockBackLinkCacheConnector)
  }

  "PeriodInReliefDatesController" must {
    "add" must {
      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in new Setup {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the date selection for the choose reliefs" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)

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

        "be redirected to the login page" in new Setup {
          saveWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {
        "for invalid data, return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          submitWithAuthorisedUser(Nil, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for valid data when adding a period return to the Periods Summary Page" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)
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
}
