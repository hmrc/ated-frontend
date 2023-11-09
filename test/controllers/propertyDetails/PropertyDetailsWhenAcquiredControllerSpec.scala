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

import builders.{PropertyDetailsBuilder, SessionBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import java.util.UUID
import scala.concurrent.Future

class PropertyDetailsWhenAcquiredControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockPropertydDetailsValueAcquiredController: PropertyDetailsValueAcquiredController = mock[PropertyDetailsValueAcquiredController]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsWhenAcquired]

  val periodKey: Int = 2016

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testPropertyDetailsWhenAcquiredController: PropertyDetailsWhenAcquiredController = new PropertyDetailsWhenAcquiredController(
      mockMcc,
      mockAuthAction,
      mockPropertydDetailsValueAcquiredController,
      mockServiceInfoService,
      mockPropertyDetailsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsWhenAcquiredController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getDataWithAuthorisedUser(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Html("")))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsWhenAcquiredController.view(id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }


    def saveWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsWhenAcquiredController.save("1", periodKey, None)
        .apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(formBody: List[(String, String)])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Html("")))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockPropertyDetailsService.saveDraftPropertyDetailsWhenAcquiredDates(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(OK))
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testPropertyDetailsWhenAcquiredController.save("1", periodKey, None)
        .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formBody: _*), userId))
      test(result)
    }
  }


  override def beforeEach(): Unit = {
  }

  "PropertyDetailsWhenAcquiredController" must {
    "propertyDetails" must {
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
        "show the when acquired page view if we id and data" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          getDataWithAuthorisedUser("1", propertyDetails) {
            result =>
              status(result) must be(OK)
          }
        }
      }
    }

    "save" must {
      "redirect to the login page when called by an unauthorised user" in new Setup {
        saveWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }

    "Authorised users" should {
      "received a BAD REQUEST when they enter an invalid date " in new Setup {
        val formBody = List(
          ("acquiredDate.day", "AA"),
          ("acquiredDate.month", "AA"),
          ("acquiredDate.year", "AA")
        )
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
        }
      }

      "be redirected to the value acquired property page" in new Setup {
        val formBody = List(
          ("acquiredDate.day", "1"),
          ("acquiredDate.month", "5"),
          ("acquiredDate.year", "2016"))
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/liability/create/value-acquired/view")
        }
      }

      "save function called with invalid dates - empty date fields - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", ""),
          ("acquiredDate.month", ""),
          ("acquiredDate.year", ""))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the property was acquired cannot be empty")
        }
      }

      "save function called with invalid dates - mising day field - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", ""),
          ("acquiredDate.month", "05"),
          ("acquiredDate.year", "2016"))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the property was acquired must include the day")
        }
      }

      "save function called with invalid dates - mising month field - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", "15"),
          ("acquiredDate.month", ""),
          ("acquiredDate.year", "2016"))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the property was acquired must include the month")
        }
      }

      "save function called with invalid dates - mising year field - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", "02"),
          ("acquiredDate.month", "05"),
          ("acquiredDate.year", ""))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the property was acquired must include the year")
        }
      }

      "save function called with invalid dates - mising day and month fields - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", ""),
          ("acquiredDate.month", ""),
          ("acquiredDate.year", "2016"))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the property was acquired must include the day and month")
        }
      }

      "save function called with invalid dates - mising day and year fields - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", ""),
          ("acquiredDate.month", "05"),
          ("acquiredDate.year", ""))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the property was acquired must include the day and year")
        }
      }

      "save function called with invalid dates - mising month and year fields - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", "02"),
          ("acquiredDate.month", ""),
          ("acquiredDate.year", ""))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the property was acquired must include the month and year")
        }
      }

      "save function called with invalid dates - invalid value for day field - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", "32"),
          ("acquiredDate.month", "05"),
          ("acquiredDate.year", "2016"))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a day for date when the property was acquired between 1 and 31")
        }
      }

      "save function called with invalid dates - invalid value for month field - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", "02"),
          ("acquiredDate.month", "23"),
          ("acquiredDate.year", "2016"))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a month for date when the property was acquired between 1 and 12")
        }
      }

      "save function called with invalid dates - value that does not contain 4-digit value for year field - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", "02"),
          ("acquiredDate.month", "05"),
          ("acquiredDate.year", "20334"))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Year for date when the property was acquired must be 4 digits")
        }
      }

      "save function called with invalid dates - value has incorrect combination of day & month fields - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", "31"),
          ("acquiredDate.month", "02"),
          ("acquiredDate.year", "2016"))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Invalid day and month for date when the property was acquired")
        }
      }

      "save function called with invalid dates - future date value - must return BAD_REQUEST" in new Setup {

        val futureYear = LocalDate.now.plusYears(1).getYear
        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", "02"),
          ("acquiredDate.month", "05"),
          ("acquiredDate.year", s"$futureYear"))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The date when the property was acquired must be in the past")
        }
      }

      "save function called with invalid dates - invalid chars - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", "02"),
          ("acquiredDate.month", "05"),
          ("acquiredDate.year", "abcd"))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the property was acquired must be a valid date")
        }
      }

      "save function called with invalid dates - invalid date 29/02 - must return BAD_REQUEST" in new Setup {

        val formBody = List(
          ("periodKey", "2016"),
          ("acquiredDate.day", "29"),
          ("acquiredDate.month", "02"),
          ("acquiredDate.year", "2027"))

        submitWithAuthorisedUser(formBody) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date when the property was acquired must be a valid date")
        }
      }

    }
  }
}
