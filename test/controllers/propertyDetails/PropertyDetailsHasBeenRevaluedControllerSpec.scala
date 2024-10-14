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
import models.HasBeenRevalued
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.propertyDetails.propertyDetailsHasBeenRevalued

class PropertyDetailsHasBeenRevaluedControllerSpec extends PropertyDetailsTestFixture {
  val injectedViewInstance: propertyDetailsHasBeenRevalued = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsHasBeenRevalued]

  val testController: PropertyDetailsHasBeenRevaluedController = new PropertyDetailsHasBeenRevaluedController(
    mockMcc,
    mockAuthAction,
    injectedViewInstance,
    mockServiceInfoService,
    mockPropertyDetailsService,
    mockBackLinkCacheConnector,
    mockDataCacheConnector,
    mockDateOfChangeController,
    mockExitController
  )

  "PropertyDetailsHasBeenRevaluedController.view" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup(enrolmentSet = invalidEnrolmentSet) {
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "render the has been revalued page" when {
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

  "PropertyDetailsHasBeenRevaluedController.save" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup(enrolmentSet = invalidEnrolmentSet) {
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "redirect to next page: date-of-change" when {
      "newRevaluedFeature flag is set to true and user clicks yes" in new Setup {
        val inputJson: JsValue = Json.toJson(HasBeenRevalued(Some(true)))
        setupPropertyDetailServiceMockExpectations()
        val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/liability/create/date-of-change/view")
      }
    }

    "render the exit page" when {
      "newRevaluedFeature flag is set to true and user clicks no" in new Setup {
        val inputJson: JsValue = Json.toJson(HasBeenRevalued(Some(false)))
        setupPropertyDetailServiceMockExpectations()
        val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/liability/create/cannot-submit-return")
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
