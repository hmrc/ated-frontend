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

import java.util.UUID

import connectors.AtedConnector
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, InternalServerException}

import scala.concurrent.Future

class SubscriptionDataAdapterServiceSpec extends PlaySpec with MockitoSugar{

  val mockAtedConnector: AtedConnector = mock[AtedConnector]

  class Setup {
    val testSubscriptionDataAdapterService: SubscriptionDataAdapterService = new SubscriptionDataAdapterService(
      mockAtedConnector
    )
  }

  val successJson: String =
    """
      |{
      |  "safeId": "XA0001234567899",
      |  "organisationName": "BusinessName",
      |  "address": [
      |    {
      |      "addressDetails": {
      |        "addressType": "Permanent Place Of Business",
      |        "addressLine1": "1 addressLine1",
      |        "addressLine2": "addressLine2",
      |        "postalCode": "XX11XX",
      |        "countryCode": "GB"
      |      }
      |    }, {
      |      "name1": "Joseph",
      |      "name2": "Joey",
      |      "addressDetails": {
      |        "addressType": "Correspondence",
      |         "addressLine1": "1 addressLine1",
      |         "addressLine2": "addressLine2",
      |         "postalCode": "XX11XX",
      |        "countryCode": "GB"
      |      }
      |    }
      |  ]
      |}
    """.stripMargin

  val successJson1: String =
    """
      |{
      |  "safeId": "XA0001234567899",
      |  "organisationName": "BusinessName",
      |  "address": [
      |    {
      |      "addressDetails": {
      |        "addressType": "Permanent Place Of Business",
      |        "addressLine1": "1 addressLine1",
      |        "addressLine2": "addressLine2",
      |        "postalCode": "XX11XX",
      |        "countryCode": "GB"
      |      }
      |    }, {
      |      "name1": "Joseph",
      |      "name2": "Joey",
      |      "addressDetails": {
      |        "addressType": "Correspondence",
      |        "addressLine1": "1 addressLine1",
      |        "addressLine2": "addressLine2",
      |        "postalCode": "XX11XX",
      |        "countryCode": "GB"
      |      },
      |		  "contactDetails": {
      |			  "phoneNumber": "7776633528",
      |			  "mobileNumber": "99999999999",
      |			  "emailAddress": "aa@mail.com"
      |		   }
      |    }
      |  ]
      |}
    """.stripMargin

  val successJson2: String =
    """
      |{
      |  "safeId": "XA0001234567899",
      |  "organisationName": "BusinessName",
      |  "address": [
      |    {
      |      "addressDetails": {
      |        "addressType": "Permanent Place Of Business",
      |        "addressLine1": "1 addressLine1",
      |        "addressLine2": "addressLine2",
      |        "postalCode": "XX11XX",
      |        "countryCode": "GB"
      |      }
      |    }, {
      |      "name1": "Joseph",
      |      "name2": "Joey",
      |      "addressDetails": {
      |        "addressType": "Correspondence",
      |        "addressLine1": "1 addressLine1",
      |        "addressLine2": "addressLine2",
      |        "countryCode": "GB"
      |      },
      |		  "contactDetails": {
      |			  "phoneNumber": "7776633528",
      |			  "mobileNumber": "99999999999",
      |			  "emailAddress": "aa@mail.com"
      |		   }
      |    }
      |  ]
      |}
    """.stripMargin

  val successJson3: String =
    """
      |{
      |  "safeId": "XA0001234567899",
      |  "organisationName": "BusinessName",
      |  "address": [
      |    {
      |      "addressDetails": {
      |        "addressType": "Permanent Place Of Business",
      |        "addressLine1": "1 addressLine1",
      |        "addressLine2": "addressLine2",
      |        "postalCode": "XX11XX",
      |        "countryCode": "GB"
      |      }
      |    }, {
      |      "name1": "Joseph",
      |      "name2": "Joey",
      |      "addressDetails": {
      |        "addressType": "Correspondence",
      |        "addressLine1": "1 addressLine1",
      |        "addressLine2": "addressLine2",
      |        "postalCode": "",
      |        "countryCode": "GB"
      |      },
      |		  "contactDetails": {
      |			  "phoneNumber": "7776633528",
      |			  "mobileNumber": "99999999999",
      |			  "emailAddress": "aa@mail.com"
      |		   }
      |    }
      |  ]
      |}
    """.stripMargin

  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]

  "SubscriptionDataAdapterService" must {
    "return data if we have some" in new Setup {

      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val successResponse: JsValue = Json.parse(successJson)
      when(mockAtedConnector.retrieveSubscriptionData()(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val result: Future[Option[SubscriptionData]] = testSubscriptionDataAdapterService.retrieveSubscriptionData
      val data: Option[SubscriptionData] = await(result)
      data.isDefined must be(true)
      data.get.safeId must be("XA0001234567899")
      data.get.address.size must be(2)
    }

    "return no data if we have none" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))

      when(mockAtedConnector.retrieveSubscriptionData()(any(), any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))

      val result: Future[Option[SubscriptionData]] = testSubscriptionDataAdapterService.retrieveSubscriptionData
      val data: Option[SubscriptionData] = await(result)
      data.isDefined must be(false)
    }

    "throws an exception for a bad request" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))

      when(mockAtedConnector.retrieveSubscriptionData()(any(), any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

      val result: Future[Option[SubscriptionData]] = testSubscriptionDataAdapterService.retrieveSubscriptionData
      val thrown: BadRequestException = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Failed to retrieve Subscription Data")
    }

    "throws an exception for a internal server error" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))

      when(mockAtedConnector.retrieveSubscriptionData()(any(), any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, None)))

      val result: Future[Option[SubscriptionData]] = testSubscriptionDataAdapterService.retrieveSubscriptionData
      val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("Failed to retrieve Subscription Data")
    }

    "update subscription data" must {
      "save address details" must {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val updatedAddress = Address(Some("name1"), Some("name2"), addressDetails = AddressDetails("Correspondence", "line1", "line2", None, None, None, "GB"))
        val updateSubscriptionData = UpdateSubscriptionDataRequest(emailConsent = true, changeIndicators = ChangeIndicators(), Seq(updatedAddress))

        "save the address details" in new Setup {

          when(mockAtedConnector.updateSubscriptionData(any())(any(), any()))
            .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

          val updateContactDetails: ContactDetails = ContactDetails()
          val result: Future[Option[UpdateSubscriptionDataRequest]] = testSubscriptionDataAdapterService.updateSubscriptionData(updateSubscriptionData)
          val addressDetails: Option[UpdateSubscriptionDataRequest] = await(result)
          addressDetails.isDefined must be(false)
        }

        "save the address details successful" in new Setup {
          when(mockAtedConnector.updateSubscriptionData(any())(any(), any()))
            .thenReturn(Future.successful(HttpResponse(OK, None)))

          val result: Future[Option[UpdateSubscriptionDataRequest]] = testSubscriptionDataAdapterService.updateSubscriptionData(updateSubscriptionData)
          val addressDetails: Option[UpdateSubscriptionDataRequest] = await(result)
        }
      }
    }

    "createEditEmailWithConsentRequest" must {
      "return None, if no correspondence address is found" in new Setup {
        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val updatedDetails: EditContactDetailsEmail = EditContactDetailsEmail(emailAddress = "aa@mail.com", emailConsent = true)
        val response: Option[UpdateSubscriptionDataRequest] = testSubscriptionDataAdapterService
          .createEditEmailWithConsentRequest(successResponse, updatedDetails)
        response.isDefined must be(false)
      }

      "return Updated Indicators, if correspondence address is found" in new Setup {
        val successResponse: SubscriptionData = Json.parse(successJson1).as[SubscriptionData]
        val updatedDetails: EditContactDetailsEmail = EditContactDetailsEmail(emailAddress = "aa@mail.com", emailConsent = true)
        val response: Option[UpdateSubscriptionDataRequest] = testSubscriptionDataAdapterService
          .createEditEmailWithConsentRequest(successResponse, updatedDetails)
        response.isDefined must be(true)
        response.get.changeIndicators.contactDetailsChanged must be(true)
        response.get.changeIndicators.nameChanged must be(false)
        response.get.changeIndicators.correspondenceChanged must be(false)
        response.get.changeIndicators.permanentPlaceOfBusinessChanged must be(false)

      }
    }
    "createEditContactDetailsRequest" must {
      "return None, if no correspondence address is found" in new Setup {
        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val updatedDetails: EditContactDetails = EditContactDetails("name1", "name2", phoneNumber = "123456789")
        val response: Option[UpdateSubscriptionDataRequest] = testSubscriptionDataAdapterService
          .createEditContactDetailsRequest(successResponse, updatedDetails)
        response.isDefined must be(false)
      }

      "return Some, if correspondence address is found" in new Setup {
        val successResponse: SubscriptionData = Json.parse(successJson1).as[SubscriptionData]
        val updatedDetails: EditContactDetails = EditContactDetails("name1", "name2", phoneNumber = "123456789")
        val response: Option[UpdateSubscriptionDataRequest] = testSubscriptionDataAdapterService
          .createEditContactDetailsRequest(successResponse, updatedDetails)
        response.isDefined must be(true)
        response.get.changeIndicators.contactDetailsChanged must be(true)
        response.get.changeIndicators.nameChanged must be(true)
        response.get.changeIndicators.correspondenceChanged must be(false)
        response.get.changeIndicators.permanentPlaceOfBusinessChanged must be(false)
      }

      "return Some, if correspondence address is found but BLANK postCode" in new Setup {
        val successResponse: SubscriptionData = Json.parse(successJson2).as[SubscriptionData]
        val updatedDetails: EditContactDetails = EditContactDetails("name1", "name2", phoneNumber = "123456789")
        val response: Option[UpdateSubscriptionDataRequest] = testSubscriptionDataAdapterService
          .createEditContactDetailsRequest(successResponse, updatedDetails)
        response.isDefined must be(true)
        response.get.changeIndicators.contactDetailsChanged must be(true)
        response.get.changeIndicators.nameChanged must be(true)
        response.get.changeIndicators.correspondenceChanged must be(false)
        response.get.changeIndicators.permanentPlaceOfBusinessChanged must be(false)
      }

      "return Some, if correspondence address is found but no postCode" in new Setup {
        val successResponse: SubscriptionData = Json.parse(successJson3).as[SubscriptionData]
        val updatedDetails: EditContactDetails = EditContactDetails("name1", "name2", phoneNumber = "123456789")
        val response: Option[UpdateSubscriptionDataRequest] = testSubscriptionDataAdapterService
          .createEditContactDetailsRequest(successResponse, updatedDetails)
        response.isDefined must be(true)
        response.get.changeIndicators.contactDetailsChanged must be(true)
        response.get.changeIndicators.nameChanged must be(true)
        response.get.changeIndicators.correspondenceChanged must be(false)
        response.get.changeIndicators.permanentPlaceOfBusinessChanged must be(false)
      }
    }

    "createUpdateCorrespondenceAddressRequest" must {
      "return None if no correspondence address is found" in new Setup {
        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val updatedDetails: AddressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val response: Option[UpdateSubscriptionDataRequest] = testSubscriptionDataAdapterService
          .createUpdateCorrespondenceAddressRequest(successResponse, updatedDetails)
        response.isDefined must be(false)
      }

      "return Some if correspondence address is found" in new Setup {
        val successResponse: SubscriptionData = Json.parse(successJson).as[SubscriptionData]
        val updatedDetails: AddressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val response: Option[UpdateSubscriptionDataRequest] = testSubscriptionDataAdapterService
          .createUpdateCorrespondenceAddressRequest(successResponse, updatedDetails)
        response.isDefined must be(true)
        response.get.changeIndicators.contactDetailsChanged must be(false)
        response.get.changeIndicators.correspondenceChanged must be(true)
        response.get.changeIndicators.nameChanged must be(false)
        response.get.changeIndicators.permanentPlaceOfBusinessChanged must be(false)
      }
    }

    "getOrganisationName" must {
      "return organisationName from registration data" in new Setup {
        val etmpRegistrationDetails: EtmpRegistrationDetails = EtmpRegistrationDetails(sapNumber = "12345678", safeId = "X1234567",
          organisation = Some(Organisation("BusinessName")), isAnIndividual = false,
          addressDetails = RegisteredAddressDetails("line 1", "line 2", Some("line 3"), countryCode = "UK"),
          contactDetails = ContactDetails(),
          individual = None,
          isEditable = false,
          isAnAgent = false,
          agentReferenceNumber = None,
          nonUKIdentification = None)
        val response: Option[String] = testSubscriptionDataAdapterService.getOrganisationName(Some(etmpRegistrationDetails))
        response must be(Some("BusinessName"))
      }
    }

    "getSafeId" must {
      "return safe id from subscription data" in new Setup {
        val successResponse: SubscriptionData = Json.parse(successJson).as[SubscriptionData]
        val response: Option[String] = testSubscriptionDataAdapterService.getSafeId(Some(successResponse))
        response must be(Some("XA0001234567899"))
      }
    }
  }
}
