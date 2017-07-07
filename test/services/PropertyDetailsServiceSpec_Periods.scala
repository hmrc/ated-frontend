/*
 * Copyright 2017 HM Revenue & Customs
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

import builders.{AuthBuilder, PropertyDetailsBuilder}
import connectors.{DataCacheConnector, PropertyDetailsConnector}
import models._
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, InternalServerException}

import scala.concurrent.Future

class PropertyDetailsServiceSpec_Periods extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockConnector = mock[PropertyDetailsConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestPropertyDetailsService extends PropertyDetailsService {
    override val atedConnector = mockConnector
    override val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach = {
    reset(mockConnector)
    reset(mockDataCacheConnector)
  }

  implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "PropertyDetailsService" must {
    "use the correct connector" in {
      PropertyDetailsService.atedConnector must be(PropertyDetailsConnector)
    }

    "Save property Details IsFullTaxPeriod" must {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new IsFullTaxPeriod(false, None)


      "save the value and return the response from the connector" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftIsFullTaxPeriod(Matchers.eq("1"), Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftIsFullTaxPeriod("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftIsFullTaxPeriod(Matchers.eq("1"), Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftIsFullTaxPeriod("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftIsFullTaxPeriod] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details IsInRelief" must {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsInRelief()


      "save the value and return the response from the connector" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftPropertyDetailsInRelief(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsInRelief("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftPropertyDetailsInRelief(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsInRelief("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsInRelief] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details TaxAvoidance" must {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsTaxAvoidance()


      "save the value and return the response from the connector" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftPropertyDetailsTaxAvoidance(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsTaxAvoidance("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftPropertyDetailsTaxAvoidance(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsTaxAvoidance("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsTaxAvoidance] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details DatesLiable" must {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsDatesLiable(new LocalDate("1970-01-01"), new LocalDate("1970-01-01"))


      "save the value and return the response from the connector" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftPropertyDetailsDatesLiable(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsDatesLiable("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftPropertyDetailsDatesLiable(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsDatesLiable("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "add DatesLiable" must {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsDatesLiable(new LocalDate("1970-01-01"), new LocalDate("1970-01-01"))


      "add the value and return the response from the connector" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.addDraftPropertyDetailsDatesLiable(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.addDraftPropertyDetailsDatesLiable("1", propValue)
        await(result) must be(OK)

      }

      "add and throw an Exception if it fails" in {
        val successResponse = Json.toJson(propValue)
        when(mockConnector.addDraftPropertyDetailsDatesLiable(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.addDraftPropertyDetailsDatesLiable("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][addDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }
    
    "store Chosen Relief" must {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val propValue = new PeriodChooseRelief("reliefDescription")

      "add the chosen relief to the data cache" in {
        when(mockDataCacheConnector.saveFormData(Matchers.any(), Matchers.eq(propValue))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(propValue))

        val result = TestPropertyDetailsService.storeChosenRelief(propValue)
        await(result) must be(propValue)

      }

    }


    "add DatesInRelief" must {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsDatesInRelief(new LocalDate("1970-01-01"), new LocalDate("1970-01-01"))


      "add the value and return the response from the connector" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockDataCacheConnector.fetchAndGetFormData[PeriodChooseRelief](Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(PeriodChooseRelief("reliefDescription"))))
        when(mockConnector.addDraftPropertyDetailsDatesInRelief(Matchers.eq("1"), Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.addDraftPropertyDetailsDatesInRelief("1", propValue)
        await(result) must be(OK)

      }

      "add and throw an Exception if it fails" in {
        val successResponse = Json.toJson(propValue)
        when(mockDataCacheConnector.fetchAndGetFormData[PeriodChooseRelief](Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(PeriodChooseRelief("reliefDescription"))))
        when(mockConnector.addDraftPropertyDetailsDatesInRelief(Matchers.eq("1"), Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.addDraftPropertyDetailsDatesInRelief("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][addDraftPropertyDetailsDatesInRelief] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "delete period" must {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsDatesInRelief(new LocalDate("1970-01-01"), new LocalDate("1970-01-01"))


      "delete the value and return the response from the connector" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.deleteDraftPropertyDetailsPeriod(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.deleteDraftPropertyDetailsPeriod("1", propValue.startDate)
        await(result) must be(propertyDetails)

      }

      "delete and throw an Exception if it fails" in {
        val successResponse = Json.toJson(propValue)
        when(mockConnector.deleteDraftPropertyDetailsPeriod(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.deleteDraftPropertyDetailsPeriod("1", propValue.startDate)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][deleteDraftPropertyDetailsPeriod] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details SupportingInfo" must {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
      val propValue = new PropertyDetailsSupportingInfo("")


      "save the value and return the response from the connector" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftPropertyDetailsSupportingInfo(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsSupportingInfo("1", propValue)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftPropertyDetailsSupportingInfo(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsSupportingInfo("1", propValue)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsSupportingInfo] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

  }

}
