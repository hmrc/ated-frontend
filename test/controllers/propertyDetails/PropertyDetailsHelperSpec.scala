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

package controllers.propertyDetails

import java.util.UUID

import builders.{PropertyDetailsBuilder, SessionBuilder, AuthBuilder}
import builders.AuthBuilder._
import connectors.BackLinkCacheConnector
import play.api.mvc.Result
import play.api.test.Helpers._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.PartialFunctionValues
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Results._
import services._
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}

import scala.concurrent.Future

class PropertyDetailsHelperSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with PartialFunctionValues {

  val mockAuthConnector = mock[AuthConnector]
  val mockPropertyDetailsService = mock[PropertyDetailsService]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]

  object TestPropertyDetailsHelpers extends PropertyDetailsHelpers {
    val delegationConnector = mockDelegationConnector
    val propertyDetailsService = mockPropertyDetailsService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
  }

  "PropertyDetailsHelpers" must {

    "return successful cache response for an OK response" in {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1")
      getDataWithAuthorisedUser(PropertyDetailsCacheSuccessResponse(propertyDetails)) {
        result =>
          status(result) must be(OK)
      }
    }

    "redirect to account summary when data not found in cache" in {
      getDataWithAuthorisedUser(PropertyDetailsCacheNotFoundResponse) {
        result =>
          status(result) must be(SEE_OTHER)
      }
    }

    "redirect to account summary when an error occurs" in {
      getDataWithAuthorisedUser(PropertyDetailsCacheErrorResponse) {
        result =>
          status(result) must be(SEE_OTHER)
      }
    }
  }

  def getDataWithAuthorisedUser(cacheSuccessResponse: PropertyDetailsCacheResponse)(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1")
    when(mockPropertyDetailsService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cacheSuccessResponse))
    val result = TestPropertyDetailsHelpers.propertyDetailsCacheResponse("1") {
      case PropertyDetailsCacheSuccessResponse(_) => Future.successful(Ok)
    }
    test(result)
  }

}
