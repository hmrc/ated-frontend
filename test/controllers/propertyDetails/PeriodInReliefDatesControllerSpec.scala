/*
 * Copyright 2023 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, PeriodUtils}
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class PeriodInReliefDatesControllerSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.periodInReliefDates]

  val periodKey: Int = PeriodUtils.calculatePeakStartYear()

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testPeriodInReliefDatesController: PeriodInReliefDatesController = new PeriodInReliefDatesController(
      mockMcc,
      mockAuthAction,
      mockServiceInfoService,
      mockDataCacheConnector,
      mockPropertyDetailsService,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val periodKey: Int = 2016
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      val result = testPeriodInReliefDatesController.add("1", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def addDataWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
      val periodKey: Int = 2016
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
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
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
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

        "for invalid data, -- empty start and end dates - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", ""),
            ("startDate.month", ""),
            ("startDate.year", ""),
            ("endDate.day", ""),
            ("endDate.month", ""),
            ("endDate.year", ""))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief start date cannot be empty")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief end date cannot be empty")
          }
        }

        "for invalid data, -- empty day values for start and end dates - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", ""),
            ("startDate.month", "6"),
            ("startDate.year", "2015"),
            ("endDate.day", ""),
            ("endDate.month", "8"),
            ("endDate.year", "2015"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief start date must include the day")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief end date must include the day")
          }
        }

        "for invalid data, -- empty month values for start and end dates - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", ""),
            ("startDate.year", "2015"),
            ("endDate.day", "1"),
            ("endDate.month", ""),
            ("endDate.year", "2015"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief start date must include the month")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief end date must include the month")
          }
        }

        "for invalid data, -- empty year values for start and end dates - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "6"),
            ("startDate.year", ""),
            ("endDate.day", "1"),
            ("endDate.month", "8"),
            ("endDate.year", ""))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief start date must include the year")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief end date must include the year")
          }
        }

        "for invalid data, -- empty day & month values for start and end dates - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", ""),
            ("startDate.month", ""),
            ("startDate.year", "2016"),
            ("endDate.day", ""),
            ("endDate.month", ""),
            ("endDate.year", "2016"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief start date must include the day and month")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief end date must include the day and month")
          }
        }

        "for invalid data, -- empty day & year values for start and end dates - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", ""),
            ("startDate.month", "6"),
            ("startDate.year", ""),
            ("endDate.day", ""),
            ("endDate.month", "8"),
            ("endDate.year", ""))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief start date must include the day and year")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief end date must include the day and year")
          }
        }

        "for invalid data, -- empty month & year values for start and end dates - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", ""),
            ("startDate.year", ""),
            ("endDate.day", "1"),
            ("endDate.month", ""),
            ("endDate.year", ""))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief start date must include the month and year")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief end date must include the month and year")
          }
        }

        "for invalid data, -- invalid day value start and end dates - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", "41"),
            ("startDate.month", "6"),
            ("startDate.year", "2016"),
            ("endDate.day", "99"),
            ("endDate.month", "8"),
            ("endDate.year", "2016"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a day for relief start date between 1 and 31")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a day for relief end date between 1 and 31")
          }
        }

        "for invalid data, -- invalid month value start and end dates - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "16"),
            ("startDate.year", "2016"),
            ("endDate.day", "1"),
            ("endDate.month", "18"),
            ("endDate.year", "2016"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a month for relief start date between 1 and 12")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a month for relief end date between 1 and 12")
          }
        }

        "for invalid data, -- value that does not contain 4-digit value for year for start and end dates - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "6"),
            ("startDate.year", "20167"),
            ("endDate.day", "1"),
            ("endDate.month", "8"),
            ("endDate.year", "201333"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Year for relief start date must be 4 digits")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Year for relief end date must be 4 digits")
          }
        }

        "for invalid data, -- Relief Start and End dates have incorrect combination of day & month fields - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", "30"),
            ("startDate.month", "2"),
            ("startDate.year", "2016"),
            ("endDate.day", "31"),
            ("endDate.month", "9"),
            ("endDate.year", "2016"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Invalid day and month for relief start date")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Invalid day and month for relief end date")
          }
        }

        "for invalid data, -- Relief Start and End dates have invalid values - return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", "30"),
            ("startDate.month", "2"),
            ("startDate.year", "abcd"),
            ("endDate.day", "31"),
            ("endDate.month", "9"),
            ("endDate.year", "defg"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief start date must be a valid date")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief end date must be a valid date")
          }
        }

        "for invalid data, -- Relief Start and End dates have invalid values, -- return BAD_REQUEST" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val formBody = List(
            ("startDate.day", "29"),
            ("startDate.month", "2"),
            ("startDate.year", "2017"),
            ("endDate.day", "29"),
            ("endDate.month", "2"),
            ("endDate.year", "2017"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief start date must be a valid date")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Relief end date must be a valid date")
          }
        }

        "for valid data with too old period (before 2019) when adding a period return to the Periods Summary Page" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)

          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "8"),
            ("startDate.year", "2014"),
            ("endDate.day", "1"),
            ("endDate.month", "7"),
            ("endDate.year", "2014"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The start date cannot be before this chargeable period")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The relief end date cannot be before the liability start date and must be within this chargeable period")
          }
        }

        "for valid data with future dates for both when adding a period return to the Periods Summary Page" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None, periodKey = 2023)

          val formBody = List(
            ("startDate.day", "15"),
            ("startDate.month", "2"),
            ("startDate.year", "2022"),
            ("endDate.day", "16"),
            ("endDate.month", "2"),
            ("endDate.year", "2022"))
          submitWithAuthorisedUser(formBody, propertyDetails) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The start date cannot be after this chargeable period")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The end date cannot be after this chargeable period")
          }
        }
      }
    }
  }
}
