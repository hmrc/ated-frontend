/*
 * Copyright 2017 HM Revenue & Customs
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

import builders.{AuthBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future

class EditLiabilityTypeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestEditLiabilityTypeController extends EditLiabilityTypeController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector

  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockDelegationConnector)
    reset(mockBackLinkCache)
  }

  "EditLiabilityTypeController" must {

    "use correct DelegationConnector" in {
      EditLiabilityTypeController.delegationConnector must be(FrontendDelegationConnector)
    }

    "editLiability" must {
      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/liability/1234567890/change/address"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
      }

      "take user to edit liability page" in {
        editLiabilityWithAuthorisedUser {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title must be("Edit your ATED return")
            document.getElementById("edit-liability-header").text() must be("Edit your ATED return")
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
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestEditLiabilityTypeController.editLiability("12345678901", 2015, true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = TestEditLiabilityTypeController.continue("12345678901", 2015, true)
      .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
