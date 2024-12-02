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
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.propertyDetails.propertyDetailsExit

class PropertyDetailsExitControllerSpec extends PropertyDetailsTestFixture {

  val injectedViewInstance: propertyDetailsExit = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsExit]

  val testController: PropertyDetailsExitController = new PropertyDetailsExitController(
    mockMcc,
    mockAuthAction,
    mockPropertyDetailsService,
    mockDataCacheConnector,
    mockBackLinkCacheConnector,
    injectedViewInstance
  )


  "PropertyDetailsExitController.view" must {
    "redirect to the unauthorised page" when {
      "user fails authentication" in {
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
        setInvalidAuthMocks(authMock)
        val result = testController.view().apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("ated/unauthorised")
      }
    }

    "render the Exit page" when {
      "user is authenticated" in {
        setupAuthForOrganisation()
        setupCommonMockExpectations()
        val result = testController.view().apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe OK
      }
    }
  }
}
