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

package connectors

import java.util.UUID

import builders.AuthBuilder._
import builders.PropertyDetailsBuilder
import models._
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost}

import scala.concurrent.Future
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.SessionId

class PropertyDetailsConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  trait MockedVerbs extends CoreGet with CorePost with CoreDelete
  val mockWSHttp: CoreGet with CorePost with CoreDelete = mock[MockedVerbs]

  object TestAtedConnector extends PropertyDetailsConnector {
    override val serviceURL = baseUrl("ated")
    override val http: CoreGet with CorePost with CoreDelete = mockWSHttp
  }

  override def beforeEach = {
    reset(mockWSHttp)
  }

  "PropertyDetailsConnector" must {

    "create draft property details" must {
      "for successful save, return new the id for the property details" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("testPostCode"))

        val successResponse = Json.toJson(propertyDetails)

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result = TestAtedConnector.createDraftPropertyDetails(2015, propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("testPostCode"))

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
        val result = TestAtedConnector.saveDraftPropertyDetailsAddressRef("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details address ref" must {
      "for successful save, return PropertyDetails address ref for a user" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("testPostCode"))

        val successResponse = Json.toJson(propertyDetails)

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result = TestAtedConnector.saveDraftPropertyDetailsAddressRef("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("testPostCode"))

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
        val result = TestAtedConnector.saveDraftPropertyDetailsAddressRef("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details Has Value Changed" must {
      "for successful save, return PropertyDetails Has Value Changed for a user" in {
        val propertyDetails = PropertyDetailsTitle("")

        val successResponse = Json.toJson(propertyDetails)

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftHasValueChanged("1", true)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        val propertyDetails = PropertyDetailsTitle("")

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftHasValueChanged("1", true)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details title" must {
      "for successful save, return PropertyDetails title for a user" in {
        val propertyDetails = PropertyDetailsTitle("")

        val successResponse = Json.toJson(propertyDetails)

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftPropertyDetailsTitle("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        val propertyDetails = PropertyDetailsTitle("")

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftPropertyDetailsTitle("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details Acquisition" must {
      "for successful save, return PropertyDetails Acquisition for a user" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        val successResponse = Json.toJson(true)

        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftPropertyDetailsAcquisition("1", true)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftPropertyDetailsAcquisition("1", true)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details Revalued" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = new PropertyDetailsRevalued()

      "for successful save, return PropertyDetails Revalued for a user" in {
        val successResponse = Json.toJson(propertyDetails)

        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftPropertyDetailsRevalued("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftPropertyDetailsRevalued("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details OwnedBefore" must {
      "for successful save, return PropertyDetails title for a user" in {
        val propertyDetails = new PropertyDetailsOwnedBefore()

        val successResponse = Json.toJson(propertyDetails)

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftPropertyDetailsOwnedBefore("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        val propertyDetails = new PropertyDetailsOwnedBefore()

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftPropertyDetailsOwnedBefore("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details ProfessionallyValued" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = new PropertyDetailsProfessionallyValued()

      "for successful save, return PropertyDetails title for a user" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftPropertyDetailsProfessionallyValued("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftPropertyDetailsProfessionallyValued("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details NewBuild" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = new PropertyDetailsNewBuild()

      "for successful save, return PropertyDetails title for a user" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftPropertyDetailsNewBuild("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftPropertyDetailsNewBuild("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details IsFullTaxPeriod" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = new IsFullTaxPeriod(false, None)

      "for successful save, return PropertyDetails title for a user" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftIsFullTaxPeriod("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftIsFullTaxPeriod("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details IsInRelief" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = new PropertyDetailsInRelief()

      "for successful save, return PropertyDetails title for a user" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftPropertyDetailsInRelief("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftPropertyDetailsInRelief("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details TaxAvoidance" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = new PropertyDetailsTaxAvoidance()

      "for successful save, return PropertyDetails title for a user" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftPropertyDetailsTaxAvoidance("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftPropertyDetailsTaxAvoidance("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "save property details DatesLiable" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = new PropertyDetailsDatesLiable(new LocalDate("1970-01-01"), new LocalDate("1970-01-01"))

      "for successful save, return PropertyDetails title for a user" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftPropertyDetailsDatesLiable("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftPropertyDetailsDatesLiable("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "add DatesLiable" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = PropertyDetailsDatesLiable(
        new LocalDate("2999-02-03"),new LocalDate("2999-03-04")
      )

      "for successful add, return PropertyDetails title for a user" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.addDraftPropertyDetailsDatesLiable("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful add, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.addDraftPropertyDetailsDatesLiable("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "delete Period" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = PropertyDetailsDatesLiable(
        new LocalDate("2999-02-03"),new LocalDate("2999-03-04")
      )

      "for successful delete, return PropertyDetails title for a user" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.deleteDraftPropertyDetailsPeriod("1", propertyDetails.startDate)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful delete, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.deleteDraftPropertyDetailsPeriod("1", propertyDetails.startDate)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "add DatesInRelief" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = PropertyDetailsDatesInRelief(
        new LocalDate("2999-02-03"),new LocalDate("2999-03-04")
      )

      "for successful add, return PropertyDetails title for a user" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.addDraftPropertyDetailsDatesInRelief("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful add, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.addDraftPropertyDetailsDatesInRelief("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }


    "save property details SupportingInfo" must {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
      val propertyDetails = new PropertyDetailsSupportingInfo("")

      "for successful save, return PropertyDetails title for a user" in {
        val successResponse = Json.toJson(propertyDetails)
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.saveDraftPropertyDetailsSupportingInfo("1", propertyDetails)
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful save, return an empty object" in {
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestAtedConnector.saveDraftPropertyDetailsSupportingInfo("1", propertyDetails)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "Calculate property details" must {
      "return PropertyDetails for a user when we have some" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode"))
        val successResponse = Json.toJson(propertyDetails)
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result = TestAtedConnector.calculateDraftPropertyDetails("1")
        val response = await(result)
        response.status must be(OK)
      }
    }

    "Calculate changed Liability" must {
      "return PropertyDetails for a user when we have some" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode"))
        val successResponse = Json.toJson(propertyDetails)
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result = TestAtedConnector.calculateDraftChangeLiability("1")
        val response = await(result)
        response.status must be(OK)
      }
    }

    "Retrieve property details" must {
      "return PropertyDetails for a user when we have some" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode"))
        val successResponse = Json.toJson(propertyDetails)
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result = TestAtedConnector.retrieveDraftPropertyDetails("1")
        val response = await(result)
        response.status must be(OK)
      }
    }

    "submit property details" must {
      "for successful submit, return submit response" in {
        val successResponse = Json.toJson(PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode")))
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result = TestAtedConnector.submitDraftPropertyDetails("1")
        val response = await(result)
        response.status must be(OK)
      }

      "for an unsuccessful submit, return an empty object" in {
        val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode"))
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))


        val result = TestAtedConnector.submitDraftPropertyDetails("1")
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

    "delete draft chargeable return" must {
      "for successful submit, return submit response" in {
        val successResponse = Json.toJson(Seq(PropertyDetailsBuilder.getPropertyDetails("1", Some("testPostCode"))))
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.DELETE[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        val result = TestAtedConnector.deleteDraftChargeable("ABC12345")
        val response = await(result)
        response.status must be(OK)
      }

      "for an inavlid id, return an empty object" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
        when(mockWSHttp.DELETE[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
        val result = TestAtedConnector.deleteDraftChargeable("XYZ123456")
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

  }
}
