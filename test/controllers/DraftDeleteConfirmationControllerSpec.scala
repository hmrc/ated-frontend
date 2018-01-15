/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import java.util.UUID

import builders.{AuthBuilder, PropertyDetailsBuilder, SessionBuilder, TitleBuilder}
import config.FrontendDelegationConnector
import connectors.{AgentClientMandateFrontendConnector, DataCacheConnector}
import forms.AtedForms.YesNoQuestion
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import play.twirl.api.Html
import services._
import uk.gov.hmrc.play.frontend.auth.DummyDelegationData
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.HtmlPartial
import utils.AtedConstants
import utils.AtedConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse

class DraftDeleteConfirmationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockPropertyDetailsService = mock[PropertyDetailsService]
  val mockReliefsService = mock[ReliefsService]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val organisationName = "OrganisationName"
  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789013"

  implicit def atedContext2AuthContext(implicit atedContext: AtedContext) = atedContext.user.authContext

  object TestDraftDeleteConfirmationController extends DraftDeleteConfirmationController {
    override val authConnector = mockAuthConnector
    override val propertyDetailsService = mockPropertyDetailsService
    override val reliefsService = mockReliefsService
    override val dataCacheConnector = mockDataCacheConnector
    override val delegationConnector = mockDelegationConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockPropertyDetailsService)
    reset(mockReliefsService)
    reset(mockDataCacheConnector)
    reset(mockDelegationConnector)
  }

  "DraftDeleteConfirmationController" must {

    "use correct property details service" in {
      DraftDeleteConfirmationController.propertyDetailsService must be(PropertyDetailsService)
    }

    "use correct relief service" in {
      DraftDeleteConfirmationController.reliefsService must be(ReliefsService)
    }

    "view" must {

      "not respond with NOT_FOUND when we dont pass an id" in {
        val result = route(FakeRequest(GET, "/ated/liability/create/summary/1"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
      }

      "unauthorised users" must {

        "respond with a redirect, and be redirected to unauthorised page" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "authorised users" must {

        "see the confirmation yes/no page for relief" in {
          viewWithAuthorisedUser("relief", Some("12345")) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Are you sure you want to delete this draft return?"))
          }
        }


        "see the confirmation yes/no page for charge" in {
          viewWithAuthorisedUser("charge", Some("12345")) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Are you sure you want to delete this draft return?"))
          }
        }

        "throw runtime exception, if no id is not found for charge" in {
          viewWithAuthorisedUser("charge") {
            result =>
              val thrown = the[RuntimeException] thrownBy await(result)
              thrown.getMessage must include("No id found for draft return")
          }
        }

      }
    }

    "submit" must {

      "throw a BAD_REQUEST, when nothing is selected" in {
        submitWithAuthorisedUser(Json.toJson(YesNoQuestion(None)), "relief", Some("12345")) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Are you sure you want to delete this draft return?"))
        }
      }

      "redirect it to period sumary page, for yes in relief" in {
        submitWithAuthorisedUser(Json.toJson(YesNoQuestion(Some(true))), "relief", Some("12345")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/period-summary/2017")
        }
      }

      "redirect it to period sumary page, for yes in charge" in {
        submitWithAuthorisedUser(Json.toJson(YesNoQuestion(Some(true))), "charge", Some("12345")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/period-summary/2017")
        }
      }

      "redirect it to chargable summary page, for no" in {
        submitWithAuthorisedUser(Json.toJson(YesNoQuestion(Some(false))), "charge", Some("12345")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/summary/12345")
        }
      }

      "redirect it to relief summary page, for no" in {
        submitWithAuthorisedUser(Json.toJson(YesNoQuestion(Some(false))), "relief", Some("12345")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/reliefs/2017/relief-summary")
        }
      }

      "throw runtime exception for charge, when no id is passed and yes is selceted" in {
        submitWithAuthorisedUser(Json.toJson(YesNoQuestion(Some(true))), "charge") {
          result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must include("No id found for draft return")
        }
      }

      "throw runtime exception for charge, when no id is passed and no is selceted" in {
        submitWithAuthorisedUser(Json.toJson(YesNoQuestion(Some(false))), "charge") {
          result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must include("No id found for draft return")
        }
      }
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
      val result = TestDraftDeleteConfirmationController.view(Some("123456"), 2017, "draft").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedUser(returnType: String, id: Option[String] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = TestDraftDeleteConfirmationController.view(id, 2017, returnType).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(inputJson: JsValue, returnType: String, id: Option[String] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.deleteDraftReliefs(Matchers.eq(2017))(Matchers.any(), Matchers.any())).
        thenReturn(Future.successful(HttpResponse(OK)))
      when(mockPropertyDetailsService.clearDraftReliefs(Matchers.eq("123456"))(Matchers.any(), Matchers.any())).
        thenReturn(Future.successful(HttpResponse(OK)))
      val result = TestDraftDeleteConfirmationController.submit(id, 2017, returnType).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }

  }
}
