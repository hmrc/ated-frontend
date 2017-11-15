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

import builders.{SessionBuilder, TestAudit, TitleBuilder}
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.Audit

import scala.concurrent.Future

class QuestionnaireControllerSpec extends PlaySpec with OneServerPerSuite {

  object TestQuestionnaireController extends QuestionnaireController {
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"
  }

  "Calling QuestionnaireController.showQuestionnaire" should {
    "not respond with NOT_FOUND" in {
      val result = route(FakeRequest(GET, "/ated/signed-out"))
      result.isDefined must be(true)
      status(result.get) must not be NOT_FOUND
    }

    "show the questionnaire view" in {
      showQuestionnaireWithUnauthorisedUser {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("ATED feedback questionnaire")) //Questionnaire title
          document.getElementsByClass("heading-xlarge").text() must be("You’re signed out")
          document.getElementById("signed-out-text").text() must be("You’re now signed out of your ATED account.")
          document.getElementById("satisfaction-survey-heading").text() must be("Satisfaction survey")
          document.getElementById("dont-include-text").text() must be("Please don’t include any personal or financial information, for example your National Insurance or credit card numbers.")
          document.getElementById("submit").text() must be("Send feedback")
      }
    }
  }

  "Calling QuestionnaireController.submitQuestionnaire" should {
    "not respond with NOT_FOUND" in {
      val result = route(FakeRequest(POST, "/ated/signed-out"))
      result.isDefined must be(true)
      status(result.get) must not be NOT_FOUND
    }

    "submit the valid questionnaire" in {
      val detailSentToForm = s"""{"easyToUse":"1","satisfactionLevel" : "4","howCanWeImprove" : "I am happy!"}"""

      submitQuestionnaireWithUnauthorisedUser(FakeRequest().withJsonBody(Json.parse(detailSentToForm))) {
        result =>

          val document = Jsoup.parse(contentAsString(result))

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/ated/feedbackThankYou"))
      }
    }

    "submit the invalid questionnaire" in {
      val detailSentToForm = s"""{"easyToUse":5,"easyToUseExplain": "It was too difficult", "correctInformation" : "asdsd","correctInformationExplain" : "My address was wrong","satisfactionLevel" : "4","howCanWeImprove" : "I am happy!"}"""

      submitQuestionnaireWithUnauthorisedUser(FakeRequest().withJsonBody(Json.parse(detailSentToForm))) {
        result =>
          status(result) must be(400)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be(TitleBuilder.buildTitle("ATED feedback questionnaire")) //Questionnaire title
      }

    }
  }


  "Calling QuestionnaireController.thankyou" should {
    "not respond with NOT_FOUND" in {
      val result = route(FakeRequest(GET, "/ated/feedbackThankYou"))
      result.isDefined must be(true)
      status(result.get) must not be NOT_FOUND
    }

    "show the questionnaire thank you view" in {
      thankYouWithUnauthorisedUser {
        result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("Thanks for your feedback")) //Questionnaire Thankyou title

      }
    }
  }

  def showQuestionnaireWithUnauthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val result = TestQuestionnaireController.showQuestionnaire.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitQuestionnaireWithUnauthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val result = TestQuestionnaireController.submitQuestionnaire.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  def thankYouWithUnauthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val result = TestQuestionnaireController.feedbackThankYou.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
