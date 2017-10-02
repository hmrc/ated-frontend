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
import models.{PropertyDetailsPeriod, PropertyDetailsValue, SubmitReturnsResponse}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, InternalServerException }

class PropertyDetailsServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

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

    "Create property Details" must {
      "save the address ref and return the response from the connector" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.retrieveDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        when(mockConnector.createDraftPropertyDetails(Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.createDraftPropertyDetailsAddress(2015, propertyDetails.addressProperty)
        await(result) must be("1")

      }

      "save and throw an Exception if it fails" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val addressProperty = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("postCode"))
        val successResponse = Json.toJson("1")
        when(mockConnector.createDraftPropertyDetails(Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.createDraftPropertyDetailsAddress(2015, addressProperty)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][createDraftPropertyDetails] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details Address" must {
      "save the address ref and return the response from the connector" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val addressProperty = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("postCode"))

        val successResponse = Json.toJson(addressProperty)
        when(mockConnector.saveDraftPropertyDetailsAddressRef(Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsAddress("1", addressProperty)
        await(result) must be("1")

      }

      "save and throw an Exception if it fails" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val addressProperty = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("postCode"))
        val successResponse = Json.toJson(addressProperty)
        when(mockConnector.saveDraftPropertyDetailsAddressRef(Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsAddress("1", addressProperty)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsAddressRef] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details Title" must {
      "save the value and return the response from the connector" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = propertyDetails.title

        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.saveDraftPropertyDetailsTitle(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsTitle("1", propValue.get)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue = propertyDetails.title
        val successResponse = Json.toJson(propValue)
        when(mockConnector.saveDraftPropertyDetailsTitle(Matchers.eq("1"), Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.saveDraftPropertyDetailsTitle("1", propValue.get)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsTitle] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Calculate property Details" must {
      "does not save the new calculated data and return blank OK responsereturn the response from the connector" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))

        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.retrieveDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        when(mockConnector.calculateDraftPropertyDetails(Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.calculateDraftPropertyDetails("1")
        await(result).status must be(OK)

      }

      "save the new calculated data and return the response from the connector" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsValuedByAgent("1", Some("postCode"))
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.retrieveDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        when(mockConnector.calculateDraftPropertyDetails(Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.calculateDraftPropertyDetails("1")
        await(result).status must be(OK)

      }

    }

    "Calculate Change Liablity property Details" must {
      "save the new calculated data and return the response from the connector" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsValuedByAgent("1", Some("postCode"))
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.retrieveDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        when(mockConnector.calculateDraftChangeLiability(Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.calculateDraftChangeLiability("1")
        await(result) must be(propertyDetails)

      }

      "save and throw an Exception if it fails" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsValuedByAgent("1", Some("postCode"))
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.retrieveDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        when(mockConnector.calculateDraftChangeLiability(Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result = TestPropertyDetailsService.calculateDraftChangeLiability("1")
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][calculateDraftChangeLiability] Invalid status when calculating Property Details :$BAD_REQUEST")

      }
    }

    "Retrieve property Details" must {
      "return the response from the connector with a Success Response" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("new Prop Det"))
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.retrieveDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.retrieveDraftPropertyDetails("1")
        await(result) must be(PropertyDetailsCacheSuccessResponse(propertyDetails))
      }

      "retrieve data and return a not found response if no data is found" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("new Prop Det"))

        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.retrieveDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(successResponse))))

        val result = TestPropertyDetailsService.retrieveDraftPropertyDetails("1")
        await(result) must be(PropertyDetailsCacheNotFoundResponse)
      }

      "retrieve data and return an error response if it fails" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("new Prop Det"))

        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.retrieveDraftPropertyDetails(Matchers.eq("1"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(successResponse))))

        val result = TestPropertyDetailsService.retrieveDraftPropertyDetails("1")
        await(result) must be(PropertyDetailsCacheErrorResponse)
      }
    }

    "Submit property Details" must {
      "submit and return the response from the connector" in {
        val jsonEtmpResponse =
          """
            |{
            |  "processingDate": "2001-12-17T09:30:47Z",
            |  "liabilityReturnResponse": [
            |    {
            |      "mode": "Post",
            |      "propertyKey": "1",
            |      "liabilityAmount": 1234.12,
            |      "paymentReference": "aaaaaaaaaaaaaa",
            |      "formBundleNumber": "012345678912"
            |    }
            |  ]
            |}
          """.stripMargin

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val successResponse = Json.parse(jsonEtmpResponse)
        when(mockConnector.submitDraftPropertyDetails(Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val SubmitReturnsResponseFormId = "submit-returns-response-Id"
        when(mockDataCacheConnector.saveFormData[SubmitReturnsResponse](Matchers.eq(SubmitReturnsResponseFormId),
          Matchers.eq(successResponse.as[SubmitReturnsResponse]))
          (Matchers.any(), Matchers.any(), Matchers.eq(SubmitReturnsResponse.formats)))
          .thenReturn(Future.successful(successResponse.as[SubmitReturnsResponse]))

        val result = TestPropertyDetailsService.submitDraftPropertyDetails("1")
        val response = await(result)
        verify(mockDataCacheConnector, times(1)).clearCache()(Matchers.any())
      }

    }

    "clear chargeable draft" when {
       "valid id is passed" in {
         implicit val hc: HeaderCarrier = HeaderCarrier()
         val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("new Prop Det"))
         val successResponse = Json.toJson(Seq(propertyDetails))
         when(mockConnector.deleteDraftChargeable(Matchers.eq("AB12345"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

         val result = TestPropertyDetailsService.clearDraftReliefs("AB12345")
         val response = await(result)
         verify(mockConnector, times(1)).deleteDraftChargeable(Matchers.any())(Matchers.any(), Matchers.any())
       }
    }

    "validate whether to make the calculateDraftPropertyDetails call to ETMP"  when {
      "for a valid form bundle number but property no period is found" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsWithNoValue("1", Some("new Prop Det"))
        val successResponse = Json.toJson(propertyDetails)
        when(mockConnector.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestPropertyDetailsService.validateCalculateDraftPropertyDetails("AB12345")
        val response = await(result)
        verify(mockConnector, times(1)).retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())
      }
    }
  }

}
