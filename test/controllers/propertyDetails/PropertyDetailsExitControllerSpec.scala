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
import models.PropertyDetailsNewValuation
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants.DelegatedClientAtedRefNumber
import views.html.BtaNavigationLinks
import views.html.propertyDetails.propertyDetailsExit

import java.util.UUID
import scala.concurrent.Future

class PropertyDetailsExitControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDateOfRevalueController: PropertyDetailsDateOfRevalueController = mock[PropertyDetailsDateOfRevalueController]
  val mockExitController: PropertyDetailsExitController = mock[PropertyDetailsExitController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: propertyDetailsExit = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsExit]


  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testController: PropertyDetailsExitController = new PropertyDetailsExitController(
      mockMcc,
      mockAuthAction,
      mockPropertyDetailsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    val customBtaNavigationLinks = btaNavigationLinksView()(messages, mockAppConfig)

    val userId = s"user-${UUID.randomUUID}"
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("z11 1zz")).copy(value = None)
  }

  "PropertyDetailsExitController.view" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup {
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
        setInvalidAuthMocks(authMock)
        val result = testController.view().apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "render the Exit page" when {
        "newRevaluedFeature flag is set to true" in new Setup {
          val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
          setAuthMocks(authMock)
          when(mockAppConfig.newRevaluedFeature).thenReturn(true)
          when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(customBtaNavigationLinks))
          when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(DelegatedClientAtedRefNumber))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
          when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
            (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
          when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
          val result = testController.view().apply(SessionBuilder.buildRequestWithSession(userId))
          status(result) mustBe OK
        }
      }

      "redirect to home page" when {
        "newRevaluedFeature flag is set to false" in new Setup {
          val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
          setAuthMocks(authMock)
          when(mockAppConfig.newRevaluedFeature).thenReturn(false)
          val result = testController.view().apply(SessionBuilder.buildRequestWithSession(userId))
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get must include("ated/home")
        }
      }  
  } 
}
