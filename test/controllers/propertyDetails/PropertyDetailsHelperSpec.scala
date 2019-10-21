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

import builders.PropertyDetailsBuilder
import connectors.BackLinkCacheConnector
import models.StandardAuthRetrievals
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.PartialFunctionValues
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.mvc.Results._
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.MockAuthUtil

import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsHelperSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with PartialFunctionValues with MockAuthUtil {

  implicit val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  object TestPropertyDetailsHelpers extends PropertyDetailsHelpers {
    implicit val ec: ExecutionContext = mockMcc.executionContext
   val delegationService: DelegationService = mockDelegationService
    val propertyDetailsService: PropertyDetailsService = mockPropertyDetailsService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
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

  def getDataWithAuthorisedUser(cacheSuccessResponse: PropertyDetailsCacheResponse)(test: Future[Result] => Any) {
    when(mockPropertyDetailsService.retrieveDraftPropertyDetails
    (Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cacheSuccessResponse))

    val result = TestPropertyDetailsHelpers.propertyDetailsCacheResponse("1") {
      case PropertyDetailsCacheSuccessResponse(_) => Future.successful(Ok)
    }
    test(result)
  }

}
