/*
 * Copyright 2026 HM Revenue & Customs
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

import builders.{SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import controllers.auth.AuthAction
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContentAsFormUrlEncoded, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.*
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants
import views.html.{BtaNavigationLinks, confirmDeleteDraft}

import scala.concurrent.Future

class DraftDeleteConfirmationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {


  given hc: HeaderCarrier = HeaderCarrier()

  given mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockDetailsService: DetailsService = mock[DetailsService]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  given messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: confirmDeleteDraft = app.injector.instanceOf[views.html.confirmDeleteDraft]

  val periodKey: Int = 2017
  val organisationName: String = "OrganisationName"
  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789013"

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testDraftDeleteConfirmationController: DraftDeleteConfirmationController = new DraftDeleteConfirmationController(
      mockMcc,
      mockAuthAction,
      mockPropertyDetailsService,
      mockReliefsService,
      mockServiceInfoService,
      mockDataCacheService,
      injectedViewInstance
    )
    val userId = "user-a12e4b7f-9d8c-4f3b-8c1a-5e7d9f2b4a6c"

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testDraftDeleteConfirmationController.view(Some("123456"), periodKey, "draft")
        .apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedUser(returnType: String, id: Option[String] = None)(test: Future[Result] => Any): Unit = {
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockServiceInfoService.getPartial(using ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
      val result = testDraftDeleteConfirmationController.view(id, periodKey, returnType).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedFormUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], returnType: String, id: Option[String] = None)(test: Future[Result] => Any): Unit = {
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheService.fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.deleteDraftReliefs(ArgumentMatchers.eq(periodKey))(using ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))
      when(mockPropertyDetailsService.clearDraftReliefs(ArgumentMatchers.eq("123456"))(using ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))
      val result = testDraftDeleteConfirmationController.submit(id, periodKey, returnType)
        .apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))
      test(result)
  }
}

  override def beforeEach(): Unit = {
  }

  "DraftDeleteConfirmationController" must {
    "view" must {
      "unauthorised users" must {

        "respond with a redirect, and be redirected to unauthorised page" in new Setup {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "authorised users" must {

        "see the confirmation yes/no page for relief" in new Setup {
          viewWithAuthorisedUser("relief", Some("12345")) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Are you sure you want to delete this draft return?"))
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
          }
        }

        "see the confirmation yes/no page for charge" in new Setup {
          viewWithAuthorisedUser("charge", Some("12345")) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Are you sure you want to delete this draft return?"))
          }
        }

        "throw runtime exception, if no id is not found for charge" in new Setup {
          viewWithAuthorisedUser("charge") {
            result =>
              val thrown = the[RuntimeException] thrownBy await(result)
              thrown.getMessage must include("No id found for draft return")
          }
        }
      }
    }

    "submit" must {

      "throw a BAD_REQUEST, when nothing is selected" in new Setup {
        submitWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "yesNo" -> ""),
          "relief",
          Some("12345")
        ) { result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Error: Are you sure you want to delete this draft return?"))
        }
      }

      "redirect it to period sumary page, for yes in relief" in new Setup {
        submitWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "yesNo" -> "true"),
          "relief",
          Some("12345")
        ) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/period-summary/2017")
        }
      }

      "redirect it to period sumary page, for yes in charge" in new Setup {
        submitWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "yesNo" -> "true"),
          "charge",
          Some("12345")
        ) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/period-summary/2017")
        }
      }

      "redirect it to chargable summary page, for no" in new Setup {
        submitWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "yesNo" -> "false"),
          "charge",
          Some("12345")
        ) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/summary/12345")
        }
      }

      "redirect it to relief summary page, for no" in new Setup {
        submitWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "yesNo" -> "false"),
          "relief",
          Some("12345")
        ) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/reliefs/2017/relief-summary")
        }
      }

      "throw runtime exception for charge, when no id is passed and yes is selceted" in new Setup {
        submitWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "yesNo" -> "true"),
          "charge"
        ) { result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must include("No id found for draft return")
        }
      }

      "throw runtime exception for charge, when no id is passed and no is selceted" in new Setup {
        submitWithAuthorisedFormUser(FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "yesNo" -> "false"),
          "charge"
        ) { result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must include("No id found for draft return")
        }
      }
    }
  }
}
