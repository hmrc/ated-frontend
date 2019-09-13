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

package services

import builders.PropertyDetailsBuilder
import connectors.{DataCacheConnector, PropertyDetailsConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}

import scala.concurrent.Future

class PropertyDetailsServiceSpec_Values extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockConnector: PropertyDetailsConnector = mock[PropertyDetailsConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  object TestPropertyDetailsService extends PropertyDetailsService {
    override val atedConnector: PropertyDetailsConnector = mockConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach: Unit = {
    reset(mockConnector)
    reset(mockDataCacheConnector)
  }

  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "PropertyDetailsService" must {
    "use the correct connector" in {
      PropertyDetailsService.atedConnector must be(PropertyDetailsConnector)
    }

    "Save property Details Has Value Changed" must {
      "save the value and return the response from the connector" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftHasValueChanged(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftHasValueChanged("1", hasValueChanged = true)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = propertyDetails.title
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftHasValueChanged(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftHasValueChanged("1", hasValueChanged = true)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftHasValueChanged] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details Acquisition" must {
      "save the value and return the response from the connector" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftPropertyDetailsAcquisition(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsAcquisition("1", overLimit = true)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = propertyDetails.title
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftPropertyDetailsAcquisition(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsAcquisition("1", overLimit = true)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsAcquisition] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details Revalued" must {
      "save the value and return the response from the connector" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = new PropertyDetailsRevalued()
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftPropertyDetailsRevalued(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsRevalued("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val propValue = new PropertyDetailsRevalued()
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftPropertyDetailsRevalued(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsRevalued("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsRevalued] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details OwnedBefore" must {
      "save the value and return the response from the connector" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = new PropertyDetailsOwnedBefore()
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftPropertyDetailsOwnedBefore(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsOwnedBefore("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val propValue = new PropertyDetailsOwnedBefore()
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftPropertyDetailsOwnedBefore(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsOwnedBefore("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsOwnedBefore] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details NewBuild" must {
      "save the value and return the response from the connector" in {

        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = new PropertyDetailsNewBuild()

        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftPropertyDetailsNewBuild(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsNewBuild("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val propValue = new PropertyDetailsNewBuild()
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftPropertyDetailsNewBuild(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsNewBuild("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsNewBuild] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details ProfessionallyValued" must {
      "save the value and return the response from the connector" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = new PropertyDetailsProfessionallyValued()
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftPropertyDetailsProfessionallyValued(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsProfessionallyValued("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val propValue = new PropertyDetailsProfessionallyValued()
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftPropertyDetailsProfessionallyValued(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsProfessionallyValued("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be
        s"[PropertyDetailsService][saveDraftPropertyDetailsProfessionallyValued] Invalid status when saving Property Details :$BAD_REQUEST"

      }
    }

  }

}
