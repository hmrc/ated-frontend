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
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{SEE_OTHER, redirectLocation, status, _}
import services._
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.propertyDetails.dateCouncilRegistered
import play.twirl.api.HtmlFormat

import scala.concurrent.Future
import models.{DateCouncilRegistered, DateCouncilRegisteredKnown, DateFirstOccupied, DateFirstOccupiedKnown}
import java.time.LocalDate
import play.api.libs.json.{JsValue, Json}
import utils.AtedConstants.{NewBuildCouncilRegisteredDate, NewBuildFirstOccupiedDate, NewBuildFirstOccupiedDateKnown}

class DateCouncilRegisteredControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MockitoSugar with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: dateCouncilRegistered = app.injector.instanceOf[views.html.propertyDetails.dateCouncilRegistered]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val dateCouncilRegisteredKnownController: DateCouncilRegisteredController = new DateCouncilRegisteredController(
      mockMcc,
      mockAuthAction,
      mockServiceInfoService,
      mockPropertyDetailsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    val periodKey: Int = 2015

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Any = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = dateCouncilRegisteredKnownController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(test: Future[Result] => Any): Any = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      noDelegationModelAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HtmlFormat.empty))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDataCacheConnector.fetchAndGetFormData[DateCouncilRegisteredKnown](ArgumentMatchers.eq(AtedConstants.NewBuildCouncilRegisteredDateKnown))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(DateCouncilRegisteredKnown(None))))
      when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
        Future.successful(PropertyDetailsCacheSuccessResponse(PropertyDetailsBuilder.getPropertyDetails("1")))
      }
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = dateCouncilRegisteredKnownController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithAuthorisedUser(inputJson: JsValue, test: Future[Result] => Any): Any = {

      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      noDelegationModelAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HtmlFormat.empty))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDataCacheConnector.saveFormData[DateCouncilRegistered](ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(null))
      when(mockDataCacheConnector.fetchAndGetFormData[DateCouncilRegisteredKnown](ArgumentMatchers.eq(AtedConstants.NewBuildCouncilRegisteredDateKnown))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(DateCouncilRegisteredKnown(Some(true)))))
      when(mockDataCacheConnector.fetchAndGetFormData[DateFirstOccupied](ArgumentMatchers.eq(NewBuildFirstOccupiedDate))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(DateFirstOccupied(Some(LocalDate.now())))))
      when(mockDataCacheConnector.fetchAndGetFormData[DateCouncilRegistered](ArgumentMatchers.eq(NewBuildCouncilRegisteredDate))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(DateCouncilRegistered(Some(LocalDate.now())))))
      when(mockPropertyDetailsService.saveDraftPropertyDetailsNewBuildDates(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
        Future.successful(1)}
      when(mockDataCacheConnector.fetchAndGetFormData[DateFirstOccupiedKnown](ArgumentMatchers.eq(NewBuildFirstOccupiedDateKnown))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(DateFirstOccupiedKnown(Some(true)))))
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      val result = dateCouncilRegisteredKnownController.save("1", 2016, Some("mode")).apply(SessionBuilder.buildRequestWithSession(userId).withJsonBody(inputJson))
      test(result)
    }
  }

  "DateCouncilRegisteredKnownController" must {

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

        "show When did the local council register the property for council tax? page" in new Setup {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("When did the local council register the property for council tax?"))
          }
        }
      }
    }

    "Authorised users when" must {

      "save function called with valid date - Redirect to next page" in new Setup {

        val inputJsonWithValidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "02", "dateCouncilRegistered.month": "05", "dateCouncilRegistered.year": "2016", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithValidDate, {
          result =>
            status(result) must be(SEE_OTHER)
           })
      }

      "save function called with invalid dates - empty date fields - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "", "dateCouncilRegistered.month": "", "dateCouncilRegistered.year": "", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the local council registered the property for council tax cannot be empty")
        })
      }

      "save function called with invalid dates - mising day field - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "", "dateCouncilRegistered.month": "05", "dateCouncilRegistered.year": "2016", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the local council registered the property for council tax must include the day")
        })
      }

      "save function called with invalid dates - mising month field - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "02", "dateCouncilRegistered.month": "", "dateCouncilRegistered.year": "2016", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the local council registered the property for council tax must include the month")
        })
      }

      "save function called with invalid dates - mising year field - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "02", "dateCouncilRegistered.month": "05", "dateCouncilRegistered.year": "", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the local council registered the property for council tax must include the year")
        })
      }

      "save function called with invalid dates - mising day and month fields - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "", "dateCouncilRegistered.month": "", "dateCouncilRegistered.year": "2016", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the local council registered the property for council tax must include the day and month")
        })
      }

      "save function called with invalid dates - mising day and year fields - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "", "dateCouncilRegistered.month": "05", "dateCouncilRegistered.year": "", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the local council registered the property for council tax must include the day and year")
        })
      }

      "save function called with invalid dates - mising month and year fields - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "02", "dateCouncilRegistered.month": "", "dateCouncilRegistered.year": "", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the local council registered the property for council tax must include the month and year")
        })
      }

      "save function called with invalid dates - invalid value for day field - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "32", "dateCouncilRegistered.month": "05", "dateCouncilRegistered.year": "2016", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a day for date when the local council registered the property for council tax between 1 and 31")
        })
      }

      "save function called with invalid dates - invalid value for month field - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "02", "dateCouncilRegistered.month": "23", "dateCouncilRegistered.year": "2016", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a month for date when the local council registered the property for council tax between 1 and 12")
        })
      }

      "save function called with invalid dates - value that does not contain 4-digit value for year field - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "02", "dateCouncilRegistered.month": "05", "dateCouncilRegistered.year": "20344", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Year for date when the local council registered the property for council tax must be 4 digits")
        })
      }

      "save function called with invalid dates - value has incorrect combination of day & month fields - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          """{"dateCouncilRegistered.day": "31", "dateCouncilRegistered.month": "02", "dateCouncilRegistered.year": "2016", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Invalid day and month for date when the local council registered the property for council tax")
        })
      }

      "save function called with invalid dates - future date value - must return BAD_REQUEST" in new Setup {

        val futureYear = LocalDate.now.plusYears(1).getYear
        val inputJsonWithInvalidDate: JsValue = Json.parse(
          s"""{"dateCouncilRegistered.day": "02", "dateCouncilRegistered.month": "05", "dateCouncilRegistered.year": ${futureYear}, "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the local council registered the property for council tax cannot be in the future")
        })
      }

      "save function called with invalid dates - invalid chars - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          s"""{"dateCouncilRegistered.day": "02", "dateCouncilRegistered.month": "05", "dateCouncilRegistered.year": "abcd", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the local council registered the property for council tax must be a valid date")
        })
      }

      "save function called with invalid dates - invalid date 29/02 - must return BAD_REQUEST" in new Setup {

        val inputJsonWithInvalidDate: JsValue = Json.parse(
          s"""{"dateCouncilRegistered.day": "29", "dateCouncilRegistered.month": "02", "dateCouncilRegistered.year": "2027", "periodKey": 2016}""".stripMargin)
        saveWithAuthorisedUser(inputJsonWithInvalidDate, {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the local council registered the property for council tax must be a valid date")
        })
      }
    }
  }
}
