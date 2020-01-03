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

package connectors

import java.util.UUID

import builders.PropertyDetailsBuilder
import config.ApplicationConfig
import models._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class PropertyDetailsConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]

  lazy val periodKey = 2015

  class Setup {
    val testPropertyDetailsConnector : PropertyDetailsConnector = new PropertyDetailsConnector(mockAppConfig, mockHttp)
  }

  override def beforeEach: Unit = {
  }

  "PropertyDetailsConnector" must {

    "create draft property details" must {
      "for successful save, return new the id for the property details" in new Setup {
        private val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("testPostCode"))

        val successResponse: JsValue = Json.toJson(propertyDetails)

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result: Future[HttpResponse] = testPropertyDetailsConnector.createDraftPropertyDetails(periodKey, propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("testPostCode"))

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsAddressRef("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details address ref" must {
      "for successful save, return PropertyDetails address ref for a user" in new Setup {
        val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("testPostCode"))

        val successResponse: JsValue = Json.toJson(propertyDetails)

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsAddressRef("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("testPostCode"))

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsAddressRef("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details Has Value Changed" must {
      "for successful save, return PropertyDetails Has Value Changed for a user" in new Setup {
        val propertyDetails = PropertyDetailsTitle("")

        val successResponse: JsValue = Json.toJson(propertyDetails)

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftHasValueChanged("1", propertyDetails = true)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftHasValueChanged("1", propertyDetails = true)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details title" must {
      "for successful save, return PropertyDetails title for a user" in new Setup {
        val propertyDetails = PropertyDetailsTitle("")

        val successResponse: JsValue = Json.toJson(propertyDetails)

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsTitle("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        val propertyDetails = PropertyDetailsTitle("")

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsTitle("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details Acquisition" must {
      "for successful save, return PropertyDetails Acquisition for a user" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse: JsValue = Json.toJson(true)

        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsAcquisition("1", overLimit = true)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsAcquisition("1", overLimit = true)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details Revalued" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = new PropertyDetailsRevalued()

      "for successful save, return PropertyDetails Revalued for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)

        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsRevalued("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsRevalued("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details OwnedBefore" must {
      "for successful save, return PropertyDetails title for a user" in new Setup {
        val propertyDetails = new PropertyDetailsOwnedBefore()

        val successResponse: JsValue = Json.toJson(propertyDetails)

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsOwnedBefore("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        val propertyDetails = new PropertyDetailsOwnedBefore()

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsOwnedBefore("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details ProfessionallyValued" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = new PropertyDetailsProfessionallyValued()

      "for successful save, return PropertyDetails title for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsProfessionallyValued("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsProfessionallyValued("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details NewBuild" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = new PropertyDetailsNewBuild()

      "for successful save, return PropertyDetails title for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsNewBuild("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsNewBuild("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details IsFullTaxPeriod" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = new IsFullTaxPeriod(false, None)

      "for successful save, return PropertyDetails title for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftIsFullTaxPeriod("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftIsFullTaxPeriod("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details IsInRelief" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = new PropertyDetailsInRelief()

      "for successful save, return PropertyDetails title for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsInRelief("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsInRelief("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details TaxAvoidance" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = new PropertyDetailsTaxAvoidance()

      "for successful save, return PropertyDetails title for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsTaxAvoidance("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsTaxAvoidance("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details DatesLiable" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = new PropertyDetailsDatesLiable(new LocalDate("1970-01-01"), new LocalDate("1970-01-01"))

      "for successful save, return PropertyDetails title for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsDatesLiable("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsDatesLiable("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "add DatesLiable" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = PropertyDetailsDatesLiable(
        new LocalDate("2999-02-03"),new LocalDate("2999-03-04")
      )

      "for successful add, return PropertyDetails title for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.addDraftPropertyDetailsDatesLiable("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful add, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.addDraftPropertyDetailsDatesLiable("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "delete Period" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = PropertyDetailsDatesLiable(
        new LocalDate("2999-02-03"),new LocalDate("2999-03-04")
      )

      "for successful delete, return PropertyDetails title for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.deleteDraftPropertyDetailsPeriod("1", propertyDetails.startDate)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful delete, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.deleteDraftPropertyDetailsPeriod("1", propertyDetails.startDate)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "add DatesInRelief" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = PropertyDetailsDatesInRelief(
        new LocalDate("2999-02-03"),new LocalDate("2999-03-04")
      )

      "for successful add, return PropertyDetails title for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.addDraftPropertyDetailsDatesInRelief("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful add, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.addDraftPropertyDetailsDatesInRelief("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }


    "save property details SupportingInfo" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val propertyDetails = new PropertyDetailsSupportingInfo("")

      "for successful save, return PropertyDetails title for a user" in new Setup {
        val successResponse: JsValue = Json.toJson(propertyDetails)
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsSupportingInfo("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in new Setup {
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result: Future[HttpResponse] = testPropertyDetailsConnector.saveDraftPropertyDetailsSupportingInfo("1", propertyDetails)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "Calculate property details" must {
      "return PropertyDetails for a user when we have some" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse]
          (ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result: Future[HttpResponse] = testPropertyDetailsConnector.calculateDraftPropertyDetails("1")
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "Calculate changed Liability" must {
      "return PropertyDetails for a user when we have some" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse]
          (ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result: Future[HttpResponse] = testPropertyDetailsConnector.calculateDraftChangeLiability("1")
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "Retrieve property details" must {
      "return PropertyDetails for a user when we have some" in new Setup {
        val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode"))
        val successResponse: JsValue = Json.toJson(propertyDetails)
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse]
          (ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result: Future[HttpResponse] = testPropertyDetailsConnector.retrieveDraftPropertyDetails("1")
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "submit property details" must {
      "for successful submit, return submit response" in new Setup {
        val successResponse: JsValue = Json.toJson(PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode")))
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result: Future[HttpResponse] = testPropertyDetailsConnector.submitDraftPropertyDetails("1")
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful submit, return an empty object" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))


        val result: Future[HttpResponse] = testPropertyDetailsConnector.submitDraftPropertyDetails("1")
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "delete draft chargeable return" must {
      "for successful submit, return submit response" in new Setup {
        val successResponse: JsValue = Json.toJson(Seq(PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode"))))
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.DELETE[HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        val result: Future[HttpResponse] = testPropertyDetailsConnector.deleteDraftChargeable("ABC12345")
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an inavlid id, return an empty object" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.DELETE[HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
        val result: Future[HttpResponse] = testPropertyDetailsConnector.deleteDraftChargeable("XYZ123456")
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

  }
}
