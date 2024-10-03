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
import models.{DateOfChange, HasBeenRevalued, PropertyDetailsNewValuation, PropertyDetailsRevalued}
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Captor}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants.{DelegatedClientAtedRefNumber, FortyThousandValueDateOfChange, HasPropertyBeenRevalued, propertyDetailsNewValuationValue}
import views.html.BtaNavigationLinks
import views.html.propertyDetails.propertyDetailsDateOfRevalue

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class PropertyDetailsDateOfRevalueControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with MockAuthUtil{

  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockNewValuationController: PropertyDetailsNewValuationController = mock[PropertyDetailsNewValuationController]
  val mockIsFullTaxPeriodController: IsFullTaxPeriodController = mock[IsFullTaxPeriodController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: propertyDetailsDateOfRevalue = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsDateOfRevalue]

  class Setup {
    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testController: PropertyDetailsDateOfRevalueController = new PropertyDetailsDateOfRevalueController(
      mockMcc,
      mockAuthAction,
      mockServiceInfoService,
      injectedViewInstance,
      mockPropertyDetailsService,
      mockBackLinkCacheConnector,
      mockDataCacheConnector,
      mockIsFullTaxPeriodController
    )


    val customBtaNavigationLinks = btaNavigationLinksView()(messages, mockAppConfig)

    val userId = s"user-${UUID.randomUUID}"
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("z11 1zz")).copy(value = None)
  }

  "PropertyDetailsDateOfRevalueController.view" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup {
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
        setInvalidAuthMocks(authMock)
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "render the date of revalue page" when {
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

  "PropertyDetailsDateOfRevalueController.save" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup {
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
        setInvalidAuthMocks(authMock)
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "collect information from cache and save to database" when {
      "newRevaluedFeature flag is set to true and user clicks button 'save and continue'" in new Setup {
        val inputJson: JsValue = Json.obj(
          "dateOfRevalue" -> Json.obj(
            "day" -> 1,
            "month" -> 4,
            "year" -> 2020
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

        when(mockDataCacheConnector.fetchAndGetFormData[HasBeenRevalued](ArgumentMatchers.eq(HasPropertyBeenRevalued))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(HasBeenRevalued(Some(true)))))
        when(mockDataCacheConnector.fetchAndGetFormData[PropertyDetailsNewValuation](ArgumentMatchers.eq(propertyDetailsNewValuationValue))
          (ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(PropertyDetailsNewValuation(Some(1000000)))))
        when(mockDataCacheConnector.fetchAndGetFormData[DateOfChange](ArgumentMatchers.eq(FortyThousandValueDateOfChange))
          (ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(DateOfChange(Some(LocalDate.of(2021, 6, 15))))))

        when(mockPropertyDetailsService.saveDraftPropertyDetailsRevalued(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(OK))

        val result = testController.save("1", 2020, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/liability/create/full-tax-period/view")

        val expectedPropertyDetails = PropertyDetailsRevalued(
          isPropertyRevalued = Some(true),
          revaluedValue = Some(1000000),
          revaluedDate = Some(LocalDate.of(2020, 4, 1)),
          partAcqDispDate = Some(LocalDate.of(2021, 6, 15))
        )

        verify(mockPropertyDetailsService).saveDraftPropertyDetailsRevalued(ArgumentMatchers.eq("1"),
          ArgumentMatchers.eq(expectedPropertyDetails)
        )(ArgumentMatchers.any(), ArgumentMatchers.any())

        verify(mockDataCacheConnector).fetchAndGetFormData[HasBeenRevalued](
          ArgumentMatchers.eq(HasPropertyBeenRevalued)
        )(ArgumentMatchers.any(), ArgumentMatchers.any())

        verify(mockBackLinkCacheConnector).saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())
      }
    }

    "redirect to next page: full tax period" when {
      "newRevaluedFeature flag is set to true and user enters valid date" in new Setup {
        val day = "1"
        val month = "4"
        val year = "2015"
        val inputJson: JsValue = Json.obj(
          "dateOfRevalue" -> Json.obj(
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
        redirectLocation(result).get must include("ated/liability/create/full-tax-period/view")
      }
    }

    "return BAD_REQUEST with error message for invalid date" when {
      "user enters an invalid date (31st of February)" in new Setup {
        val day = "31"
        val month = "2"
        val year = "2024"
        val inputJson: JsValue = Json.obj(
          "dateOfRevalue" -> Json.obj(
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
          "dateOfRevalue" -> Json.obj(
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
