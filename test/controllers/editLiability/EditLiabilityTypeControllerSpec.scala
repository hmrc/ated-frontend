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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DelegationService
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class EditLiabilityTypeControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {


  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  object TestEditLiabilityTypeController extends EditLiabilityTypeController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector

  }

  override def beforeEach: Unit = {
    reset(mockAuthConnector)
    reset(mockDelegationService)
    reset(mockBackLinkCache)
  }

  "EditLiabilityTypeController" must {

    "use correct DelegationService" in {
      EditLiabilityTypeController.delegationService must be(DelegationService)
    }

    "editLiability" must {

      "take user to edit liability page" in {
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

      "if user doesn't select any radio button, show form error with bad_request" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType": ""}"""))
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedUser(fakeRequest) {
          result =>
            status(result) must be(BAD_REQUEST)
        }
      }
      "if user select 'change return' any radio button, redirect to edit return page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType":"CR"}"""))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        when(mockBackLinkCache.clearBackLinks(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Nil))
        continueWithAuthorisedUser(fakeRequest) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/address"))
        }
      }
      "if user select 'dispose property' any radio button, redirect to dispose property page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType":"DP"}"""))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedUser(fakeRequest) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/dispose"))
        }
      }
      "for anything else, redirect to edit liability page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"editLiabilityType":"X"}"""))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedUser(fakeRequest) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/edit/2015?editAllowed=true"))
        }
      }
    }
  }

  def editLiabilityWithAuthorisedUser(test: Future[Result] => Any) {
    val periodKey: Int = 2015
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestEditLiabilityTypeController.editLiability("12345678901", periodKey, editAllowed = true)
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val periodKey: Int = 2015
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = TestEditLiabilityTypeController.continue("12345678901", periodKey, editAllowed = true)
      .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
