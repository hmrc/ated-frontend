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

import java.util.UUID

import connectors.AtedConnector
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, InternalServerException}

import scala.concurrent.Future

class SubscriptionDataAdapterSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockConnector: AtedConnector = mock[AtedConnector]

  override def beforeEach: Unit = {
    reset(mockConnector)
  }

  object TestSubscriptionDataService extends SubscriptionDataAdapterService {
    override val atedConnector: AtedConnector = mockConnector
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

  val successJson1 =
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

  val successJson2 =
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

  val successJson3 =
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

  implicit lazy val authContext = mock[StandardAuthRetrievals]

  "SubscriptionDataAdapterService" must {
    val emptySubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))

    "use the correct connectors" in {
      SubscriptionDataAdapterService.atedConnector must be(AtedConnector)
    }

    "return data if we have some" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val successResponse = Json.parse(successJson)
      when(mockConnector.retrieveSubscriptionData()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val result = TestSubscriptionDataService.retrieveSubscriptionData
      val data = await(result)
      data.isDefined must be(true)
      data.get.safeId must be("XA0001234567899")
      data.get.address.size must be(2)
    }

    "return no data if we have none" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))

      when(mockConnector.retrieveSubscriptionData()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))

      val result = TestSubscriptionDataService.retrieveSubscriptionData
      val data = await(result)
      data.isDefined must be(false)
    }

    "throws an exception for a bad request" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))

      when(mockConnector.retrieveSubscriptionData()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

      val result = TestSubscriptionDataService.retrieveSubscriptionData
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Failed to retrieve Subscription Data")
    }

    "throws an exception for a internal server error" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))

      when(mockConnector.retrieveSubscriptionData()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NO_CONTENT, None)))

      val result = TestSubscriptionDataService.retrieveSubscriptionData
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("Failed to retrieve Subscription Data")
    }

    "update subscription data" must {
      "save address details" must {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val updatedAddress = Address(Some("name1"), Some("name2"), addressDetails = AddressDetails("Correspondence", "line1", "line2", None, None, None, "GB"))
        val updateSubscriptionData = UpdateSubscriptionDataRequest(true, changeIndicators = ChangeIndicators(), Seq(updatedAddress))

        "save the address details" in {

          when(mockConnector.updateSubscriptionData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

          val updateContactDetails = ContactDetails()
          val result = TestSubscriptionDataService.updateSubscriptionData(updateSubscriptionData)
          val addressDetails = await(result)
          addressDetails.isDefined must be(false)
        }

        "save the address details successful" in {
          when(mockConnector.updateSubscriptionData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

          val result = TestSubscriptionDataService.updateSubscriptionData(updateSubscriptionData)
          val addressDetails = await(result)
        }
      }
    }

    "createEditEmailWithConsentRequest" must {
      "return None, if no correspondence address is found" in {
        val successResponse = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val updatedDetails = EditContactDetailsEmail(emailAddress = "aa@mail.com", emailConsent = true)
        val response = TestSubscriptionDataService.createEditEmailWithConsentRequest(successResponse, updatedDetails)
        response.isDefined must be(false)
      }

      "return Updated Indicators, if correspondence address is found" in {
        val successResponse = Json.parse(successJson1).as[SubscriptionData]
        val updatedDetails = EditContactDetailsEmail(emailAddress = "aa@mail.com", emailConsent = true)
        val response = TestSubscriptionDataService.createEditEmailWithConsentRequest(successResponse, updatedDetails)
        response.isDefined must be(true)
        response.get.changeIndicators.contactDetailsChanged must be(true)
        response.get.changeIndicators.nameChanged must be(false)
        response.get.changeIndicators.correspondenceChanged must be(false)
        response.get.changeIndicators.permanentPlaceOfBusinessChanged must be(false)

      }
    }
    "createEditContactDetailsRequest" must {
      "return None, if no correspondence address is found" in {
        val successResponse = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val updatedDetails = EditContactDetails("name1", "name2", phoneNumber = "123456789")
        val response = TestSubscriptionDataService.createEditContactDetailsRequest(successResponse, updatedDetails)
        response.isDefined must be(false)
      }

      "return Some, if correspondence address is found" in {
        val successResponse = Json.parse(successJson1).as[SubscriptionData]
        val updatedDetails = EditContactDetails("name1", "name2", phoneNumber = "123456789")
        val response = TestSubscriptionDataService.createEditContactDetailsRequest(successResponse, updatedDetails)
        response.isDefined must be(true)
        response.get.changeIndicators.contactDetailsChanged must be(true)
        response.get.changeIndicators.nameChanged must be(true)
        response.get.changeIndicators.correspondenceChanged must be(false)
        response.get.changeIndicators.permanentPlaceOfBusinessChanged must be(false)
      }

      "return Some, if correspondence address is found but BLANK postCode" in {
        val successResponse = Json.parse(successJson2).as[SubscriptionData]
        val updatedDetails = EditContactDetails("name1", "name2", phoneNumber = "123456789")
        val response = TestSubscriptionDataService.createEditContactDetailsRequest(successResponse, updatedDetails)
        response.isDefined must be(true)
        response.get.changeIndicators.contactDetailsChanged must be(true)
        response.get.changeIndicators.nameChanged must be(true)
        response.get.changeIndicators.correspondenceChanged must be(false)
        response.get.changeIndicators.permanentPlaceOfBusinessChanged must be(false)
      }

      "return Some, if correspondence address is found but no postCode" in {
        val successResponse = Json.parse(successJson3).as[SubscriptionData]
        val updatedDetails = EditContactDetails("name1", "name2", phoneNumber = "123456789")
        val response = TestSubscriptionDataService.createEditContactDetailsRequest(successResponse, updatedDetails)
        response.isDefined must be(true)
        response.get.changeIndicators.contactDetailsChanged must be(true)
        response.get.changeIndicators.nameChanged must be(true)
        response.get.changeIndicators.correspondenceChanged must be(false)
        response.get.changeIndicators.permanentPlaceOfBusinessChanged must be(false)
      }
    }

    "createUpdateCorrespondenceAddressRequest" must {
      "return None if no correspondence address is found" in {
        val successResponse = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val updatedDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val response = TestSubscriptionDataService.createUpdateCorrespondenceAddressRequest(successResponse, updatedDetails)
        response.isDefined must be(false)
      }

      "return Some if correspondence address is found" in {
        val successResponse = Json.parse(successJson).as[SubscriptionData]
        val updatedDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val response = TestSubscriptionDataService.createUpdateCorrespondenceAddressRequest(successResponse, updatedDetails)
        response.isDefined must be(true)
        response.get.changeIndicators.contactDetailsChanged must be(false)
        response.get.changeIndicators.correspondenceChanged must be(true)
        response.get.changeIndicators.nameChanged must be(false)
        response.get.changeIndicators.permanentPlaceOfBusinessChanged must be(false)
      }
    }

    "getOrganisationName" must {
      "return organisationName from registration data" in {
        val etmpRegistrationDetails = EtmpRegistrationDetails(sapNumber = "12345678", safeId = "X1234567",
          organisation = Some(Organisation("BusinessName")), isAnIndividual = false,
          addressDetails = RegisteredAddressDetails("line 1", "line 2", Some("line 3"), countryCode = "UK"),
          contactDetails = ContactDetails(),
          individual = None,
          isEditable = false,
          isAnAgent = false,
          agentReferenceNumber = None,
          nonUKIdentification = None)
        val response = TestSubscriptionDataService.getOrganisationName(Some(etmpRegistrationDetails))
        response must be(Some("BusinessName"))
      }
    }

    "getSafeId" must {
      "return safe id from subscription data" in {
        val successResponse = Json.parse(successJson).as[SubscriptionData]
        val response = TestSubscriptionDataService.getSafeId(Some(successResponse))
        response must be(Some("XA0001234567899"))
      }
    }
  }
}
