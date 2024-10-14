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
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import utils.AtedConstants.HasPropertyBeenRevalued
import views.html.propertyDetails.propertyDetailsDateOfRevalue

import java.time.LocalDate

class PropertyDetailsDateOfRevalueControllerSpec extends PropertyDetailsTestFixture {
  val injectedViewInstance: propertyDetailsDateOfRevalue = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsDateOfRevalue]

  val testController: PropertyDetailsDateOfRevalueController = new PropertyDetailsDateOfRevalueController(
    mockMcc,
    mockAuthAction,
    mockServiceInfoService,
    injectedViewInstance,
    mockPropertyDetailsService,
    mockBackLinkCacheConnector,
    mockDataCacheConnector,
    mockIsFullTaxPeriodController
  )

  "PropertyDetailsDateOfRevalueController.view" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup(enrolmentSet = invalidEnrolmentSet) {
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "render the date of revalue page" when {
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
      setupPropertyDetailServiceMockExpectations()
      val inputJson: JsValue = Json.obj()
      val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("There is a problem")
    }
  }

  "PropertyDetailsDateOfRevalueController.save" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup(enrolmentSet = invalidEnrolmentSet) {
        val result = testController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "collect information from cache and save to database" when {
      "newRevaluedFeature flag is set to true and save invoked for a valid period" in new Setup {
        val inputJson: JsValue = Json.obj(
          "dateOfRevalue" -> Json.obj("day" -> 1, "month" -> 4, "year" -> 2020)
        )
        setupDataCacheConnectorExpectations(
          newValuation = Some(BigDecimal.valueOf(1000000)),
          hasPropertyBeenRevalued = Some(true),
          dateOfRevaluationChange = Some(LocalDate.of(2021, 6, 15))
        )
        setupPropertyDetailServiceMockExpectations()

        val result = testController.save("1", 2020, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/liability/create/full-tax-period/view")

        verifyPropertyDetailsService(Some(true), Some(1000000), Some(LocalDate.of(2020, 4, 1)), Some(LocalDate.of(2021, 6, 15)))
        verifyDataCacheConnectorRetursHasBeenRevalued(HasPropertyBeenRevalued)
        verifySaveBackLinkIsCalled
      }
    }

    "redirect to next page: full tax period" when {
      "newRevaluedFeature flag is set to true and user enters valid date" in new Setup {
        val inputJson: JsValue = Json.obj(
          "dateOfRevalue" -> Json.obj("day" -> "1", "month" -> "4", "year" -> "2015")
        )

        setupPropertyDetailServiceMockExpectations()

        val result = testController.save("1", 2015, None).
          apply(
            SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson),
              userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/liability/create/full-tax-period/view")
      }
    }

    "return BAD_REQUEST with error message for invalid date" when {
      "user enters an invalid date (31st of February)" in new Setup {
        val inputJson: JsValue = Json.obj(
          "dateOfRevalue" -> Json.obj("day" -> "31", "month" -> "2", "year" -> "2024")
        )
        setupPropertyDetailServiceMockExpectations()
        val result = testController.save("1", 2015, None)
          .apply(
            SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson),
              userId))
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("There is a problem")
      }
    }

    "return BAD_REQUEST with error message for missing date fields" when {
      "user omits some date fields" in new Setup {
        val inputJson: JsValue = Json.obj(
          "dateOfRevalue" -> Json.obj("day" -> "", "month" -> "4", "year" -> "2024")
        )

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


