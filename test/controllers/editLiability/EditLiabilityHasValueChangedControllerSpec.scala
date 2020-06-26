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

package controllers.editLiability

import java.util.UUID

import builders.{PropertyDetailsBuilder, SessionBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import controllers.propertyDetails.{IsFullTaxPeriodController, PropertyDetailsOwnedBeforeController, PropertyDetailsTaxAvoidanceController}
import models.{HasValueChanged, PropertyDetails}
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
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class EditLiabilityHasValueChangedControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsOwnedBeforeController: PropertyDetailsOwnedBeforeController = mock[PropertyDetailsOwnedBeforeController]
  val mockIsFullTaxPeriodController: IsFullTaxPeriodController = mock[IsFullTaxPeriodController]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockPropertyDetailsTaxAvoidanceController: PropertyDetailsTaxAvoidanceController = mock[PropertyDetailsTaxAvoidanceController]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testEditLiabilityHasValueChangedController: EditLiabilityHasValueChangedController = new EditLiabilityHasValueChangedController(
    mockMcc,
    mockPropertyDetailsOwnedBeforeController,
    mockIsFullTaxPeriodController,
    mockAuthAction,
    mockServiceInfoService,
    mockPropertyDetailsService,
    mockDataCacheConnector,
    mockBackLinkCacheConnector
  )

  def viewWithAuthorisedUser(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testEditLiabilityHasValueChangedController.view("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editFromSummary(propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = testEditLiabilityHasValueChangedController.editFromSummary("12345678901", Some(true)).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def saveWithAuthorisedUser(propertyDetails: Option[PropertyDetails], inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    propertyDetails.map(propVal =>
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propVal)))
    )
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockPropertyDetailsService.saveDraftHasValueChanged(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
      thenReturn(Future.successful(OK))
    val result = testEditLiabilityHasValueChangedController.save("12345678901").apply(SessionBuilder.updateRequestWithSession(FakeRequest()
      .withJsonBody(inputJson), userId))
    test(result)
  }
}

  override def beforeEach: Unit = {

    reset(mockPropertyDetailsOwnedBeforeController)
    reset(mockIsFullTaxPeriodController)
reset(mockPropertyDetailsService)
    reset(mockDelegationService)
    reset(mockDataCacheConnector)
    reset(mockBackLinkCacheConnector)
  }

  "EditLiabilityHasValueChangedController" must {
    "view - for authorised users" must {

      "return a status of OK, when that liability return is found in cache" in new Setup {
        val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getPropertyDetailsWithFormBundleReturn("12345678901")
        viewWithAuthorisedUser(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be ("Has the value of your property changed for the purposes of ATED? - GOV.UK")
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
        }
      }
    }

    "editFromSummary - for authorised users" must {

      "return a status of OK and set the back link to the summary page" in new Setup {
        val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getPropertyDetailsWithFormBundleReturn("12345678901")
        editFromSummary(changeLiabilityReturn) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be ("Has the value of your property changed for the purposes of ATED? - GOV.UK")

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/liability/create/summary/12345678901")
        }
      }
    }


    "save - for authorised user" must {

      "for invalid data, return BAD_REQUEST" in new Setup {
        val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getPropertyDetailsWithFormBundleReturn("12345678901")
        val inputJson: JsValue = Json.parse("""{"startDate.day": "31", "startDate.month": "6", "startDate.year": "2015", "periodKey": 2015}""")
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(Some(changeLiabilityReturn), inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
        }
      }

      "for valid date when we have indicated that the value has changed, save and redirect to change in acquisition page" in new Setup {
        val value1 = HasValueChanged(Some(true))
        val inputJson: JsValue = Json.toJson(value1)
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(None, inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/create/owned-before/view/12345678901"))
        }
      }

      "for valid date when we have indicated that the value has NOT changed, save and redirect to change in period page" in new Setup {
        val value1 = HasValueChanged(Some(false))
        val inputJson: JsValue = Json.toJson(value1)
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(None, inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/create/full-tax-period/view/12345678901"))
        }
      }
    }
  }
}
