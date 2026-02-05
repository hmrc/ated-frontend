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

package controllers.editLiability

import java.util.UUID
import builders.{PropertyDetailsBuilder, SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import controllers.auth.AuthAction
import controllers.propertyDetails.PropertyDetailsTaxAvoidanceSchemeController
import models.{PropertyDetails, PropertyDetailsPeriod}

import java.time.LocalDate
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
import services.{BackLinkCacheService, DataCacheService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class EditLiabilityDatesLiableControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockBackLinkCacheService: BackLinkCacheService = mock[BackLinkCacheService]
  val mockPropertyDetailsTaxAvoidanceController: PropertyDetailsTaxAvoidanceSchemeController = mock[PropertyDetailsTaxAvoidanceSchemeController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.editLiability.editLiabilityDatesLiable]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testEditLiabilityDatesLiableController: EditLiabilityDatesLiableController = new EditLiabilityDatesLiableController(
      mockMcc,
      mockAuthAction,
      mockPropertyDetailsTaxAvoidanceController,
      mockServiceInfoService,
      mockPropertyDetailsService,
      mockDataCacheService,
      mockBackLinkCacheService,
      injectedViewInstance
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testEditLiabilityDatesLiableController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getDataWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testEditLiabilityDatesLiableController.view(propertyDetails.id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testEditLiabilityDatesLiableController.save("1", periodKey)
        .apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(formBody: List[(String, String)])(test: Future[Result] => Any): Unit = {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.saveDraftPropertyDetailsDatesLiable(ArgumentMatchers.eq("1"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(OK))
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testEditLiabilityDatesLiableController.save("1", periodKey)
        .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formBody: _*), userId))
      test(result)
    }

  }


  override def beforeEach(): Unit = {

    reset(mockPropertyDetailsService)
        reset(mockDelegationService)
        reset(mockDataCacheService)
        reset(mockBackLinkCacheService)
        reset(mockPropertyDetailsTaxAvoidanceController)

      }

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
              document.title() must be(TitleBuilder.buildTitle("Enter the dates this change applies to"))
          }
        }

        "show the chargeable property details value view with existing data" in new Setup {
          val propertyDetailsPeriod: Option[PropertyDetailsPeriod] = PropertyDetailsBuilder
            .getPropertyDetailsPeriodDatesLiable(LocalDate.parse("2015-05-01"), LocalDate.parse("2016-02-23")).
            map(_.copy(isFullPeriod = Some(false), isInRelief = Some(false)))

          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = propertyDetailsPeriod)
          getDataWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("startDate.day").attr("value") must be("1")
              document.getElementById("startDate.month").attr("value") must be("5")
              document.getElementById("startDate.year").attr("value") must be("2015")
              document.getElementById("endDate.day").attr("value") must be("23")
              document.getElementById("endDate.month").attr("value") must be("2")
              document.getElementById("endDate.year").attr("value") must be("2016")
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")

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
          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("backLink")))
          submitWithAuthorisedUser(Nil) {
            result =>
              status(result) must be(BAD_REQUEST)

              val document = Jsoup.parse(contentAsString(result))
              assert(document.getElementById("startDate-error").text() === "Error: The liability start date cannot be empty")
              assert(document.getElementById("endDate-error").text() === "Error: The liability end date cannot be empty")
              assert(document.getElementsByClass("govuk-list govuk-error-summary__list").text() contains "The liability start date cannot be empty")
              assert(document.getElementsByClass("govuk-list govuk-error-summary__list").text() contains "The liability end date cannot be empty")
          }
        }

        "for invalid data, -- empty start and end dates - return BAD_REQUEST" in new Setup {
          val startAndEndDatesList = List(
            ("startDate.day", ""),
            ("startDate.month", ""),
            ("startDate.year", ""),
            ("endDate.day", ""),
            ("endDate.month", ""),
            ("endDate.year", ""))
          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(startAndEndDatesList) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date cannot be empty")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability end date cannot be empty")
          }
        }

        "for invalid data, -- empty day values for start and end dates - return BAD_REQUEST" in new Setup {
          val startAndEndDatesList = List(
            ("startDate.day", ""),
            ("startDate.month", "6"),
            ("startDate.year", "2015"),
            ("endDate.day", ""),
            ("endDate.month", "8"),
            ("endDate.year", "2015"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(startAndEndDatesList) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date must include the day")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability end date must include the day")
          }
        }

        "for invalid data, -- empty month values for start and end dates - return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", ""),
            ("startDate.year", "2015"),
            ("endDate.day", "1"),
            ("endDate.month", ""),
            ("endDate.year", "2015"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date must include the month")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability end date must include the month")
          }
        }

        "for invalid data, -- empty year values for start and end dates - return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "6"),
            ("startDate.year", ""),
            ("endDate.day", "1"),
            ("endDate.month", "8"),
            ("endDate.year", ""))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date must include the year")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date must include the year")
          }
        }

        "for invalid data, -- empty day & month values for start and end dates - return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", ""),
            ("startDate.month", ""),
            ("startDate.year", "2016"),
            ("endDate.day", ""),
            ("endDate.month", ""),
            ("endDate.year", "2016"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date must include the day and month")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability end date must include the day and month")
          }
        }

        "for invalid data, -- empty day & year values for start and end dates - return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", ""),
            ("startDate.month", "6"),
            ("startDate.year", ""),
            ("endDate.day", ""),
            ("endDate.month", "8"),
            ("endDate.year", ""))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date must include the day and year")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability end date must include the day and year")
          }
        }

        "for invalid data, -- empty month & year values for start and end dates - return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", ""),
            ("startDate.year", ""),
            ("endDate.day", "1"),
            ("endDate.month", ""),
            ("endDate.year", ""))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date must include the month and year")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability end date must include the month and year")
          }
        }

        "for invalid data, -- invalid day value start and end dates - return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "41"),
            ("startDate.month", "6"),
            ("startDate.year", "2016"),
            ("endDate.day", "99"),
            ("endDate.month", "8"),
            ("endDate.year", "2016"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a day for the liability start date between 1 and 31")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a day for the liability end date between 1 and 31")
          }
        }

        "for invalid data, -- invalid month value start and end dates - return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "16"),
            ("startDate.year", "2016"),
            ("endDate.day", "1"),
            ("endDate.month", "18"),
            ("endDate.year", "2016"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a month for the liability start date between 1 and 12")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a month for the liability end date between 1 and 12")
          }
        }

        "for invalid data, -- value that does not contain 4-digit value for year for start and end dates - return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "6"),
            ("startDate.year", "20167"),
            ("endDate.day", "1"),
            ("endDate.month", "8"),
            ("endDate.year", "201333"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Year for the liability start date must be 4 digits")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Year for the liability end date must be 4 digits")
          }
        }

        "for invalid data, -- Relief Start and End dates have incorrect combination of day & month fields - return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "30"),
            ("startDate.month", "2"),
            ("startDate.year", "2016"),
            ("endDate.day", "31"),
            ("endDate.month", "9"),
            ("endDate.year", "2016"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Invalid day and month for the liability start date")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Invalid day and month for the liability end date")
          }
        }

        "for invalid data, -- Relief Start and End dates have invalid values - return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "30"),
            ("startDate.month", "2"),
            ("startDate.year", "abcd"),
            ("endDate.day", "31"),
            ("endDate.month", "9"),
            ("endDate.year", "defg"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date must be a valid date")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability end date must be a valid date")
          }
        }

        "for invalid data, -- Relief Start and End dates have invalid values, -- return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "29"),
            ("startDate.month", "2"),
            ("startDate.year", "2017"),
            ("endDate.day", "29"),
            ("endDate.month", "2"),
            ("endDate.year", "2017"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date must be a valid date")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability end date must be a valid date")
          }
        }

        "for invalid data, -- Relief Start and End dates have inappropriate order and range, -- return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "25"),
            ("startDate.month", "2"),
            ("startDate.year", "2024"),
            ("endDate.day", "2"),
            ("endDate.month", "2"),
            ("endDate.year", "2024"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date cannot be after this chargeable period")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability end date cannot be before the liability start date")
          }
        }

        "for invalid data, -- Relief Start and End dates both out of chargeable period, -- return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "6"),
            ("startDate.year", "2020"),
            ("endDate.day", "1"),
            ("endDate.month", "8"),
            ("endDate.year", "2020"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date cannot be after this chargeable period")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability end date cannot be after this chargeable period")
          }
        }

        "for invalid data, -- Only one of Relief Start and End dates is out of chargeable period and the other with invalid date, -- return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("startDate.day", "23"),
            ("startDate.month", "04"),
            ("startDate.year", "2022"),
            ("endDate.day", "45"),
            ("endDate.month", "07"),
            ("endDate.year", "2022"))

          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("")))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The liability start date cannot be after this chargeable period")
              document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a day for the liability end date between 1 and 31")
          }
        }

        "for valid data forward to the TaxAvoidance Page" in new Setup {
          val formBody = List(
            ("startDate.day", "1"),
            ("startDate.month", "6"),
            ("startDate.year", "2015"),
            ("endDate.day", "1"),
            ("endDate.month", "8"),
            ("endDate.year", "2015"))

          when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/tax-avoidance/view")
          }
        }
      }
    }
  }

