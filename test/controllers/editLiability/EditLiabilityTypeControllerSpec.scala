/*
 * Copyright 2019 HM Revenue & Customs
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
import testhelpers.MockAuthUtil
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants

import scala.concurrent.Future

class EditLiabilityTypeControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsAddressController: PropertyDetailsAddressController = mock[PropertyDetailsAddressController]
  val mockAddressLookupController: AddressLookupController = mock[AddressLookupController]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDisposePropertyController: DisposePropertyController = mock[DisposePropertyController]

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
    mockDataCacheConnector,
    mockBackLinkCacheConnector
  )

    def editLiabilityWithAuthorisedUser(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testEditLiabilityTypeController.editLiability("12345678901", periodKey, editAllowed = true)
        .apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
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

      "take user to edit liability page" in new Setup {
        editLiabilityWithAuthorisedUser {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title must be("How do you want to change your ATED return? - GOV.UK")
            document.getElementById("edit-liability-header").text() must be("How do you want to change your ATED return?")
        }
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
        val fakeRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType":"CR"}"""))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockBackLinkCacheConnector.clearBackLinks(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Nil))
        continueWithAuthorisedUser(fakeRequest) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/address"))
        }
      }
      "if user select 'dispose property' any radio button, redirect to dispose property page" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType":"DP"}"""))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedUser(fakeRequest) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/dispose"))
        }
      }
      "for anything else, redirect to edit liability page" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType":"X"}"""))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedUser(fakeRequest) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/edit/2015?editAllowed=true"))
        }
      }
    }
  }
}
