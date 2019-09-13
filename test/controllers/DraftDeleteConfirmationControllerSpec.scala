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

package controllers

import java.util.UUID

import builders.{SessionBuilder, TitleBuilder}
import connectors.DataCacheConnector
import forms.AtedForms.YesNoQuestion
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import services._
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class DraftDeleteConfirmationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val periodKey: Int = 2017

  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val organisationName: String = "OrganisationName"
  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789013"

  object TestDraftDeleteConfirmationController extends DraftDeleteConfirmationController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val propertyDetailsService: PropertyDetailsService = mockPropertyDetailsService
    override val reliefsService: ReliefsService = mockReliefsService
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
    override val delegationService: DelegationService = mockDelegationService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockPropertyDetailsService)
    reset(mockReliefsService)
    reset(mockDataCacheConnector)
    reset(mockDelegationService)
  }

  "DraftDeleteConfirmationController" must {

    "use correct property details service" in {
      DraftDeleteConfirmationController.propertyDetailsService must be(PropertyDetailsService)
    }

    "use correct relief service" in {
      DraftDeleteConfirmationController.reliefsService must be(ReliefsService)
    }

    "view" must {

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
          val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = TestDraftDeleteConfirmationController.view(Some("123456"), periodKey, "draft")
        .apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedUser(returnType: String, id: Option[String] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = TestDraftDeleteConfirmationController.view(id, periodKey, returnType).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(inputJson: JsValue, returnType: String, id: Option[String] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.deleteDraftReliefs(Matchers.eq(periodKey))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK)))
      when(mockPropertyDetailsService.clearDraftReliefs(Matchers.eq("123456"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK)))
      val result = TestDraftDeleteConfirmationController.submit(id, periodKey, returnType)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }

  }
}
