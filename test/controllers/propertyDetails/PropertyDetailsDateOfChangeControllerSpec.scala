/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.propertyDetails

import builders.SessionBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.propertyDetails.propertyDetailsDateOfChange

class PropertyDetailsDateOfChangeControllerSpec extends PropertyDetailsTestFixture {

  val injectedViewInstance: propertyDetailsDateOfChange = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsDateOfChange]

  val testController: PropertyDetailsDateOfChangeController = new PropertyDetailsDateOfChangeController(
    mockMcc,
    mockAuthAction,
    mockServiceInfoService,
    injectedViewInstance,
    mockPropertyDetailsService,
    mockBackLinkCacheConnector,
    mockDataCacheConnector,
    mockNewValuationController
  )

  "PropertyDetailsDateOfChangeController.view" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup(enrolmentSet = invalidEnrolmentSet) {
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "render the date of change page" when {
      "newRevaluedFeature flag is set to true" in new Setup {
        setupPropertyDetailServiceMockExpectations()
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe OK
      }
    }

    "redirect to home page" when {
      "newRevaluedFeature flag is set to false" in new Setup(isFeatureFlagEnabled = false) {
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/home")
      }
    }

    "for page errors, return BAD_REQUEST" in new Setup {
      val inputJson: JsValue = Json.obj()
      setupPropertyDetailServiceMockExpectations()
      val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("There is a problem")
    }
  }

  "PropertyDetailsDateOfChangeController.save" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup(enrolmentSet = invalidEnrolmentSet) {
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "redirect to next page: new-valuation" when {
      "newRevaluedFeature flag is set to true and user enters valid date" in new Setup {
        val inputJson: JsValue = Json.obj(
          "dateOfChange" -> Json.obj("day" -> "1", "month" -> "4", "year" -> "2015")
        )
        setupPropertyDetailServiceMockExpectations()
        val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/liability/create/new-valuation/view")
      }
    }

    "return BAD_REQUEST with error message for invalid date" when {
      "user enters an invalid date (31st of February)" in new Setup {
        val inputJson: JsValue = Json.obj(
          "dateOfChange" -> Json.obj("day" -> "31", "month" -> "2", "year" -> "2024")
        )
        val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
        setupPropertyDetailServiceMockExpectations()
        val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("There is a problem")
      }
    }

    "return BAD_REQUEST with error message for missing date fields" when {
      "user omits some date fields" in new Setup {
        val inputJson: JsValue = Json.obj(
          "dateOfChange" -> Json.obj("day" -> "", "month" -> "4", "year" -> "2024")
        )
        setupPropertyDetailServiceMockExpectations()
        val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("There is a problem")
      }
    }

    "redirect to home page" when {
      "newRevaluedFeature flag is set to false" in new Setup(isFeatureFlagEnabled = false) {
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/home")
      }
    }
  }
}
