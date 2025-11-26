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

import builders.{PropertyDetailsBuilder, SessionBuilder}
import config.ApplicationConfig
import controllers.auth.AuthAction
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.{JsValue, Json}
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

class PropertyDetailsAcquisitionControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockBackLinkCacheService: BackLinkCacheService = mock[BackLinkCacheService]
  val mockIsFullTaxPeriodController: IsFullTaxPeriodController = mock[IsFullTaxPeriodController]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsAcquisition]
  val mockHasBeenRevaluedController: PropertyDetailsHasBeenRevaluedController = mock[PropertyDetailsHasBeenRevaluedController]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testPropertyDetailsAcquisitionController: PropertyDetailsAcquisitionController = new PropertyDetailsAcquisitionController (
      mockMcc,
      mockAuthAction,
      mockIsFullTaxPeriodController,
      mockHasBeenRevaluedController,
      mockServiceInfoService,
      mockPropertyDetailsService,
      mockDataCacheService,
      mockBackLinkCacheService,
      injectedViewInstance
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsAcquisitionController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getDataWithAuthorisedUser(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any): Unit = {val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[Boolean](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsAcquisitionController.view(id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def editFromSummary(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any): Unit = {val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheService.fetchAndGetData[Boolean](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsAcquisitionController.editFromSummary(id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsAcquisitionController.save("1", periodKey, None).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(inputJson: JsValue)(test: Future[Result] => Any): Unit = {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.saveDraftPropertyDetailsAcquisition(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(OK))
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testPropertyDetailsAcquisitionController.save("1", periodKey, None)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))

      test(result)
    }
  }

  override def beforeEach(): Unit = {
                       
  }

  "PropertyDetailsAcquisitionController" must {
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

        "show the chargeable property details view if we have id and data" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(value = None)
          getDataWithAuthorisedUser("1", propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
              document.getElementsByTag("h1")
                .text() must include("In this chargeable period, have you bought or sold land, or extended an existing lease on the property, for £40,000 or more?")
              document.getElementsByClass("govuk-details__summary").text() must be("Why is the £40,000 level important?")
              document.getElementById("anAcquisition").text() contains "Yes"
              document.getElementById("anAcquisition-2").text() contains "Yes"
              document.getElementById("anAcquisition").hasAttr("checked") must be(false)
              document.getElementById("anAcquisition-2").hasAttr("checked") must be(false)


          }
        }

        "show the chargeable property details view if we have id and data even with a propertyDetails value" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          getDataWithAuthorisedUser("1", propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementsByTag("h1")
                .text() must include("In this chargeable period, have you bought or sold land, or extended an existing lease on the property, for £40,000 or more?")
              document.getElementsByClass("govuk-details__summary").text() must be("Why is the £40,000 level important?")
              document.getElementById("anAcquisition").text() contains "Yes"
              document.getElementById("anAcquisition-2").text() contains "Yes"
              document.getElementById("anAcquisition").hasAttr("checked") must be(true)
              document.getElementById("anAcquisition-2").hasAttr("checked") must be(false)

          }
        }
      }
    }

    "editFromSummary" must {

      "Authorised users" must {

        "show the owned before view if we id and data and add the summary back link" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          editFromSummary("1", propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(
                "In this chargeable period, have you bought or sold land, or extended an existing lease on the property, for £40,000 or more? - Submit and view your ATED returns - GOV.UK")
              document.getElementsByClass("govuk-back-link").text must be("Back")
              document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/liability/create/summary")

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

          val inputJson: JsValue = Json.parse( """{"anAcquisition": "2"}""")
          when(mockBackLinkCacheService.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "When the acquisition is true forward to the Has Been Revalued Page" in new Setup {
          when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Json.toJson(PropertyDetailsAcquisition(Some(true)))) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/has-been-revalued/view/")
          }
        }
        "When the acquisition is false forward to the Owned Before Page" in new Setup {
          when(mockBackLinkCacheService.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Json.toJson(PropertyDetailsAcquisition(Some(false)))) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/full-tax-period/view")
          }
        }
      }
    }
  }
}
