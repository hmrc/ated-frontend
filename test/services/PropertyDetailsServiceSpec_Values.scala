/*
 * Copyright 2022 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.{DataCacheConnector, PropertyDetailsConnector}
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}

import scala.concurrent.Future

class PropertyDetailsServiceSpec_Values extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneServerPerSuite {

  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val mockPropertyDetailsConnector: PropertyDetailsConnector = mock[PropertyDetailsConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  class Setup {
    val testPropertyDetailsService: PropertyDetailsService = new PropertyDetailsService(
      mockPropertyDetailsConnector,
      mockDataCacheConnector
    )
  }

  override def beforeEach: Unit = {
  }

  "PropertyDetailsService" must {
    "Save property Details Has Value Changed" must {
      "save the value and return the response from the connector" in new Setup {

        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftHasValueChanged(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftHasValueChanged("1", hasValueChanged = true)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue: Option[PropertyDetailsTitle] = propertyDetails.title
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftHasValueChanged(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftHasValueChanged("1", hasValueChanged = true)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftHasValueChanged] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details Acquisition" must {
      "save the value and return the response from the connector" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsAcquisition(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsAcquisition("1", overLimit = true)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue: Option[PropertyDetailsTitle] = propertyDetails.title
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsAcquisition(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsAcquisition("1", overLimit = true)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsAcquisition] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details Revalued" must {
      "save the value and return the response from the connector" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = new PropertyDetailsRevalued()
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsRevalued(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsRevalued("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val propValue = new PropertyDetailsRevalued()
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsRevalued(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsRevalued("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsRevalued] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details OwnedBefore" must {
      "save the value and return the response from the connector" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = new PropertyDetailsOwnedBefore()
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsOwnedBefore(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsOwnedBefore("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val propValue = new PropertyDetailsOwnedBefore()
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsOwnedBefore(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsOwnedBefore("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsOwnedBefore] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details NewBuild" must {
      "save the value and return the response from the connector" in new Setup {

        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = new PropertyDetailsNewBuild()

        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsNewBuild(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsNewBuild("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val propValue = new PropertyDetailsNewBuild()
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsNewBuild(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsNewBuild("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsNewBuild] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details ProfessionallyValued" must {
      "save the value and return the response from the connector" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = new PropertyDetailsProfessionallyValued()
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsProfessionallyValued(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsProfessionallyValued("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val propValue = new PropertyDetailsProfessionallyValued()
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsProfessionallyValued(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsProfessionallyValued("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be
        s"[PropertyDetailsService][saveDraftPropertyDetailsProfessionallyValued] Invalid status when saving Property Details :$BAD_REQUEST"

      }
    }

  }

}
