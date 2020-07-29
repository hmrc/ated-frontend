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
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class PeriodDatesLiableControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockPropertyDetailsTaxAvoidanceController: PropertyDetailsTaxAvoidanceController = mock[PropertyDetailsTaxAvoidanceController]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.periodDatesLiable]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testPeriodDatesLiableController: PeriodDatesLiableController = new PeriodDatesLiableController (
      mockMcc,
      mockAuthAction,
      mockPropertyDetailsTaxAvoidanceController,
      mockServiceInfoService,
      mockPropertyDetailsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testPeriodDatesLiableController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getDataWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)

      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails
      (ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      val result = testPeriodDatesLiableController.view(propertyDetails.id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def addDataWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
      val periodKey: Int = 2016
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)

      val result = testPeriodDatesLiableController.add(propertyDetails.id, periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPeriodDatesLiableController.save("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(formBody: List[(String, String)], propDetails: PropertyDetails)(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails
      (ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propDetails)))
      when(mockPropertyDetailsService.saveDraftPropertyDetailsDatesLiable(ArgumentMatchers.eq("1"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(OK))
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testPeriodDatesLiableController.save("1", periodKey)
        .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formBody: _*), userId))
      test(result)
    }

    def submitAddWithAuthorisedUser(formBody: List[(String, String)], propDetails: PropertyDetails)(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propDetails)))
      when(mockPropertyDetailsService.addDraftPropertyDetailsDatesLiable(ArgumentMatchers.eq("1"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(OK))
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testPeriodDatesLiableController.save("1", periodKey, Some("add"))
        .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formBody: _*), userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
  }

  "PeriodDatesLiableController" must {
    "view" must {

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

        "show the chargeable property details value view with no data" in new Setup {

          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)

          getDataWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Enter the dates the property was liable for an ATED charge"))
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
          }
        }

        "show the chargeable property details value view with existing data" in new Setup {
          val propertyDetailsPeriod: Option[PropertyDetailsPeriod] = PropertyDetailsBuilder
            .getPropertyDetailsPeriodDatesLiable(new LocalDate("2015-5-1"), new LocalDate("2016-2-23")).
            map(_.copy(isFullPeriod = Some(false), isInRelief = Some(false)))

          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = propertyDetailsPeriod)
          getDataWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("startDate-day").attr("value") must be("1")
              document.getElementById("startDate-month").attr("value") must be("5")
              document.getElementById("startDate-year").attr("value") must be("2015")
              document.getElementById("endDate-day").attr("value") must be("23")
              document.getElementById("endDate-month").attr("value") must be("2")
              document.getElementById("endDate-year").attr("value") must be("2016")

          }
        }
      }
    }

    "add" must {
      "show the chargeable property details value view with no data" in new Setup {

        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)

        addDataWithAuthorisedUser(propertyDetails) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Add the dates the property was liable for an ATED charge"))
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
          when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Nil, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for valid data forward to the TaxAvoidance Page" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "6"),
            ("startDate.year", "2015"),
            ("endDate.day", "1"),
            ("endDate.month", "8"),
            ("endDate.year", "2015"))

          when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/tax-avoidance/view")
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

          when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitAddWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/periods-in-relief/view/1")
          }
        }
      }
    }
  }
}
