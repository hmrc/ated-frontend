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

import java.util.UUID

import builders.{AuthBuilder, RegistrationBuilder}
import connectors.DataCacheConnector
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants._

import scala.concurrent.Future

class SubscriptionDataServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockSubscriptionDataAdapterService = mock[SubscriptionDataAdapterService]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockDetailsService = mock[DetailsService]

  object TestSubscriptionDataService extends SubscriptionDataService {
    override val dataCacheConnector = mockDataCacheConnector
    override val subscriptionDataAdapterService = mockSubscriptionDataAdapterService
    override val detailsDataService = mockDetailsService
  }


  override def beforeEach = {
    reset(mockDataCacheConnector)
    reset(mockSubscriptionDataAdapterService)
    reset(mockDetailsService)
  }

  implicit val user = createAtedContext(createAgentAuthContext("User-Id", "name", Some("JARN1234567")))

  val addressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
  val registrationDetails = RegistrationBuilder.getEtmpRegistrationForOrganisation("testName")

  "Caching Data Service" must {
    "use the correct connectors" in {
      SubscriptionDataService.dataCacheConnector must be(DataCacheConnector)
    }

  }

  "caching" must {

    "return None if we have no cached data and none in etmp" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
      val successData = CachedData(successResponse)

      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(None))

      when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(mockDetailsService.getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheConnector.saveFormData[CachedData]
        (Matchers.eq(RetrieveSubscriptionDataId), Matchers.any[CachedData]())
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(successData))

      when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(Matchers.any())).thenReturn(None)

      val result = TestSubscriptionDataService.getCorrespondenceAddress
      val data = await(result)
      data.isDefined must be(false)
      verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(Matchers.any(), Matchers.any())
      verify(mockDetailsService, times(0)).getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(0)).saveFormData[CachedData](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      verify(mockSubscriptionDataAdapterService, times(1)).getCorrespondenceAddress(Matchers.any())
    }


    "return the data from the cache if we have some" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
      val successData = CachedData(successResponse)

      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(successData)))

      val addressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
      val successAddress = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))
      when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(Matchers.any())).thenReturn(successAddress)
      val result = TestSubscriptionDataService.getCorrespondenceAddress
      val data = await(result)
      data.isDefined must be(true)
      verify(mockSubscriptionDataAdapterService, times(0)).retrieveSubscriptionData(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(0)).saveFormData[CachedData](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      verify(mockSubscriptionDataAdapterService, times(1)).getCorrespondenceAddress(Matchers.any())
    }

    "return correspondence data from etmp if we have none in cache" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
      val successData = CachedData(successResponse)

      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(None))


      val successSubscriptionData = SubscriptionData("", "", address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)), emailConsent = Some(true))
      when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(successSubscriptionData)))
      when(mockDetailsService.getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

      val successAddress = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))
      when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(Matchers.any())).thenReturn(successAddress)

      when(mockDataCacheConnector.saveFormData[CachedData]
        (Matchers.eq(RetrieveSubscriptionDataId), Matchers.any[CachedData]())
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(successData))

      val result = TestSubscriptionDataService.getCorrespondenceAddress
      val data = await(result)
      data.isDefined must be(true)

      verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(Matchers.any(), Matchers.any())
      verify(mockDetailsService, times(1)).getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(1)).saveFormData[CachedData](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      verify(mockSubscriptionDataAdapterService, times(1)).getCorrespondenceAddress(Matchers.any())
    }

    "return business partner data as None from etmp if we have none in cache" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
      val successData = CachedData(successResponse)

      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(None))

      val addressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
      val successSubscriptionData = SubscriptionData("", "", address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)), emailConsent = Some(true))
      when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(successSubscriptionData)))
      when(mockDetailsService.getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

      val successAddress = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))
      when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(Matchers.any())).thenReturn(successAddress)

      when(mockDataCacheConnector.saveFormData[CachedData]
        (Matchers.eq(RetrieveSubscriptionDataId), Matchers.any[CachedData]())
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(successData))

      val result = TestSubscriptionDataService.getRegisteredDetails
      val data = await(result)
      data.isDefined must be(false)
      verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(1)).saveFormData[CachedData](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      verify(mockDetailsService, times(1)).getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())
    }

    "return business partner data as Some from etmp if we have none in cache" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
      val successData = CachedData(successResponse)

      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(None))

      val successSubscriptionData = SubscriptionData("", "", address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)), emailConsent = Some(true))
      when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(successSubscriptionData)))
      when(mockDetailsService.getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(registrationDetails)))

      val successAddress = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))
      when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(Matchers.any())).thenReturn(successAddress)

      when(mockDataCacheConnector.saveFormData[CachedData]
        (Matchers.eq(RetrieveSubscriptionDataId), Matchers.any[CachedData]())
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(successData))

      val result = TestSubscriptionDataService.getRegisteredDetails
      val data = await(result)
      data.isDefined must be(true)
      verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(Matchers.any(), Matchers.any())
      verify(mockDetailsService, times(1)).getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())
    }
  }


  "save correspondence address" must {
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

    "return None we have no data to update" in {
      val updatedContactDetails = ContactDetails()

      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      when(mockSubscriptionDataAdapterService.createUpdateCorrespondenceAddressRequest(Matchers.any(), Matchers.any())).thenReturn(None)

      val updatedDetails = AddressDetails("Correspondence", "line1", "line2", None, None, None, "GB")
      val result = TestSubscriptionDataService.updateCorrespondenceAddressDetails(updatedDetails)
      await(result).isDefined must be(false)

      verify(mockSubscriptionDataAdapterService, times(1)).createUpdateCorrespondenceAddressRequest(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(0)).clearCache()(Matchers.any())
    }

    "save the data and clear the cache if it was successful" in {

      val updatedContactDetails = ContactDetails()

      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

      val updateRequest = UpdateSubscriptionDataRequest(true, ChangeIndicators(), Nil)
      when(mockSubscriptionDataAdapterService.createUpdateCorrespondenceAddressRequest(Matchers.any(), Matchers.any())).thenReturn(Some(updateRequest))

      when(mockSubscriptionDataAdapterService.updateSubscriptionData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(updateRequest)))
      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val updatedDetails = AddressDetails("Correspondence", "line1", "line2", None, None, None, "GB")
      val result = TestSubscriptionDataService.updateCorrespondenceAddressDetails(updatedDetails)
      await(result).isDefined must be(true)

      verify(mockSubscriptionDataAdapterService, times(1)).createUpdateCorrespondenceAddressRequest(Matchers.any(), Matchers.any())
      verify(mockSubscriptionDataAdapterService, times(1)).updateSubscriptionData(Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(1)).clearCache()(Matchers.any())

    }
  }

  "save registered details" must {
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val cachedOrgResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)),
      registrationDetails = Some(RegistrationBuilder.getEtmpRegistrationForOrganisation("testName")))

    "save the data when we have no cached data" in {
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(None))

      when(mockDetailsService.updateOrganisationRegisteredDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val result = TestSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
      await(result).isDefined must be(false)

      verify(mockDetailsService, times(0)).updateOrganisationRegisteredDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(0)).clearCache()(Matchers.any())
    }

    "save the data when we have no registered detailsl" in {
      val cachedOrgNoRegistered = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cachedOrgNoRegistered)))

      when(mockDetailsService.updateOrganisationRegisteredDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val result = TestSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
      await(result).isDefined must be(false)

      verify(mockDetailsService, times(0)).updateOrganisationRegisteredDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(0)).clearCache()(Matchers.any())
    }

    "save the data don't clear the cache if it was not successful" in {
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cachedOrgResponse)))

      when(mockDetailsService.updateOrganisationRegisteredDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val result = TestSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
      await(result).isDefined must be(false)

      verify(mockDetailsService, times(1)).updateOrganisationRegisteredDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(0)).clearCache()(Matchers.any())
    }

    "save the data and clear the cache if it was successful" in {
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cachedOrgResponse)))

      val updateDataResponse = RegistrationBuilder.getEtmpRegistrationUpdateRequest("testResonse")
      when(mockDetailsService.updateOrganisationRegisteredDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(updateDataResponse)))
      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val result = TestSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
      await(result).isDefined must be(true)

      verify(mockDetailsService, times(1)).updateOrganisationRegisteredDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(1)).clearCache()(Matchers.any())
    }
  }

  "updateOverseasCompanyRegistration" must {
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val cachedOrgResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)),
      registrationDetails = Some(RegistrationBuilder.getEtmpRegistrationForOrganisation("testName")))
    val cachedOrgNoRegistered = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

    "not update when can't retrieve data" in {
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cachedOrgNoRegistered)))

      val updateDataResponse = RegistrationBuilder.getEtmpRegistrationUpdateRequest("testResonse")
      when(mockDetailsService.updateOverseasCompanyRegistration(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(updateDataResponse)))
      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val result = TestSubscriptionDataService.updateOverseasCompanyRegistration(registrationDetails.nonUKIdentification.get)
      await(result).isDefined must be(false)

      verify(mockDetailsService, times(0)).updateOverseasCompanyRegistration(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(0)).clearCache()(Matchers.any())
    }
  }

  "updateOverseasCompanyRegistration" must {
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val cachedOrgResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)),
      registrationDetails = Some(RegistrationBuilder.getEtmpRegistrationForOrganisation("testName")))

    "save the data when we have no cached data" in {
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cachedOrgResponse)))

      val updateDataResponse = RegistrationBuilder.getEtmpRegistrationUpdateRequest("testResonse")
      when(mockDetailsService.updateOverseasCompanyRegistration(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(updateDataResponse)))
      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val result = TestSubscriptionDataService.updateOverseasCompanyRegistration(registrationDetails.nonUKIdentification.get)
      await(result).isDefined must be(true)

      verify(mockDetailsService, times(1)).updateOverseasCompanyRegistration(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(1)).clearCache()(Matchers.any())
    }
  }

  "getOrganisationName" must {
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val etmpRegDetails = EtmpRegistrationDetails(sapNumber = "12345678", safeId = "X1234567",
      organisation = Some(Organisation("BusinessName")), isAnIndividual = false,
      addressDetails = RegisteredAddressDetails("line 1", "line 2", Some("line 3"), countryCode = "UK"),
      contactDetails = ContactDetails(),
      individual = None,
      isEditable = false,
      isAnAgent = false,
      agentReferenceNumber = None,
      nonUKIdentification = None)
    val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)), Some(etmpRegDetails))
    "retrieve cached data and return organisation name" in {
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))
      when(mockSubscriptionDataAdapterService.getOrganisationName(Matchers.any())).thenReturn(Some("BusinessName"))
      val response = TestSubscriptionDataService.getOrganisationName
      await(response) must be(Some("BusinessName"))
      verify(mockSubscriptionDataAdapterService, times(1)).getOrganisationName(Matchers.eq(cacheDataResponse.registrationDetails))
    }
  }

  "getSafeId" must {
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))
    "retrieve cached data and return safe id" in {
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))
      when(mockSubscriptionDataAdapterService.getSafeId(Matchers.any())).thenReturn(Some(cacheDataResponse.subscriptionData.safeId))
      val response = TestSubscriptionDataService.getSafeId
      await(response) must be(Some(cacheDataResponse.subscriptionData.safeId))
      verify(mockSubscriptionDataAdapterService, times(1)).getSafeId(Matchers.eq(Some(cacheDataResponse.subscriptionData)))
    }
  }

  "getOverseasCompanyRegistration" must {
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))
    "returns info from cache" in {
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(None))
      val successSubscriptionData = SubscriptionData("", "", address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)), emailConsent = Some(true))
      when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(successSubscriptionData)))
      when(mockDetailsService.getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(registrationDetails)))
      val response = TestSubscriptionDataService.getOverseasCompanyRegistration
      await(response).get.idNumber must be("AAAAAAAA")
    }
  }

  "email consent" must {

    "return true, if emailConsent is true in cached data" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))
      val response = TestSubscriptionDataService.getEmailConsent
      await(response) must be (true)
    }

    "return false, if no cached data is found" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(None))
      val successSubscriptionData = None
      when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(Matchers.any(), Matchers.any())).thenReturn(Future.successful(successSubscriptionData))
      when(mockDetailsService.getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      val response = TestSubscriptionDataService.getEmailConsent
      await(response) must be (false)
    }
  }

  "get email with consent" must {

    "return None, if no cached data is found" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      //Setup Cache
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(None))
      val successSubscriptionData = None
      when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(Matchers.any(), Matchers.any())).thenReturn(Future.successful(successSubscriptionData))
      when(mockDetailsService.getRegisteredDetailsFromSafeId(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

      //Now Mock Specific calls
      when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(Matchers.any())).thenReturn(None)
      val result = await(TestSubscriptionDataService.getEmailWithConsent)
      result.isDefined must be (false)
    }

    "return None, if we have cached data but no address" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      //Setup Cache
      val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

      when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(Matchers.any())).thenReturn(None)
      val result = await(TestSubscriptionDataService.getEmailWithConsent)
      result.isDefined must be (false)
    }


    "return EmailWithConsent, if we have one" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      //Setup Cache
      val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

      val successAddress = Some(Address(Some("name1"), Some("name2"),
        addressDetails = addressDetails,
        contactDetails = Some(ContactDetails(emailAddress = Some("a@b.c")))))
      when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(Matchers.any())).thenReturn(successAddress)
      val result = await(TestSubscriptionDataService.getEmailWithConsent)
      result.isDefined must be (true)
      result.get.emailConsent must be (true)
      result.get.emailAddress must be ("a@b.c")
    }
  }

  "edit EmailWithConsent" must {
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

    "return None we have no data to edit" in {

      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      when(mockSubscriptionDataAdapterService.createEditEmailWithConsentRequest(Matchers.any(), Matchers.any())).thenReturn(None)

      val editDetails = EditContactDetailsEmail(emailAddress = "aa@mail.com",emailConsent = true)
      val result = TestSubscriptionDataService.editEmailWithConsent(editDetails)
      await(result).isDefined must be(false)

      verify(mockSubscriptionDataAdapterService, times(1)).createEditEmailWithConsentRequest(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(0)).clearCache()(Matchers.any())
    }

    "save the data and clear the cache if it was successful" in {
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

      val updateRequest = UpdateSubscriptionDataRequest(true, ChangeIndicators(), Nil)
      when(mockSubscriptionDataAdapterService.createEditEmailWithConsentRequest(Matchers.any(), Matchers.any())).thenReturn(Some(updateRequest))

      when(mockSubscriptionDataAdapterService.updateSubscriptionData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(updateRequest)))
      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val editDetails = EditContactDetailsEmail(
        emailAddress = "aa@mail.com",
        emailConsent = true)
      val result = TestSubscriptionDataService.editEmailWithConsent(editDetails)
      await(result).isDefined must be(true)

      verify(mockSubscriptionDataAdapterService, times(1)).createEditEmailWithConsentRequest(Matchers.any(), Matchers.any())
      verify(mockSubscriptionDataAdapterService, times(1)).updateSubscriptionData(Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(1)).clearCache()(Matchers.any())

    }
  }

  "edit contact details" must {
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

    "return None we have no data to edit" in {

      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      when(mockSubscriptionDataAdapterService.createEditContactDetailsRequest(Matchers.any(), Matchers.any())).thenReturn(None)

      val editDetails = EditContactDetails("name1", "name2",  phoneNumber = "123456779")
      val result = TestSubscriptionDataService.editContactDetails(editDetails)
      await(result).isDefined must be(false)

      verify(mockSubscriptionDataAdapterService, times(1)).createEditContactDetailsRequest(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(0)).clearCache()(Matchers.any())
    }

    "save the data and clear the cache if it was successful" in {
      when(mockDataCacheConnector.fetchAndGetFormData[CachedData](Matchers.eq(RetrieveSubscriptionDataId))
        (Matchers.any(), Matchers.any(), Matchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

      val updateRequest = UpdateSubscriptionDataRequest(true, ChangeIndicators(), Nil)
      when(mockSubscriptionDataAdapterService.createEditContactDetailsRequest(Matchers.any(), Matchers.any())).thenReturn(Some(updateRequest))

      when(mockSubscriptionDataAdapterService.updateSubscriptionData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(updateRequest)))
      when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val editDetails = EditContactDetails(firstName = "TestFirstName",
        lastName = "TestLastName",
        phoneNumber = "123456779")
      val result = TestSubscriptionDataService.editContactDetails(editDetails)
      await(result).isDefined must be(true)

      verify(mockSubscriptionDataAdapterService, times(1)).createEditContactDetailsRequest(Matchers.any(), Matchers.any())
      verify(mockSubscriptionDataAdapterService, times(1)).updateSubscriptionData(Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockDataCacheConnector, times(1)).clearCache()(Matchers.any())

    }
  }
}
