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
import connectors.DataCacheConnector
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants._

import scala.concurrent.Future

class SubscriptionDataServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]

  val addressDetails: AddressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
  val registrationDetails: EtmpRegistrationDetails = RegistrationBuilder.getEtmpRegistrationForOrganisation("testName")

  val mockSubscriptionDataAdapterService: SubscriptionDataAdapterService = mock[SubscriptionDataAdapterService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockDetailsService: DetailsService = mock[DetailsService]

  class Setup {
    val testSubscriptionDataService: SubscriptionDataService = new SubscriptionDataService(
      mockDataCacheConnector,
      mockSubscriptionDataAdapterService,
      mockDetailsService
    )
  }

  override def beforeEach: Unit = {
    reset(mockSubscriptionDataAdapterService)
    reset(mockDataCacheConnector)
    reset(mockDetailsService)
  }

  "Caching Data Service" must {
    "caching" must {

      "return None if we have no cached data and none in etmp" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val successData = CachedData(successResponse)

        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(None))

        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockDetailsService.getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockDataCacheConnector.saveFormData[CachedData]
          (ArgumentMatchers.eq(RetrieveSubscriptionDataId), ArgumentMatchers.any[CachedData]())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(successData))

        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(ArgumentMatchers.any())).thenReturn(None)

        val result: Future[Option[Address]] = testSubscriptionDataService.getCorrespondenceAddress
        val data: Option[Address] = await(result)
        data.isDefined must be(false)
        verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDetailsService, times(0)).getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).saveFormData[CachedData](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockSubscriptionDataAdapterService, times(1)).getCorrespondenceAddress(ArgumentMatchers.any())
      }


      "return the data from the cache if we have some" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val successData = CachedData(successResponse)

        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(successData)))

        val addressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val successAddress = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))
        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(ArgumentMatchers.any())).thenReturn(successAddress)
        val result: Future[Option[Address]] = testSubscriptionDataService.getCorrespondenceAddress
        val data: Option[Address] = await(result)
        data.isDefined must be(true)
        verify(mockSubscriptionDataAdapterService, times(0)).retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).saveFormData[CachedData](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockSubscriptionDataAdapterService, times(1)).getCorrespondenceAddress(ArgumentMatchers.any())
      }

      "return correspondence data from etmp if we have none in cache" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val successData = CachedData(successResponse)

        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(None))

        val successSubscriptionData = SubscriptionData("", "", address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)),
          emailConsent = Some(true))
        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(successSubscriptionData)))
        when(mockDetailsService.getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

        val successAddress = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))
        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(ArgumentMatchers.any())).thenReturn(successAddress)

        when(mockDataCacheConnector.saveFormData[CachedData]
          (ArgumentMatchers.eq(RetrieveSubscriptionDataId), ArgumentMatchers.any[CachedData]())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(successData))

        val result: Future[Option[Address]] = testSubscriptionDataService.getCorrespondenceAddress
        val data: Option[Address] = await(result)
        data.isDefined must be(true)

        verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDetailsService, times(1)).getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).saveFormData[CachedData](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockSubscriptionDataAdapterService, times(1)).getCorrespondenceAddress(ArgumentMatchers.any())
      }

      "return business partner data as None from etmp if we have none in cache" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val successData = CachedData(successResponse)

        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(None))

        val addressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val successSubscriptionData = SubscriptionData("", "", address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)),
          emailConsent = Some(true))
        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(successSubscriptionData)))
        when(mockDetailsService.getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        val successAddress = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))
        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(ArgumentMatchers.any())).thenReturn(successAddress)

        when(mockDataCacheConnector.saveFormData[CachedData]
          (ArgumentMatchers.eq(RetrieveSubscriptionDataId), ArgumentMatchers.any[CachedData]())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(successData))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.getRegisteredDetails
        val data: Option[RegisteredDetails] = await(result)
        data.isDefined must be(false)
        verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).saveFormData[CachedData](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDetailsService, times(1)).getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "return business partner data as Some from etmp if we have none in cache" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val successData = CachedData(successResponse)

        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(None))

        val successSubscriptionData = SubscriptionData("", "", address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)), emailConsent = Some(true))
        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(successSubscriptionData)))
        when(mockDetailsService.getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(registrationDetails)))

        val successAddress = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))
        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(ArgumentMatchers.any())).thenReturn(successAddress)

        when(mockDataCacheConnector.saveFormData[CachedData]
          (ArgumentMatchers.eq(RetrieveSubscriptionDataId), ArgumentMatchers.any[CachedData]())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(successData))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.getRegisteredDetails
        val data: Option[RegisteredDetails] = await(result)
        data.isDefined must be(true)
        verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDetailsService, times(1)).getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }


    "save correspondence address" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

      "return None we have no data to update" in new Setup {
        val updatedContactDetails = ContactDetails()

        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        when(mockSubscriptionDataAdapterService.createUpdateCorrespondenceAddressRequest(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(None)

        val updatedDetails = AddressDetails("Correspondence", "line1", "line2", None, None, None, "GB")
        val result: Future[Option[AddressDetails]] = testSubscriptionDataService.updateCorrespondenceAddressDetails(updatedDetails)
        await(result).isDefined must be(false)

        verify(mockSubscriptionDataAdapterService, times(1)).createUpdateCorrespondenceAddressRequest(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).clearCache()(ArgumentMatchers.any())
      }

      "save the data and clear the cache if it was successful" in new Setup {

        val updatedContactDetails = ContactDetails()

        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

        val updateRequest = UpdateSubscriptionDataRequest(emailConsent = true, ChangeIndicators(), Nil)
        when(mockSubscriptionDataAdapterService.createUpdateCorrespondenceAddressRequest(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Some(updateRequest))

        when(mockSubscriptionDataAdapterService.updateSubscriptionData(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(updateRequest)))
        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val updatedDetails = AddressDetails("Correspondence", "line1", "line2", None, None, None, "GB")
        val result: Future[Option[AddressDetails]] = testSubscriptionDataService.updateCorrespondenceAddressDetails(updatedDetails)
        await(result).isDefined must be(true)

        verify(mockSubscriptionDataAdapterService, times(1)).createUpdateCorrespondenceAddressRequest(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockSubscriptionDataAdapterService, times(1)).updateSubscriptionData(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).clearCache()(ArgumentMatchers.any())

      }
    }

    "save registered details" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cachedOrgResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)),
        registrationDetails = Some(RegistrationBuilder.getEtmpRegistrationForOrganisation("testName")))

      "save the data when we have no cached data" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(None))

        when(mockDetailsService.updateOrganisationRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
        await(result).isDefined must be(false)

        verify(mockDetailsService, times(0)).updateOrganisationRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).clearCache()(ArgumentMatchers.any())
      }

      "save the data when we have no registered detailsl" in new Setup {
        val cachedOrgNoRegistered = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cachedOrgNoRegistered)))

        when(mockDetailsService.updateOrganisationRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
        await(result).isDefined must be(false)

        verify(mockDetailsService, times(0)).updateOrganisationRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).clearCache()(ArgumentMatchers.any())
      }

      "save the data don't clear the cache if it was not successful" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cachedOrgResponse)))

        when(mockDetailsService.updateOrganisationRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
        await(result).isDefined must be(false)

        verify(mockDetailsService, times(1)).updateOrganisationRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).clearCache()(ArgumentMatchers.any())
      }

      "save the data and clear the cache if it was successful" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cachedOrgResponse)))

        val updateDataResponse: UpdateRegistrationDetailsRequest = RegistrationBuilder.getEtmpRegistrationUpdateRequest("testResonse")
        when(mockDetailsService.updateOrganisationRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(updateDataResponse)))
        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
        await(result).isDefined must be(true)

        verify(mockDetailsService, times(1)).updateOrganisationRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).clearCache()(ArgumentMatchers.any())
      }
    }

    "updateOverseasCompanyRegistration" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cachedOrgNoRegistered = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

      "not update when can't retrieve data" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cachedOrgNoRegistered)))

        val updateDataResponse: UpdateRegistrationDetailsRequest = RegistrationBuilder.getEtmpRegistrationUpdateRequest("testResonse")
        when(mockDetailsService.updateOverseasCompanyRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(updateDataResponse)))
        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val result: Future[Option[Identification]] = testSubscriptionDataService.updateOverseasCompanyRegistration(registrationDetails.nonUKIdentification.get)
        await(result).isDefined must be(false)

        verify(mockDetailsService, times(0)).updateOverseasCompanyRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).clearCache()(ArgumentMatchers.any())
      }
    }

    "updateOverseasCompanyRegistration" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cachedOrgResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)),
        registrationDetails = Some(RegistrationBuilder.getEtmpRegistrationForOrganisation("testName")))

      "save the data when we have no cached data" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cachedOrgResponse)))

        val updateDataResponse: UpdateRegistrationDetailsRequest = RegistrationBuilder.getEtmpRegistrationUpdateRequest("testResonse")
        when(mockDetailsService.updateOverseasCompanyRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(updateDataResponse)))
        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val result: Future[Option[Identification]] = testSubscriptionDataService.updateOverseasCompanyRegistration(registrationDetails.nonUKIdentification.get)
        await(result).isDefined must be(true)

        verify(mockDetailsService, times(1)).updateOverseasCompanyRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).clearCache()(ArgumentMatchers.any())
      }
    }

    "getOrganisationName" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
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
      "retrieve cached data and return organisation name" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))
        when(mockSubscriptionDataAdapterService.getOrganisationName(ArgumentMatchers.any())).thenReturn(Some("BusinessName"))
        val response: Future[Option[String]] = testSubscriptionDataService.getOrganisationName
        await(response) must be(Some("BusinessName"))
        verify(mockSubscriptionDataAdapterService, times(1)).getOrganisationName(ArgumentMatchers.eq(cacheDataResponse.registrationDetails))
      }
    }

    "getSafeId" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))
      "retrieve cached data and return safe id" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))
        when(mockSubscriptionDataAdapterService.getSafeId(ArgumentMatchers.any())).thenReturn(Some(cacheDataResponse.subscriptionData.safeId))
        val response: Future[Option[String]] = testSubscriptionDataService.getSafeId
        await(response) must be(Some(cacheDataResponse.subscriptionData.safeId))
        verify(mockSubscriptionDataAdapterService, times(1)).getSafeId(ArgumentMatchers.eq(Some(cacheDataResponse.subscriptionData)))
      }
    }

    "getOverseasCompanyRegistration" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      "returns info from cache" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(None))
        val successSubscriptionData = SubscriptionData("", "",
          address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)), emailConsent = Some(true))
        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(successSubscriptionData)))
        when(mockDetailsService.getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(registrationDetails)))
        val response: Future[Option[Identification]] = testSubscriptionDataService.getOverseasCompanyRegistration
        await(response).get.idNumber must be("AAAAAAAA")
      }
    }

    "email consent" must {

      "return true, if emailConsent is true in cached data" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))
        val response: Future[Boolean] = testSubscriptionDataService.getEmailConsent
        await(response) must be(true)
      }

      "return false, if no cached data is found" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(None))
        val successSubscriptionData = None
        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(successSubscriptionData))
        when(mockDetailsService.getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        val response: Future[Boolean] = testSubscriptionDataService.getEmailConsent
        await(response) must be(false)
      }
    }

    "get email with consent" must {

      "return None, if no cached data is found" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        //Setup Cache
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(None))
        val successSubscriptionData = None
        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(successSubscriptionData))
        when(mockDetailsService.getRegisteredDetailsFromSafeId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

        //Now Mock Specific calls
        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(ArgumentMatchers.any())).thenReturn(None)
        val result: Option[EditContactDetailsEmail] = await(testSubscriptionDataService.getEmailWithConsent)
        result.isDefined must be(false)
      }

      "return None, if we have cached data but no address" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //Setup Cache
        val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(ArgumentMatchers.any())).thenReturn(None)
        val result: Option[EditContactDetailsEmail] = await(testSubscriptionDataService.getEmailWithConsent)
        result.isDefined must be(false)
      }


      "return EmailWithConsent, if we have one" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //Setup Cache
        val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

        val successAddress = Some(Address(Some("name1"), Some("name2"),
          addressDetails = addressDetails,
          contactDetails = Some(ContactDetails(emailAddress = Some("a@b.c")))))
        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(ArgumentMatchers.any())).thenReturn(successAddress)
        val result: Option[EditContactDetailsEmail] = await(testSubscriptionDataService.getEmailWithConsent)
        result.isDefined must be(true)
        result.get.emailConsent must be(true)
        result.get.emailAddress must be("a@b.c")
      }
    }

    "edit EmailWithConsent" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

      "return None we have no data to edit" in new Setup {

        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        when(mockSubscriptionDataAdapterService.createEditEmailWithConsentRequest(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(None)

        val editDetails = EditContactDetailsEmail(emailAddress = "aa@mail.com", emailConsent = true)
        val result: Future[Option[EditContactDetailsEmail]] = testSubscriptionDataService.editEmailWithConsent(editDetails)
        await(result).isDefined must be(false)

        verify(mockSubscriptionDataAdapterService, times(1)).createEditEmailWithConsentRequest(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).clearCache()(ArgumentMatchers.any())
      }

      "save the data and clear the cache if it was successful" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

        val updateRequest = UpdateSubscriptionDataRequest(emailConsent = true, ChangeIndicators(), Nil)
        when(mockSubscriptionDataAdapterService.createEditEmailWithConsentRequest(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Some(updateRequest))

        when(mockSubscriptionDataAdapterService.updateSubscriptionData(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(updateRequest)))
        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val editDetails = EditContactDetailsEmail(
          emailAddress = "aa@mail.com",
          emailConsent = true)
        val result: Future[Option[EditContactDetailsEmail]] = testSubscriptionDataService.editEmailWithConsent(editDetails)
        await(result).isDefined must be(true)

        verify(mockSubscriptionDataAdapterService, times(1)).createEditEmailWithConsentRequest(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockSubscriptionDataAdapterService, times(1)).updateSubscriptionData(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).clearCache()(ArgumentMatchers.any())

      }
    }

    "edit contact details" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

      "return None we have no data to edit" in new Setup {

        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        when(mockSubscriptionDataAdapterService.createEditContactDetailsRequest(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(None)

        val editDetails = EditContactDetails("name1", "name2", phoneNumber = "123456779")
        val result: Future[Option[EditContactDetails]] = testSubscriptionDataService.editContactDetails(editDetails)
        await(result).isDefined must be(false)

        verify(mockSubscriptionDataAdapterService, times(1)).createEditContactDetailsRequest(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).clearCache()(ArgumentMatchers.any())
      }

      "save the data and clear the cache if it was successful" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[CachedData](ArgumentMatchers.eq(RetrieveSubscriptionDataId))
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(CachedData.formats))).thenReturn(Future.successful(Some(cacheDataResponse)))

        val updateRequest = UpdateSubscriptionDataRequest(emailConsent = true, ChangeIndicators(), Nil)
        when(mockSubscriptionDataAdapterService.createEditContactDetailsRequest(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Some(updateRequest))

        when(mockSubscriptionDataAdapterService.updateSubscriptionData(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(updateRequest)))
        when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

        val editDetails = EditContactDetails(firstName = "TestFirstName",
          lastName = "TestLastName",
          phoneNumber = "123456779")
        val result: Future[Option[EditContactDetails]] = testSubscriptionDataService.editContactDetails(editDetails)
        await(result).isDefined must be(true)

        verify(mockSubscriptionDataAdapterService, times(1)).createEditContactDetailsRequest(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockSubscriptionDataAdapterService, times(1)).updateSubscriptionData(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).clearCache()(ArgumentMatchers.any())

      }
    }
  }
}
