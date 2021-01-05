/*
 * Copyright 2021 HM Revenue & Customs
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

import builders.SessionBuilder
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import controllers.propertyDetails.{AddressLookupController, PropertyDetailsAddressController}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServiceInfoService
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.editLiability.editLiability

import scala.concurrent.Future

class EditLiabilityTypeControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsAddressController: PropertyDetailsAddressController = mock[PropertyDetailsAddressController]
  val mockAddressLookupController: AddressLookupController = mock[AddressLookupController]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDisposePropertyController: DisposePropertyController = mock[DisposePropertyController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val fakeDisposePropertyRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType":"DP"}"""))
  val fakeChangeReturnRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType":"CR"}"""))
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: editLiability = app.injector.instanceOf[views.html.editLiability.editLiability]

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testEditLiabilityTypeController:EditLiabilityTypeController = new EditLiabilityTypeController (
    mockMcc,
    mockPropertyDetailsAddressController,
    mockAddressLookupController,
    mockAuthAction,
    mockDisposePropertyController,
    mockServiceInfoService,
    mockDataCacheConnector,
    mockBackLinkCacheConnector,
    injectedViewInstance
  )

    def editLiabilityWithAuthorisedUser(test: Future[Result] => Any, queryParams: Option[(String, Seq[String])]) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      val backLink = "/ated/form-bundle/12345678901/" + periodKey
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(backLink)))
      val result = testEditLiabilityTypeController.editLiability("12345678901", periodKey, editAllowed = true)
        .apply(SessionBuilder.buildRequestWithSession(userId, queryParams))
      test(result)
    }

    def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = testEditLiabilityTypeController.continue("12345678901", periodKey, editAllowed = true)
        .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
      test(result)
    }
}

  override def beforeEach: Unit = {

    reset(mockPropertyDetailsAddressController)
    reset(mockAddressLookupController)
    reset(mockDelegationService)
    reset(mockDataCacheConnector)
    reset(mockBackLinkCacheConnector)
    reset(mockDisposePropertyController)
  }

  "EditLiabilityTypeController" must {
    "editLiability" must {

      "take user to edit liability page with no pre-population" in new Setup {
        editLiabilityWithAuthorisedUser({
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title must be("Have you disposed of the property? - GOV.UK")
            document.getElementById("edit-liability-header").text() must be("Have you disposed of the property?")
            assert(!document.getElementById("editLiabilityType-cr").hasAttr("checked"))
            assert(!document.getElementById("editLiabilityType-dp").hasAttr("checked"))
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must be("/ated/form-bundle/12345678901/2015")
        }, queryParams = None)
      }

      "take user to edit liability page with change return option pre-populated" in new Setup {
        editLiabilityWithAuthorisedUser({
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title must be("Have you disposed of the property? - GOV.UK")
            document.getElementById("edit-liability-header").text() must be("Have you disposed of the property?")
            assert(document.getElementById("editLiabilityType-cr").hasAttr("checked"))
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must be("/ated/form-bundle/12345678901/2015")
        }, queryParams = Some(Tuple2("disposal", Seq("false"))))
      }

      "take user to edit liability page with dispose option pre-populated" in new Setup {
        editLiabilityWithAuthorisedUser({
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title must be("Have you disposed of the property? - GOV.UK")
            document.getElementById("edit-liability-header").text() must be("Have you disposed of the property?")
            assert(document.getElementById("editLiabilityType-dp").hasAttr("checked"))
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must be("/ated/form-bundle/12345678901/2015")
        }, queryParams = Some(Tuple2("disposal", Seq("true"))))
      }
    }

    "continue" must {

      "if user doesn't select any radio button, show form error with bad_request" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType": ""}"""))
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedUser(fakeRequest) {
          result =>
            status(result) must be(BAD_REQUEST)
        }
      }
      "if user select 'change return' any radio button, redirect to edit return page" in new Setup {
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        when(mockBackLinkCacheConnector.clearBackLinks(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Nil))
        continueWithAuthorisedUser(fakeChangeReturnRequest) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/address"))
        }
      }
      "if user select 'dispose property' any radio button, redirect to dispose property page" in new Setup {
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        continueWithAuthorisedUser(fakeDisposePropertyRequest) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/dispose"))
        }
      }
      "for anything else, redirect to edit liability page" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType":"X"}"""))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        continueWithAuthorisedUser(fakeRequest) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/edit/2015?editAllowed=true"))
        }
      }
    }
  }
}
