/*
 * Copyright 2020 HM Revenue & Customs
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

class PropertyDetailsServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  implicit  val mockAppCongfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val mockPropertyDetailsConnector : PropertyDetailsConnector = mock[PropertyDetailsConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

 class Setup {
   val testPropertyDetailsService: PropertyDetailsService = new PropertyDetailsService(
     mockPropertyDetailsConnector,
     mockDataCacheConnector
   )
 }
  override def beforeEach: Unit = {
    reset(mockPropertyDetailsConnector)
  }

  "PropertyDetailsService" must {
    "Create property Details" must {
      "save the address ref and return the response from the connector" in new Setup {

        val periodKey: Int = 2015
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.retrieveDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        when(mockPropertyDetailsConnector.createDraftPropertyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[String] = testPropertyDetailsService.createDraftPropertyDetailsAddress(periodKey, propertyDetails.addressProperty)
        await(result) must be("1")

      }

      "save and throw an Exception if it fails" in new Setup {
        val periodKey: Int = 2015
        val addressProperty: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("postCode"))
        val successResponse: JsValue = Json.toJson("1")
        when(mockPropertyDetailsConnector.createDraftPropertyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result: Future[String] = testPropertyDetailsService.createDraftPropertyDetailsAddress(periodKey, addressProperty)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][createDraftPropertyDetails] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details Address" must {
      "save the address ref and return the response from the connector" in new Setup {

        val addressProperty: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("postCode"))

        val successResponse: JsValue = Json.toJson(addressProperty)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsAddressRef(ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[String] = testPropertyDetailsService.saveDraftPropertyDetailsAddress("1", addressProperty)
        await(result) must be("1")

      }

      "save and throw an Exception if it fails" in new Setup {
        val addressProperty: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("postCode"))
        val successResponse: JsValue = Json.toJson(addressProperty)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsAddressRef(ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result: Future[String] = testPropertyDetailsService.saveDraftPropertyDetailsAddress("1", addressProperty)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsAddressRef] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property Details Title" must {
      "save the value and return the response from the connector" in new Setup {

        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue: Option[PropertyDetailsTitle] = propertyDetails.title

        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsTitle(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsTitle("1", propValue.get)
        await(result) must be(OK)

      }

      "save and throw an Exception if it fails" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
        val propValue: Option[PropertyDetailsTitle] = propertyDetails.title
        val successResponse: JsValue = Json.toJson(propValue)
        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsTitle(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsTitle("1", propValue.get)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsTitle] Invalid status when saving Property Details :$BAD_REQUEST")

      }
    }

    "Save property details New Build Dates" should {
      "return OK" in new Setup {
        val newBuildDates: Option[PropertyDetailsNewBuildDates] = PropertyDetailsBuilder.getPropertyDetailsNewBuildDates
        val successResponse: JsValue = Json.toJson(newBuildDates)

        when(mockPropertyDetailsConnector.saveDraftPropertyDetailNewBuildDates(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsNewBuildDates("1", newBuildDates.get)
        await(result) must be(OK)
      }

      "return an exception if the save fails" in new Setup {
        val newBuildDates: Option[PropertyDetailsNewBuildDates] = PropertyDetailsBuilder.getPropertyDetailsNewBuildDates
        val successResponse: JsValue = Json.toJson(newBuildDates)

        when(mockPropertyDetailsConnector.saveDraftPropertyDetailNewBuildDates(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsNewBuildDates("1", newBuildDates.get)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)

        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsNewBuildDates] Invalid status when saving Property Details :$BAD_REQUEST")
      }
    }

    "Save property details when acquired dates" should {
      "return OK" in new Setup {
        val buildAcquiredDates: Option[PropertyDetailsWhenAcquiredDates] = PropertyDetailsBuilder.getPropertyDetailsWhenAcquired
        val successResponse: JsValue = Json.toJson(buildAcquiredDates)

        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsWhenAcquiredDates(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsWhenAcquiredDates("1", buildAcquiredDates.get)
        await(result) must be(OK)
      }

      "return an exception if the save fails" in new Setup {
        val buildAcquiredDates: Option[PropertyDetailsWhenAcquiredDates] = PropertyDetailsBuilder.getPropertyDetailsWhenAcquired
        val successResponse: JsValue = Json.toJson(buildAcquiredDates)

        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsWhenAcquiredDates(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsWhenAcquiredDates("1", buildAcquiredDates.get)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)

        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsWhenAcquiredDates] Invalid status when saving Property Details :$BAD_REQUEST")
      }
    }

    "Save property details new build value " should {
      "return OK" in new Setup {
        val newBuildValue: Option[PropertyDetailsNewBuildValue] = PropertyDetailsBuilder.getPropertyDetailsNewBuildValue
        val successResponse: JsValue = Json.toJson(newBuildValue)

        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsNewBuildValue(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsNewBuildValue("1",newBuildValue.get)
        await(result) must be(OK)
      }

      "return an exception if the save fails" in new Setup {
        val newBuildValue: Option[PropertyDetailsNewBuildValue] = PropertyDetailsBuilder.getPropertyDetailsNewBuildValue
        val successResponse: JsValue = Json.toJson(newBuildValue)

        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsNewBuildValue(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsNewBuildValue("1",newBuildValue.get)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)

        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsNewBuildValue] Invalid status when saving Property Details :$BAD_REQUEST")
      }
    }

    "Save property details value acquired" should {
      "return OK" in new Setup {
        val valueOnAcquisition : Option[PropertyDetailsValueOnAcquisition] = PropertyDetailsBuilder.getPropertyDetailsValueOnAcquisition
        val successResponse: JsValue = Json.toJson(valueOnAcquisition)

        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsValueAcquired(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsValueAcquired("1",valueOnAcquisition.get)
        await(result) must be(OK)

      }

      "return an exception if the save fails" in new Setup {
        val valueOnAcquisition : Option[PropertyDetailsValueOnAcquisition] = PropertyDetailsBuilder.getPropertyDetailsValueOnAcquisition
        val successResponse: JsValue = Json.toJson(valueOnAcquisition)

        when(mockPropertyDetailsConnector.saveDraftPropertyDetailsValueAcquired(ArgumentMatchers.eq("1"), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result: Future[Int] = testPropertyDetailsService.saveDraftPropertyDetailsValueAcquired("1",valueOnAcquisition.get)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)

        thrown.getMessage must be(s"[PropertyDetailsService][saveDraftPropertyDetailsValueAcquired] Invalid status when saving Property Details :$BAD_REQUEST")
      }
    }

    "Calculate property Details" must {
      "does not save the new calculated data and return blank OK response return the response from the connector" in new Setup {

        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))

        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.retrieveDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        when(mockPropertyDetailsConnector.calculateDraftPropertyDetails(ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsService.calculateDraftPropertyDetails("1")
        await(result).status must be(OK)

      }

      "save the new calculated data and return the response from the connector" in new Setup {

        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetailsValuedByAgent("1", Some("postCode"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.retrieveDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        when(mockPropertyDetailsConnector.calculateDraftPropertyDetails(ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsService.calculateDraftPropertyDetails("1")
        await(result).status must be(OK)

      }

    }

    "Calculate Change Liability property Details" must {
      "return the property details if the calculation is successful" in new Setup {

        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetailsValuedByAgent("1", Some("postCode"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.retrieveDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        when(mockPropertyDetailsConnector.calculateDraftChangeLiability(ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[Option[PropertyDetails]] = testPropertyDetailsService.calculateDraftChangeLiability("1")
        await(result) must be(Some(propertyDetails))

      }

      "not return property details if the calculation is unsuccessful" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetailsValuedByAgent("1", Some("postCode"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.retrieveDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        when(mockPropertyDetailsConnector.calculateDraftChangeLiability(ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(successResponse))))

        val result: Future[Option[PropertyDetails]] = testPropertyDetailsService.calculateDraftChangeLiability("1")
        await(result) must be(None)

      }
    }

    "Retrieve property Details" must {
      "return the response from the connector with a Success Response" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("new Prop Det"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.retrieveDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[PropertyDetailsCacheResponse] = testPropertyDetailsService.retrieveDraftPropertyDetails("1")
        await(result) must be(PropertyDetailsCacheSuccessResponse(propertyDetails))
      }

      "retrieve data and return a not found response if no data is found" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("new Prop Det"))

        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.retrieveDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(successResponse))))

        val result: Future[PropertyDetailsCacheResponse] = testPropertyDetailsService.retrieveDraftPropertyDetails("1")
        await(result) must be(PropertyDetailsCacheNotFoundResponse)
      }

      "retrieve data and return an error response if it fails" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("new Prop Det"))

        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.retrieveDraftPropertyDetails(ArgumentMatchers.eq("1"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(successResponse))))

        val result: Future[PropertyDetailsCacheResponse] = testPropertyDetailsService.retrieveDraftPropertyDetails("1")
        await(result) must be(PropertyDetailsCacheErrorResponse)
      }
    }

    "Submit property Details" must {
      "submit and return the response from the connector" in new Setup {
        val jsonEtmpResponse: String =
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

        val successResponse: JsValue = Json.parse(jsonEtmpResponse)
        when(mockPropertyDetailsConnector.submitDraftPropertyDetails(ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val SubmitReturnsResponseFormId = "submit-returns-response-Id"
        when(mockDataCacheConnector.saveFormData[SubmitReturnsResponse](ArgumentMatchers.eq(SubmitReturnsResponseFormId),
          ArgumentMatchers.eq(successResponse.as[SubmitReturnsResponse]))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(SubmitReturnsResponse.formats)))
          .thenReturn(Future.successful(successResponse.as[SubmitReturnsResponse]))

        val result: Future[HttpResponse] = testPropertyDetailsService.submitDraftPropertyDetails("1")
        val response: HttpResponse = await(result)
        verify(mockDataCacheConnector, times(1)).clearCache()(ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).saveFormData(ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())
      }

    }

    "clear chargeable draft" when {
       "valid id is passed" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("new Prop Det"))
         val successResponse: JsValue = Json.toJson(Seq(propertyDetails))
         when(mockPropertyDetailsConnector.deleteDraftChargeable(ArgumentMatchers.eq("AB12345"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
           .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

         val result: Future[HttpResponse] = testPropertyDetailsService.clearDraftReliefs("AB12345")
         val response: HttpResponse = await(result)
         verify(mockPropertyDetailsConnector, times(1)).deleteDraftChargeable(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
       }
    }

    "validate whether to make the calculateDraftPropertyDetails call to ETMP"  when {
      "for a valid form bundle number but property no period is found" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetailsWithNoValue("1", Some("new Prop Det"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockPropertyDetailsConnector.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[Boolean] = testPropertyDetailsService.validateCalculateDraftPropertyDetails("AB12345")
        val response: Boolean = await(result)
        verify(mockPropertyDetailsConnector, times(1)).retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
  }

}
