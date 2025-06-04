/*
 * Copyright 2023 HM Revenue & Customs
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
import java.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsServiceSpec_Periods extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with Injecting {

  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  implicit val ec: ExecutionContext = inject[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val mockPropertyDetailsConnector: PropertyDetailsConnector = mock[PropertyDetailsConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

 class Setup {
   val testPropertyDetailsService: PropertyDetailsService = new PropertyDetailsService(
     mockPropertyDetailsConnector,
     mockDataCacheConnector
   )
 }

  override def beforeEach(): Unit = {
  }

  "PropertyDetailsService" must {
    "Save property Details IsFullTaxPeriod" must {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new IsFullTaxPeriod(false, None)


      "save the value and return the response from the connector" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftIsFullTaxPeriod(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftIsFullTaxPeriod("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftIsFullTaxPeriod(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftIsFullTaxPeriod("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftIsFullTaxPeriod] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details IsInRelief" must {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsInRelief()


      "save the value and return the response from the connector" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsInRelief(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsInRelief("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsInRelief(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsInRelief("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsInRelief] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details TaxAvoidance reference" must {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsTaxAvoidanceReferences()


      "save the value and return the response from the connector" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsTaxAvoidanceReferences(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsTaxAvoidanceReferences("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsTaxAvoidanceReferences(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsTaxAvoidanceReferences("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsTaxAvoidance] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details TaxAvoidance scheme" must {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsTaxAvoidanceScheme()


      "save the value and return the response from the connector" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsTaxAvoidanceScheme(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsTaxAvoidanceScheme("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsTaxAvoidanceScheme(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsTaxAvoidanceScheme("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsTaxAvoidanceScheme] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details DatesLiable" must {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsDatesLiable(Some(LocalDate.parse("1970-01-01")), Some(LocalDate.parse("1970-01-01")))


      "save the value and return the response from the connector" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsDatesLiable(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsDatesLiable("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsDatesLiable(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsDatesLiable("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "add DatesLiable" must {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsDatesLiable(Some(LocalDate.parse("1970-01-01")), Some(LocalDate.parse("1970-01-01")))


      "add the value and return the response from the connector" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.addDraftPropertyDetailsDatesLiable(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.addDraftPropertyDetailsDatesLiable("1", propValue)
        await(result) must be(OK)

      }

      "add and throw an Exception if it fails" in new Setup {
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.addDraftPropertyDetailsDatesLiable(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.addDraftPropertyDetailsDatesLiable("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][addDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "store Chosen Relief" must {
      val propValue = new PeriodChooseRelief("reliefDescription")

      "add the chosen relief to the data cache" in new Setup {
        when(mockDataCacheConnector.saveFormData(ArgumentMatchers.any(), ArgumentMatchers.eq(propValue))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(propValue))

        val result: Future[PeriodChooseRelief] = testPropertyDetailsService.storeChosenRelief(propValue)
        await(result) must be(propValue)

      }

    }


    "add DatesInRelief" must {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsDatesInRelief(Some(LocalDate.parse("1970-01-01")), Some(LocalDate.parse("1970-01-01")))


      "add the value and return the response from the connector" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockDataCacheConnector.fetchAndGetData[PeriodChooseRelief](ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(PeriodChooseRelief("reliefDescription"))))
        when(mockPropertyDetailsConnector.addDraftPropertyDetailsDatesInRelief(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.addDraftPropertyDetailsDatesInRelief("1", propValue)
        await(result) must be(OK)

      }

      "add and throw an Exception if it fails" in new Setup {
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockDataCacheConnector.fetchAndGetData[PeriodChooseRelief](ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(PeriodChooseRelief("reliefDescription"))))
        when(mockPropertyDetailsConnector.addDraftPropertyDetailsDatesInRelief(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.addDraftPropertyDetailsDatesInRelief("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][addDraftPropertyDetailsDatesInRelief] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "delete period" must {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsDatesInRelief(Some(LocalDate.parse("1970-01-01")), Some(LocalDate.parse("1970-01-01")))


      "delete the value and return the response from the connector" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.deleteDraftPropertyDetailsPeriod(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[PropertyDetails] = testPropertyDetailsService.deleteDraftPropertyDetailsPeriod("1", propValue.startDate.get)
        await(result) must be(propertyDetails)

      }

      "delete and throw an Exception if it fails" in new Setup {
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.deleteDraftPropertyDetailsPeriod(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[PropertyDetails] = testPropertyDetailsService.deleteDraftPropertyDetailsPeriod("1", propValue.startDate.get)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][deleteDraftPropertyDetailsPeriod] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details SupportingInfo" must {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsSupportingInfo("")


      "save the value and return the response from the connector" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsSupportingInfo(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsSupportingInfo("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsSupportingInfo(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsSupportingInfo("1", propValue)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsSupportingInfo] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }
  }
}
