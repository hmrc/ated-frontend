/*
 * Copyright 2024 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.MessagesControllerComponents
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import play.api.test.Helpers._
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import views.html.BtaNavigationLinks
import views.html.propertyDetails.propertyDetailsDateOfChange
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import utils.AtedConstants.DelegatedClientAtedRefNumber

import java.util.UUID
import scala.concurrent.Future

class PropertyDetailsDateOfChangeControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with MockAuthUtil{

  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockHasBeenRevaluedController: PropertyDetailsHasBeenRevaluedController = mock[PropertyDetailsHasBeenRevaluedController]
  val mockNewValuationController: PropertyDetailsNewValuationController = mock[PropertyDetailsNewValuationController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: propertyDetailsDateOfChange = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsDateOfChange]



  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testController: PropertyDetailsDateOfChangeController = new PropertyDetailsDateOfChangeController(
      mockMcc,
      mockAuthAction,
      mockServiceInfoService,
      injectedViewInstance,
      mockPropertyDetailsService,
      mockBackLinkCacheConnector,
      mockDataCacheConnector,
      mockNewValuationController
    )

    val customBtaNavigationLinks = btaNavigationLinksView()(messages, mockAppConfig)

    val userId = s"user-${UUID.randomUUID}"
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("z11 1zz")).copy(value = None)
  }

  "PropertyDetailsDateOfChangeController.view" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup {
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
        setInvalidAuthMocks(authMock)
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "render the date of change page" when {
      "newRevaluedFeature flag is set to true" in new Setup {
        val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
        setAuthMocks(authMock)
        when(mockAppConfig.newRevaluedFeature).thenReturn(true)
        when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(customBtaNavigationLinks))
        when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(DelegatedClientAtedRefNumber))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
        when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe OK
      }
    }

    "redirect to home page" when {
      "newRevaluedFeature flag is set to false" in new Setup {
        val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
        setAuthMocks(authMock)
        when(mockAppConfig.newRevaluedFeature).thenReturn(false)
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/home")
      }
    }

    "for page errors, return BAD_REQUEST" in new Setup {
      val inputJson: JsValue = Json.obj()
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockAppConfig.newRevaluedFeature).thenReturn(true)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(customBtaNavigationLinks))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(DelegatedClientAtedRefNumber))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockPropertyDetailsService.saveDraftPropertyDetailsRevalued(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(OK))
      val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("There is a problem")
    }
  }

  "PropertyDetailsDateOfChangeController.save" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup {
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
        setInvalidAuthMocks(authMock)
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "redirect to next page: new-valuation" when {
      "newRevaluedFeature flag is set to true and user enters valid date" in new Setup {
        val day = "1"
        val month = "4"
        val year = "2015"
        val inputJson: JsValue = Json.obj(
          "dateOfChange" -> Json.obj(
            "day" -> day,
            "month" -> month,
            "year" -> year
          )
        )
        val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
        setAuthMocks(authMock)
        when(mockAppConfig.newRevaluedFeature).thenReturn(true)
        when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(customBtaNavigationLinks))
        when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(DelegatedClientAtedRefNumber))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
        when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockPropertyDetailsService.saveDraftPropertyDetailsRevalued(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(OK))
        val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/liability/create/new-valuation/view")
      }
    }

    "return BAD_REQUEST with error message for invalid date" when {
      "user enters an invalid date (31st of February)" in new Setup {
        val day = "31"
        val month = "2"
        val year = "2024"
        val inputJson: JsValue = Json.obj(
          "dateOfChange" -> Json.obj(
            "day" -> day,
            "month" -> month,
            "year" -> year
          )
        )
        val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
        setAuthMocks(authMock)
        when(mockAppConfig.newRevaluedFeature).thenReturn(true)
        when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(customBtaNavigationLinks))
        when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(DelegatedClientAtedRefNumber))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
        when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockPropertyDetailsService.saveDraftPropertyDetailsRevalued(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(OK))
        val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("There is a problem")
      }
    }

    "return BAD_REQUEST with error message for missing date fields" when {
      "user omits some date fields" in new Setup {
        val day = ""
        val month = "4"
        val year = "2024"
        val inputJson: JsValue = Json.obj(
          "dateOfChange" -> Json.obj(
            "day" -> day,
            "month" -> month,
            "year" -> year
          )
        )
        val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
        setAuthMocks(authMock)
        when(mockAppConfig.newRevaluedFeature).thenReturn(true)
        when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(customBtaNavigationLinks))
        when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(DelegatedClientAtedRefNumber))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
        when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockPropertyDetailsService.saveDraftPropertyDetailsRevalued(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(OK))
        val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("There is a problem")
      }
    }

    "redirect to home page" when {
      "newRevaluedFeature flag is set to false" in new Setup {
        val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
        setAuthMocks(authMock)
        when(mockAppConfig.newRevaluedFeature).thenReturn(false)
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/home")
      }
    }
  }
}
