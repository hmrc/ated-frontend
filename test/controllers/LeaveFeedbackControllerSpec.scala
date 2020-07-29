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

package controllers

import java.util.UUID

import builders.{SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.AuthAction
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.ServiceInfoService
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class LeaveFeedbackControllerSpec extends PlaySpec with MockitoSugar with GuiceOneServerPerSuite with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.feedback.leaveFeedback]
  val injectedViewInstanceThanks = app.injector.instanceOf[views.html.feedback.thanks]


  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testLeaveFeedbackController: LeaveFeedbackController = new LeaveFeedbackController (
      mockMcc,
      mockAuthAction,
      mockServiceInfoService,
      mockAuditConnector,
      injectedViewInstance,
      injectedViewInstanceThanks
    )

    def getWithAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      val result = testLeaveFeedbackController.view("/ated/home").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submit(inputData: Seq[(String, String)])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testLeaveFeedbackController.submitFeedback("/ated/home")
        .apply(SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody(inputData: _*))
      test(result)
    }

    def thanks(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testLeaveFeedbackController.thanks("/ated/home").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
  }

  "LeaveFeedbackController" must {
    "show the feedback view" in new Setup {
      getWithAuthorisedUser {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("Leave Feedback"))
          assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
          document.getElementById("feedback-header").text() must be("Leave Feedback")
          document.getElementById("feedback-txt")
            .text() must be("You will not get a reply to any feedback. If you want to raise a technical problem or get a response use the get help with this page link. Do not include any personal or financial information.")
          document.getElementById("summaryInfo_field").text() must be("What were you trying to do today?")
          document.getElementById("moreInfo_field").text() must be("What would you like to tell us?")
          document.getElementById("submit").text() must be("Send feedback")
          document.getElementById("back-link").attr("href") must be("/ated/home")
      }
    }

    "show the correct errors if nothing has been entered on the form" in new Setup {
      val inputData: Seq[(String, String)] = Seq(("summaryInfo", ""), ("moreInfo", ""))
      submit(inputData) {
        result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("Leave Feedback"))
          document.getElementById("feedback-header").text() must be("Leave Feedback")
          document.getElementById("summaryInfo-error").text() must be("There is a problem with the what were you trying to do question")
          document.getElementById("moreInfo-error").text() must be("There is a problem with the what would you like to tell us question")
          document.getElementById("experienceLevel-error").text() must be("There is a problem with the your experience question")
          document.getElementById("summaryInfo-error-0").text() must be("You must answer this question")
          document.getElementById("moreInfo-error-0").text() must be("You must answer this question")
          document.getElementById("experienceLevel-error-0").text() must be("Select an option for this question")
      }
    }

    "redirect to the Thank You page if the data has been entered into the form correctly" in new Setup {
      val inputData: Seq[(String, String)] = Seq(("summaryInfo", "This is my summary info"), ("moreInfo", "This is more info"), ("experienceLevel", "2"))
      submit(inputData) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/ated/feedback/thanks?return=%2Fated%2Fhome"))
      }
    }

    "display the Thank You page" in new Setup {
      thanks {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("Thank you"))
          document.getElementById("feedback-thanks-header").text() must be("Thank you")
      }
    }
  }
}
