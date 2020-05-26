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

import builders._
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import controllers.editLiability.EditLiabilitySummaryController
import testhelpers.MockAuthUtil
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AtedConstants, AtedUtils, PeriodUtils}

import scala.concurrent.Future

class PropertyDetailsSupportingInfoControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockEditLiabilitySummaryController: EditLiabilitySummaryController = mock[EditLiabilitySummaryController]
  val mockPropertyDetailsSummaryController: PropertyDetailsSummaryController = mock[PropertyDetailsSummaryController]

  val periodKey: Int = PeriodUtils.calculatePeakStartYear()

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testPropertyDetailsSupportingInfoController: PropertyDetailsSupportingInfoController = new PropertyDetailsSupportingInfoController(
      mockMcc,
      mockAuthAction,
      mockEditLiabilitySummaryController,
      mockPropertyDetailsSummaryController,
      mockPropertyDetailsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsSupportingInfoController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getDataWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsSupportingInfoController.view(propertyDetails.id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def editFromSummary(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsSupportingInfoController.editFromSummary(propertyDetails.id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
    def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsSupportingInfoController.save("1", periodKey, None).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithInvalidAgent(inputJson: JsValue, propertyDetails: Option[PropertyDetails], mode: Option[String] = None)(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.validateCalculateDraftPropertyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(true))
      when(mockPropertyDetailsService.saveDraftPropertyDetailsSupportingInfo(ArgumentMatchers.eq("1"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(OK))
      when(mockPropertyDetailsService.calculateDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(Json.parse("""{"Reason":"Agent not Valid"}""")))))
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testPropertyDetailsSupportingInfoController.save("1", periodKey, mode)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }

    def submitWithAuthorisedUser(inputJson: JsValue, propertyDetails: Option[PropertyDetails], mode: Option[String] = None)(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.eq(AtedConstants.SelectedPreviousReturn))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(true)))
      when(mockPropertyDetailsService.validateCalculateDraftPropertyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(true))
      when(mockPropertyDetailsService.saveDraftPropertyDetailsSupportingInfo(ArgumentMatchers.eq("1"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(OK))
      when(mockPropertyDetailsService.calculateDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(propertyDetails)))))
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testPropertyDetailsSupportingInfoController.save("1", periodKey, mode)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }

    def submitWithAuthorisedUserEdit(inputJson: JsValue, propertyDetails: Option[PropertyDetails], mode: Option[String] = None)(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.eq(AtedConstants.SelectedPreviousReturn))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockPropertyDetailsService.validateCalculateDraftPropertyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(true))
      when(mockPropertyDetailsService.saveDraftPropertyDetailsSupportingInfo(ArgumentMatchers.eq("1"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(OK))
      when(mockPropertyDetailsService.calculateDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(propertyDetails)))))
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testPropertyDetailsSupportingInfoController.save("1", periodKey, mode)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {

  }

  "PropertyDetailsSupportingInfoController" must {
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

        "show the chargeable property details value view with no data" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)

          getDataWithAuthorisedUser(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Do you have any supporting information to add? (optional)"))

              document.getElementById("supportingInfo").attr("value") must be("")

              document.getElementById("submit").text() must be("Save and continue")
          }
        }

        "show the chargeable property details value view with existing data" in new Setup {
          val propertyDetailsPeriod: Option[PropertyDetailsPeriod] = PropertyDetailsBuilder.getPropertyDetailsPeriodDatesLiable(new LocalDate("970-12-01"), new LocalDate("1999-03-02")).
            map(_.copy(supportingInfo = Some("supportingInfoTextAreaData")))

          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = propertyDetailsPeriod)
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

        "show the chargeable property details value view with no data and add a back link to the summary page" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(period = None)

          editFromSummary(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Do you have any supporting information to add? (optional)"))


              document.getElementById("backLinkHref").text must be("Back")
              document.getElementById("backLinkHref").attr("href") must include("/ated/liability/create/summary")
          }
        }

        "show the chargeable property details value view with no data and add a back link to the summary page even with a propertyDetails period" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))

          editFromSummary(propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Do you have any supporting information to add? (optional)"))


              document.getElementById("backLinkHref").text must be("Back")
              document.getElementById("backLinkHref").attr("href") must include("/ated/liability/create/summary")
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

          val invalidData: String = "a" * 201
          val inputJson: JsValue = Json.toJson(PropertyDetailsSupportingInfo(invalidData))
          when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson, None) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }
        "for valid data, return Forward to the summary page" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          val inputJson: JsValue = Json.toJson(PropertyDetailsSupportingInfo(""))
          when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson, Some(propertyDetails)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/summary/")
          }
        }

        "for valid edit liability data forward to the Edit Liability Summary Page" in new Setup {
          val propertyDetails: PropertyDetails = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("1")
          val inputJson: JsValue = Json.toJson(PropertyDetailsSupportingInfo(""))
          when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUserEdit(inputJson, Some(propertyDetails), Some(AtedUtils.EDIT_SUBMITTED)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/1/change/summary")
          }
        }

        "for invalid agent, throw BAD_REQUEST"  in new Setup {
          val propertyDetails: PropertyDetails = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("1")
          val inputJson: JsValue = Json.toJson(PropertyDetailsSupportingInfo(""))
          when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithInvalidAgent(inputJson, Some(propertyDetails)) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }
      }
    }
  }
}
