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

package controllers

import java.util.UUID

import builders.AuthBuilder._
import builders.{AuthBuilder, SessionBuilder, TestAudit}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class LeaveFeedbackControllerSpec extends PlaySpec with MockitoSugar with OneServerPerSuite with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]

  override def beforeEach() = {
    reset(mockAuthConnector)
  }

  object TestLeaveFeedbackController extends LeaveFeedbackController {
    override val authConnector = mockAuthConnector
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"
  }

  "LeaveFeedbackController" must {

    "have the correct audit connector" in {
      LeaveFeedbackController.audit must be(LeaveFeedbackController.audit)
    }

    "show the feedback view" in {
      getWithAuthorisedUser {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Leave Feedback")
          document.getElementById("feedback-header").text() must be("Leave Feedback")
          document.getElementById("feedback-txt").text() must be("You won’t get a reply to any feedback. If you want to raise a technical problem or get a response use the get help with this page link. Do not include any personal or financial information.")
          document.getElementById("summaryInfo_field").text() must be("What were you trying to do today?")
          document.getElementById("moreInfo_field").text() must be("What would you like to tell us?")
          document.getElementById("experienceLevel_legend").text() must be("Overall, how do you feel about your experience using the service today?")
          document.getElementById("submit").text() must be("Send feedback")
          document.getElementById("back-link").attr("href") must be("/ated/home")
      }
    }

    "show the correct errors if nothing has been entered on the form" in {
      val inputData = Seq(("summaryInfo", ""), ("moreInfo", ""))
      submit(inputData) {
        result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Leave Feedback")
          document.getElementById("feedback-header").text() must be("Leave Feedback")
          document.getElementById("summaryInfo-error").text() must be("There is a problem with the what were you trying to do question")
          document.getElementById("moreInfo-error").text() must be("There is a problem with the what would you like to tell us question")
          document.getElementById("experienceLevel-error").text() must be("There is a problem with the your experience question")
          document.getElementById("summaryInfo-error-0").text() must be("You must answer this question")
          document.getElementById("moreInfo-error-0").text() must be("You must answer this question")
          document.getElementById("experienceLevel-error-0").text() must be("Select an option for this question")
      }
    }

    "redirect to the Thank You page if the data has been entered into the form correctly" in {
      val inputData = Seq(("summaryInfo", "This is my summary info"), ("moreInfo", "This is more info"), ("experienceLevel", "2"))
      submit(inputData) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/ated/feedback/thanks?return=%2Fated%2Fhome"))
      }
    }

    "display the Thank You page" in {
      thanks {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Thank you")
          document.getElementById("feedback-thanks-header").text() must be("Thank you")
      }
    }

  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestLeaveFeedbackController.view("/ated/home").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submit(inputData: Seq[(String, String)])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestLeaveFeedbackController.submitFeedback("/ated/home").apply(SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody(inputData: _*))
    test(result)
  }

  def thanks(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestLeaveFeedbackController.thanks("/ated/home").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
