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

package controllers.propertyDetails

import builders.SessionBuilder
import models.DateOfRevalue
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, verify}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import utils.AtedConstants.HasPropertyBeenRevalued
import utils.AtedUtils.EDIT_FROM_SUMMARY
import views.html.propertyDetails.propertyDetailsDateOfRevalue

import java.time.LocalDate

class PropertyDetailsDateOfRevalueControllerSpec extends PropertyDetailsTestFixture with BeforeAndAfterEach {
  val injectedViewInstance: propertyDetailsDateOfRevalue = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsDateOfRevalue]
  val mockPropertyDetailsSummaryController: PropertyDetailsSummaryController = mock[PropertyDetailsSummaryController]
  

  val testController: PropertyDetailsDateOfRevalueController = new PropertyDetailsDateOfRevalueController(
    mockMcc,
    mockAuthAction,
    mockServiceInfoService,
    injectedViewInstance,
    mockPropertyDetailsService,
    mockBackLinkCacheService,
    mockDataCacheService,
    mockIsFullTaxPeriodController,
    mockPropertyDetailsSummaryController
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockPropertyDetailsService,
      mockDataCacheService,
      mockBackLinkCacheService
    )
  }

  "PropertyDetailsDateOfRevalueController.view" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup(enrolmentSet = invalidEnrolmentSet) {
        val result = testController.view("1", None).apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "render the date of revalue page" when {
      "user is authenticated" in new Setup {
        setupPropertyDetailServiceMockExpectations()
        val result = testController.view("1", None).apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe OK
      }
    }

    "preserve edit from summary mode when displaying the view" in new Setup {
      setupPropertyDetailServiceMockExpectations()

      val result = testController.view("1", Some(EDIT_FROM_SUMMARY))
        .apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))
      document.select("form").attr("action") must include("mode=editFromSummary")
    }

    "for page errors, return BAD_REQUEST" in new Setup {
      setupPropertyDetailServiceMockExpectations()
      val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
        .withMethod("POST")
        .withFormUrlEncodedBody()
      val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("There is a problem")
    }
  }

  "PropertyDetailsDateOfRevalueController.save" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in new Setup(enrolmentSet = invalidEnrolmentSet) {
        val result = testController.view("1", None).apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "save the date of revaluation to the database" when {
      "save invoked for a valid period" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "dateOfRevalue.day" -> "1",
            "dateOfRevalue.month" -> "4",
            "dateOfRevalue.year" -> "2020"
          )

        setupPropertyDetailServiceMockExpectations()

        val result = testController.save("1", 2020, None).apply(
          SessionBuilder.updateRequestFormWithSession(fakeRequest, userId)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/liability/create/full-tax-period/view")

        verifySaveDateOfRevaluation(LocalDate.of(2020, 4, 1))
        verifySaveBackLinkIsCalled
      }
    }

    "redirect to next page: full tax period" when {
      "user enters valid date" in new Setup {

        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "dateOfRevalue.day" -> "1",
            "dateOfRevalue.month" -> "4",
            "dateOfRevalue.year" -> "2015")
        setupPropertyDetailServiceMockExpectations()

        val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/liability/create/full-tax-period/view")
      }
    }

    "redirect to the property details summary page" when {
      "user enters valid date when editing from summary" in new Setup {

        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "dateOfRevalue.day" -> "1",
            "dateOfRevalue.month" -> "4",
            "dateOfRevalue.year" -> "2015"
          )
        setupPropertyDetailServiceMockExpectations()

        val result = testController.save("1", 2015, Some(EDIT_FROM_SUMMARY)).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("/ated/liability/create/summary/1")

        verifySaveDateOfRevaluation(LocalDate.of(2015, 4, 1))
        verifySaveBackLinkIsCalled
      }
    }

    "return BAD_REQUEST with error message for invalid date" when {
      "user enters an invalid date (31st of February)" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "dateOfRevalue.day" -> "31",
            "dateOfRevalue.month" -> "2",
            "dateOfRevalue.year" -> "2024")
        setupPropertyDetailServiceMockExpectations()
        val result = testController.save("1", 2015, None).apply(
            SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("There is a problem")
      }
    }

    "return BAD_REQUEST with error message for missing date fields" when {
      "user omits some date fields" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody(
            "dateOfRevalue.day" -> "",
            "dateOfRevalue.month" -> "4",
            "dateOfRevalue.year" -> "2024")
        val result = testController.save("1", 2015, None).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("There is a problem")
      }
    }
  }
}


