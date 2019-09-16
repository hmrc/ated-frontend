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

import builders.RegistrationBuilder
import connectors.{AgentClientMandateFrontendConnector, AtedConnector, DataCacheConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, InternalServerException}

import scala.concurrent.Future

class DetailsServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val mockMandateFrontendConnector: AgentClientMandateFrontendConnector = mock[AgentClientMandateFrontendConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  object TestDetailsService extends DetailsService {
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
    override val atedConnector: AtedConnector = mockAtedConnector
    override val mandateFrontendConnector: AgentClientMandateFrontendConnector = mockMandateFrontendConnector
  }

  override def beforeEach: Unit = {
    reset(mockAtedConnector)
    reset(mockMandateFrontendConnector)
  }

  val successResponseInd: JsValue = Json.parse(
    """
      |{
      |  "sapNumber":"1234567890", "safeId": "EX0012345678909",
      |  "agentReferenceNumber": "AARN1234567",
      |  "isAnIndividual": true,
      |  "isAnAgent": true,
      |  "isEditable": true,
      |  "individual": {
      |    "firstName": "TestFirstName",
      |    "lastName": "TestLastName",
      |    "dateOfBirth": "1962-10-12"
      |  },
      |  "addressDetails": {
      |    "addressLine1": "line 1",
      |    "addressLine2": "line 2",
      |    "addressLine3": "line 3",
      |    "addressLine4": "line 4",
      |    "postalCode": "XX1 1XX",
      |    "countryCode": "GB"
      |  },
      |  "contactDetails" : {}
      |}
    """.stripMargin
  )

  val clientMandateDetails: JsValue = Json.parse(
    """
      |{
      | "agentName": "agent name",
      | "changeAgentLink": "changeUrl",
      | "email": "email@address",
      | "changeEmailLink": "changeEmailUrl"
      | }
    """.stripMargin
  )

  val failureResponse: JsValue = Json.parse( """{"reason":"Agent not found!"}""")
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  val identifier = "JARN1234567"
  val identifierType = "arn"

  "getDetails" must {
    "for OK response status, return body as Some(EtmpRegistrationDetails)" in {
      when(mockAtedConnector
        .getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponseInd))))
      val result = TestDetailsService.getDetails(identifier, identifierType)
      await(result) must be(Some(successResponseInd.as[EtmpRegistrationDetails]))
    }
    "for NOT_FOUND response status, return body as None" in {
      when(mockAtedConnector.getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(failureResponse))))
      val result = TestDetailsService.getDetails(identifier, identifierType)
      await(result) must be(None)
    }
    "for BAD_REQUEST response status, throw bad request exception" in {
      when(mockAtedConnector
        .getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
      val result = TestDetailsService.getDetails(identifier, identifierType)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.message must include("Bad Data")
    }
    "getAgentDetails throws InternalServerException exception for call to ETMP, when BadRequest response is received" in {
      when(mockAtedConnector
        .getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
      val result = TestDetailsService.getDetails(identifier, identifierType)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.message must include("Internal server error")
    }
  }

  "getBusinessPartnerDetails" must {
    "return details for organisation" in {
      val indResponse = RegistrationBuilder.getEtmpRegistrationForIndividual("TestFirstName", "TestLastName")
      val successResponseInd = Json.toJson(indResponse)
      when(mockAtedConnector
        .getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponseInd))))
      val result = TestDetailsService.getRegisteredDetailsFromSafeId("1234567890")
      val bp = await(result)
      bp.isDefined must be(true)
      bp.get.name must be(Some("TestFirstName TestLastName"))
      bp.get.isEditable must be(indResponse.isEditable)
      bp.get.addressDetails.addressLine1 must be(indResponse.addressDetails.addressLine1)
    }

    "return details for individual" in {
      val orgResponse = RegistrationBuilder.getEtmpRegistrationForOrganisation("Agents Limited")
      val successResponseOrg = Json.toJson(orgResponse)
      when(mockAtedConnector
        .getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponseOrg))))
      val result = TestDetailsService.getRegisteredDetailsFromSafeId("1234567890")
      val bp = await(result)
      bp.isDefined must be(true)
      bp.get.name must be(Some("Agents Limited"))
      bp.get.isEditable must be(orgResponse.isEditable)
      bp.get.addressDetails.addressLine1 must be(orgResponse.addressDetails.addressLine1)
    }

    "return details for organisation when not found" in {
      when(mockAtedConnector.getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(failureResponse))))
      val result = TestDetailsService.getRegisteredDetailsFromSafeId("1234567890")
      val bp = await(result)
      bp.isDefined must be(false)
    }
  }

  "update registration data" must {
    "save registration details" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val registeredDetails = RegisteredDetails(isEditable = true, "testName", RegisteredAddressDetails(addressLine1 = "newLine1",
        addressLine2 = "newLine2", countryCode = "GB"))
      val oldOrgDetails = RegistrationBuilder.getEtmpRegistrationForOrganisation("oldName")

      "save the registration details for an organistaion" in {

        when(mockAtedConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestDetailsService.updateOrganisationRegisteredDetails(oldOrgDetails, registeredDetails)
        val updatedDetails = await(result)
        updatedDetails.isDefined must be(false)
      }

      "save the registration details successful for an organistion" in {
        when(mockAtedConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, None)))

        val result = TestDetailsService.updateOrganisationRegisteredDetails(oldOrgDetails, registeredDetails)
        val updatedDetails = await(result)
        updatedDetails.isDefined must be(true)

      }

      "fail to save the registration details for an organisation without isAGroup defined" in {
        val oldOrgDetailsNoGroup = oldOrgDetails.copy(organisation = oldOrgDetails.organisation.map(_.copy(isAGroup = None)))

        when(mockAtedConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, None)))

        val result = TestDetailsService.updateOrganisationRegisteredDetails(oldOrgDetailsNoGroup, registeredDetails)
        val updatedDetails = await(result)
        updatedDetails.isDefined must be(false)
      }

      "fail to save the registration details for an individual" in {
        val oldIndDetails = RegistrationBuilder.getEtmpRegistrationForIndividual("TestFirstName", "TestLastName")

        when(mockAtedConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, None)))

        val result = TestDetailsService.updateOrganisationRegisteredDetails(oldIndDetails, registeredDetails)
        val updatedDetails = await(result)
        updatedDetails.isDefined must be(false)

      }
    }
  }

  "update overseas company registration data" must {
    "save details" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val overseasCompanyRegistration = Identification("AAAAAAAAA", "Some Place", "FR")
      val oldOrgDetails = RegistrationBuilder.getEtmpRegistrationForOrganisation("oldName")

      "save the details for an organistaion" in {

        when(mockAtedConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestDetailsService.updateOverseasCompanyRegistration(oldOrgDetails, Some(overseasCompanyRegistration))
        val updatedDetails = await(result)
        updatedDetails.isDefined must be(false)
      }

      "save the details successfully for an organistion" in {
        when(mockAtedConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, None)))

        val result = TestDetailsService.updateOverseasCompanyRegistration(oldOrgDetails, Some(overseasCompanyRegistration))
        val updatedDetails = await(result)
        updatedDetails.isDefined must be(true)

      }

      "fail to save the registration details for an organisation without isAGroup defined" in {
        val oldOrgDetailsNoGroup = oldOrgDetails.copy(organisation = oldOrgDetails.organisation.map(_.copy(isAGroup = None)))

        when(mockAtedConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, None)))

        val result = TestDetailsService.updateOverseasCompanyRegistration(oldOrgDetailsNoGroup, Some(overseasCompanyRegistration))
        val updatedDetails = await(result)
        updatedDetails.isDefined must be(false)
      }

      "fail to save the registration details for an individual" in {
        val oldIndDetails = RegistrationBuilder.getEtmpRegistrationForIndividual("TestFirstName", "TestLastName")

        when(mockAtedConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, None)))

        val result = TestDetailsService.updateOverseasCompanyRegistration(oldIndDetails, Some(overseasCompanyRegistration))
        val updatedDetails = await(result)
        updatedDetails.isDefined must be(false)

      }
    }
  }

  "getClientMandateDetails" must {
    "return None when can't find mandate details" in {
      implicit val request = FakeRequest()
      when(mockMandateFrontendConnector.getClientDetails(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))
      val result = TestDetailsService.getClientMandateDetails("clientId", "ated")
      await(result) must be(None)
    }

    "return None when agent tries to get details" in {
      implicit val request = FakeRequest()
      implicit val authContext: StandardAuthRetrievals = StandardAuthRetrievals(Set(), Some(AffinityGroup.Agent), None)
      val result = TestDetailsService.getClientMandateDetails("clientId", "ated")(authContext, request, hc)
      await(result) must be(None)
    }

    "return result when found mandate details" in {
      implicit val request = FakeRequest()
      when(mockMandateFrontendConnector.getClientDetails(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(clientMandateDetails))))
      val result = TestDetailsService.getClientMandateDetails("clientId", "ated")
      await(result) must be(Some(clientMandateDetails.as[ClientMandateDetails]))
    }
  }

  "cacheClientReference" must {

    "save the new client ref num, if clear cache is successful" in {
      implicit val request = FakeRequest()
      implicit val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]

      when(mockDataCacheConnector.saveFormData[String](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful("XN1200000100001"))

      val result = TestDetailsService.cacheClientReference("XN1200000100001")(authContext, request, hc)
      await(result) must be("XN1200000100001")
    }

  }


}
