/*
 * Copyright 2025 HM Revenue & Customs
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
import connectors.{AgentClientMandateFrontendConnector, AtedConnector, DataCacheService}
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, InternalServerException}
import play.api.test.Injecting

import scala.concurrent.{ExecutionContext, Future}

class DetailsServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with Injecting {

  implicit val ec: ExecutionContext = inject[ExecutionContext]
  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val mockMandateFrontendConnector: AgentClientMandateFrontendConnector = mock[AgentClientMandateFrontendConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  class Setup {
    val testDetailsService: DetailsService = new DetailsService(
      mockAtedConnector,
      mockMandateFrontendConnector,
      mockDataCacheService
    )

    def setupCommonMocks(
                          atedGetDetailsResponse: HttpResponse = HttpResponse(OK, ""),
                          atedUpdateRegistrationDetailsResponse: HttpResponse = HttpResponse(OK, ""),
                          mandateFrontendGetClientDetailsResponse: HttpResponse = HttpResponse(NOT_FOUND, ""),
                          dataCacheSaveFormDataResponse: String = ""
                        ): Unit = {
      when(mockAtedConnector.getDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(atedGetDetailsResponse))

      when(mockAtedConnector.updateRegistrationDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(atedUpdateRegistrationDetailsResponse))

      when(mockMandateFrontendConnector.getClientDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(mandateFrontendGetClientDetailsResponse))

      when(mockDataCacheConnector.saveFormData[String](ArgumentMatchers.any(), ArgumentMatchers.any())(
        ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(dataCacheSaveFormDataResponse))
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAtedConnector, mockMandateFrontendConnector, mockDataCacheConnector)
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
      | "changeEmailLink": "changeEmailUrl",
      | "status": ""
      | }
    """.stripMargin
  )

  val failureResponse: JsValue = Json.parse( """{"reason":"Agent not found!"}""")
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  val identifier = "JARN1234567"
  val identifierType = "arn"

  "getDetails" must {
    "for OK response status, return body as Some(EtmpRegistrationDetails)" in new Setup {
      setupCommonMocks(atedGetDetailsResponse = HttpResponse(OK, successResponseInd.toString))
      val result: Future[Option[EtmpRegistrationDetails]] = testDetailsService.getDetails(identifier, identifierType)
      await(result) must be(Some(successResponseInd.as[EtmpRegistrationDetails]))
    }
    "for NOT_FOUND response status, return body as None" in new Setup {
      setupCommonMocks(atedGetDetailsResponse = HttpResponse(NOT_FOUND, failureResponse.toString))
      val result: Future[Option[EtmpRegistrationDetails]] = testDetailsService.getDetails(identifier, identifierType)
      await(result) must be(None)
    }
    "for BAD_REQUEST response status, throw bad request exception" in new Setup {
      setupCommonMocks(atedGetDetailsResponse = HttpResponse(BAD_REQUEST, ""))
      val result: Future[Option[EtmpRegistrationDetails]] = testDetailsService.getDetails(identifier, identifierType)
      val thrown: BadRequestException = the[BadRequestException] thrownBy await(result)
      thrown.message must include("Bad Data")
    }
    "getAgentDetails throws InternalServerException exception for call to ETMP, when BadRequest response is received" in new Setup {
      setupCommonMocks(atedGetDetailsResponse = HttpResponse(INTERNAL_SERVER_ERROR, ""))
      val result: Future[Option[EtmpRegistrationDetails]] = testDetailsService.getDetails(identifier, identifierType)
      val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
      thrown.message must include("Internal server error")
    }
  }

  "getBusinessPartnerDetails" must {
    "return details for individual" in new Setup {
      val indResponse: EtmpRegistrationDetails = RegistrationBuilder.getEtmpRegistrationForIndividual("TestFirstName", "TestLastName")
      val successResponseInd: JsValue = Json.toJson(indResponse)
      setupCommonMocks(atedGetDetailsResponse = HttpResponse(OK, successResponseInd.toString))
      val result: Future[Option[EtmpRegistrationDetails]] = testDetailsService.getRegisteredDetailsFromSafeId("1234567890")
      val bp: Option[EtmpRegistrationDetails] = await(result)
      bp.isDefined must be(true)
      bp.get.name must be(Some("TestFirstName TestLastName"))
      bp.get.isEditable must be(indResponse.isEditable)
      bp.get.addressDetails.addressLine1 must be(indResponse.addressDetails.addressLine1)
    }

    "return details for organisation" in new Setup {
      val orgResponse: EtmpRegistrationDetails = RegistrationBuilder.getEtmpRegistrationForOrganisation("Agents Limited")
      val successResponseOrg: JsValue = Json.toJson(orgResponse)
      setupCommonMocks(atedGetDetailsResponse = HttpResponse(OK, successResponseOrg.toString))
      val result: Future[Option[EtmpRegistrationDetails]] = testDetailsService.getRegisteredDetailsFromSafeId("1234567890")
      val bp: Option[EtmpRegistrationDetails] = await(result)
      bp.isDefined must be(true)
      bp.get.name must be(Some("Agents Limited"))
      bp.get.isEditable must be(orgResponse.isEditable)
      bp.get.addressDetails.addressLine1 must be(orgResponse.addressDetails.addressLine1)
    }

    "return details for organisation when not found" in new Setup {
      setupCommonMocks(atedGetDetailsResponse = HttpResponse(NOT_FOUND, failureResponse.toString))
      val result: Future[Option[EtmpRegistrationDetails]] = testDetailsService.getRegisteredDetailsFromSafeId("1234567890")
      val bp: Option[EtmpRegistrationDetails] = await(result)
      bp.isDefined must be(false)
    }
  }

  "update registration data" must {
    "save registration details" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val registeredDetails = RegisteredDetails(isEditable = true, "testName", RegisteredAddressDetails(addressLine1 = "newLine1",
        addressLine2 = "newLine2", countryCode = "GB"))
      val oldOrgDetails = RegistrationBuilder.getEtmpRegistrationForOrganisation("oldName")

      "save the registration details for an organisation" in new Setup {

        setupCommonMocks(atedUpdateRegistrationDetailsResponse = HttpResponse(BAD_REQUEST, ""))

        val result: Future[Option[UpdateRegistrationDetailsRequest]] = testDetailsService.updateOrganisationRegisteredDetails(oldOrgDetails, registeredDetails)
        val updatedDetails: Option[UpdateRegistrationDetailsRequest] = await(result)
        updatedDetails.isDefined must be(false)
      }

      "save the registration details successful for an organisation" in new Setup {
        setupCommonMocks(atedUpdateRegistrationDetailsResponse = HttpResponse(OK, ""))

        val result: Future[Option[UpdateRegistrationDetailsRequest]] = testDetailsService.updateOrganisationRegisteredDetails(oldOrgDetails, registeredDetails)
        val updatedDetails: Option[UpdateRegistrationDetailsRequest] = await(result)
        updatedDetails.isDefined must be(true)

      }

      "fail to save the registration details for an organisation without isAGroup defined" in new Setup {
        val oldOrgDetailsNoGroup: EtmpRegistrationDetails = oldOrgDetails.copy(organisation = oldOrgDetails.organisation.map(_.copy(isAGroup = None)))

        setupCommonMocks(atedUpdateRegistrationDetailsResponse = HttpResponse(OK, ""))

        val result: Future[Option[UpdateRegistrationDetailsRequest]] = testDetailsService.updateOrganisationRegisteredDetails(oldOrgDetailsNoGroup,
          registeredDetails)
        val updatedDetails: Option[UpdateRegistrationDetailsRequest] = await(result)
        updatedDetails.isDefined must be(false)
      }

      "fail to save the registration details for an individual" in new Setup {
        val oldIndDetails: EtmpRegistrationDetails = RegistrationBuilder.getEtmpRegistrationForIndividual("TestFirstName", "TestLastName")

        setupCommonMocks(atedUpdateRegistrationDetailsResponse = HttpResponse(OK, ""))

        val result: Future[Option[UpdateRegistrationDetailsRequest]] = testDetailsService.updateOrganisationRegisteredDetails(oldIndDetails, registeredDetails)
        val updatedDetails: Option[UpdateRegistrationDetailsRequest] = await(result)
        updatedDetails.isDefined must be(false)

      }
    }
  }

  "update overseas company registration data" must {
    "save details" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val overseasCompanyRegistration = Identification("AAAAAAAAA", "Some Place", "FR")
      val oldOrgDetails = RegistrationBuilder.getEtmpRegistrationForOrganisation("oldName")

      "save the details for an organisation" in new Setup {

        setupCommonMocks(atedUpdateRegistrationDetailsResponse = HttpResponse(BAD_REQUEST, ""))

        val result: Future[Option[UpdateRegistrationDetailsRequest]] = testDetailsService.updateOverseasCompanyRegistration(oldOrgDetails,
          Some(overseasCompanyRegistration))
        val updatedDetails: Option[UpdateRegistrationDetailsRequest] = await(result)
        updatedDetails.isDefined must be(false)
      }

      "save the details successfully for an organisation" in new Setup {
        setupCommonMocks(atedUpdateRegistrationDetailsResponse = HttpResponse(OK, ""))

        val result: Future[Option[UpdateRegistrationDetailsRequest]] = testDetailsService.updateOverseasCompanyRegistration(oldOrgDetails,
          Some(overseasCompanyRegistration))
        val updatedDetails: Option[UpdateRegistrationDetailsRequest] = await(result)
        updatedDetails.isDefined must be(true)

      }

      "fail to save the registration details for an organisation without isAGroup defined" in new Setup {
        val oldOrgDetailsNoGroup: EtmpRegistrationDetails = oldOrgDetails.copy(organisation = oldOrgDetails.organisation.map(_.copy(isAGroup = None)))

        setupCommonMocks(atedUpdateRegistrationDetailsResponse = HttpResponse(OK, ""))

        val result: Future[Option[UpdateRegistrationDetailsRequest]] = testDetailsService.updateOverseasCompanyRegistration(oldOrgDetailsNoGroup,
          Some(overseasCompanyRegistration))
        val updatedDetails: Option[UpdateRegistrationDetailsRequest] = await(result)
        updatedDetails.isDefined must be(false)
      }

      "fail to save the registration details for an individual" in new Setup {
        val oldIndDetails: EtmpRegistrationDetails = RegistrationBuilder.getEtmpRegistrationForIndividual("TestFirstName", "TestLastName")

        setupCommonMocks(atedUpdateRegistrationDetailsResponse = HttpResponse(OK, ""))

        val result: Future[Option[UpdateRegistrationDetailsRequest]] = testDetailsService.updateOverseasCompanyRegistration(oldIndDetails,
          Some(overseasCompanyRegistration))
        val updatedDetails: Option[UpdateRegistrationDetailsRequest] = await(result)
        updatedDetails.isDefined must be(false)

      }
    }
  }

  "getClientMandateDetails" must {
    "return None when can't find mandate details" in new Setup {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      setupCommonMocks(mandateFrontendGetClientDetailsResponse = HttpResponse(NOT_FOUND, ""))
      val result: Future[Option[ClientMandateDetails]] = testDetailsService.getClientMandateDetails("clientId", "ated")
      await(result) must be(None)
    }

    "return None when agent tries to get details" in new Setup {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      implicit val authContext: StandardAuthRetrievals = StandardAuthRetrievals(Set(), Some(AffinityGroup.Agent), None)
      val result: Future[Option[ClientMandateDetails]] = testDetailsService.getClientMandateDetails("clientId", "ated")(authContext, request)
      await(result) must be(None)
    }

    "return result when found mandate details" in new Setup {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      setupCommonMocks(mandateFrontendGetClientDetailsResponse = HttpResponse(OK, clientMandateDetails.toString))
      val result: Future[Option[ClientMandateDetails]] = testDetailsService.getClientMandateDetails("clientId", "ated")
      await(result) must be(Some(clientMandateDetails.as[ClientMandateDetails]))
    }
  }

  "cacheClientReference" must {

    "save the new client ref num, if clear cache is successful" in new Setup {

      when(mockDataCacheService.saveFormData[String](ArgumentMatchers.any(), ArgumentMatchers.any())(
        ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful("XN1200000100001"))
      setupCommonMocks(dataCacheSaveFormDataResponse = "XN1200000100001")

      val result: Future[String] = testDetailsService.cacheClientReference("XN1200000100001")(hc)
      await(result) must be("XN1200000100001")
    }

  }
}
