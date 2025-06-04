/*
 * Copyright 2023 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants._

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionDataServiceSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite with Injecting {

  implicit val ec: ExecutionContext = inject[ExecutionContext]
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

  override def beforeEach(): Unit = {
    reset(mockSubscriptionDataAdapterService)
    reset(mockDataCacheConnector)
    reset(mockDetailsService)
  }

  "Caching Data Service" must {
    "caching" must {
      "return None if we have no cached data and none in etmp" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val successData: CachedData = CachedData(successResponse)

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDetailsService.getRegisteredDetailsFromSafeId(any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.saveFormData[CachedData](eqTo(RetrieveSubscriptionDataId), any[CachedData]())(any(), any()))
          .thenReturn(Future.successful(successData))

        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(any()))
          .thenReturn(None)

        val result: Future[Option[Address]] = testSubscriptionDataService.getCorrespondenceAddress
        val data: Option[Address] = await(result)
        data.isDefined must be(false)

        verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(any(), any())
        verify(mockDetailsService, times(0)).getRegisteredDetailsFromSafeId(any())(any(), any())
        verify(mockDataCacheConnector, times(0)).saveFormData[CachedData](any(), any())(any(), any())
        verify(mockSubscriptionDataAdapterService, times(1)).getCorrespondenceAddress(any())
      }


      "return the data from the cache if we have some" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val successData: CachedData = CachedData(successResponse)

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(successData)))

        val addressDetails: AddressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val successAddress: Option[Address] = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))

        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(any()))
          .thenReturn(successAddress)

        val result: Future[Option[Address]] = testSubscriptionDataService.getCorrespondenceAddress
        val data: Option[Address] = await(result)
        data.isDefined must be(true)

        verify(mockSubscriptionDataAdapterService, times(0)).retrieveSubscriptionData(any(), any())
        verify(mockDataCacheConnector, times(0)).saveFormData[CachedData](any(), any())(any(), any())
        verify(mockSubscriptionDataAdapterService, times(1)).getCorrespondenceAddress(any())
      }

      "return correspondence data from etmp if we have none in cache" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val successData: CachedData = CachedData(successResponse)

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(None))

        val successSubscriptionData: SubscriptionData =
          SubscriptionData("", "", address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)), emailConsent = Some(true))

        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(any(), any()))
          .thenReturn(Future.successful(Some(successSubscriptionData)))

        when(mockDetailsService.getRegisteredDetailsFromSafeId(any())(any(), any()))
          .thenReturn(Future.successful(None))

        val successAddress: Option[Address] = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))

        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(any()))
          .thenReturn(successAddress)

        when(mockDataCacheConnector.saveFormData[CachedData](eqTo(RetrieveSubscriptionDataId), any[CachedData]())(any(), any()))
          .thenReturn(Future.successful(successData))

        val result: Future[Option[Address]] = testSubscriptionDataService.getCorrespondenceAddress
        val data: Option[Address] = await(result)
        data.isDefined must be(true)

        verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(any(), any())
        verify(mockDetailsService, times(1)).getRegisteredDetailsFromSafeId(any())(any(), any())
        verify(mockDataCacheConnector, times(1)).saveFormData[CachedData](any(), any())(any(), any())
        verify(mockSubscriptionDataAdapterService, times(1)).getCorrespondenceAddress(any())
      }

      "return business partner data as None from etmp if we have none in cache" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val successData: CachedData = CachedData(successResponse)

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(None))

        val addressDetails: AddressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val successSubscriptionData: SubscriptionData =
          SubscriptionData("", "", address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)), emailConsent = Some(true))

        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(any(), any()))
          .thenReturn(Future.successful(Some(successSubscriptionData)))

        when(mockDetailsService.getRegisteredDetailsFromSafeId(any())(any(), any()))
          .thenReturn(Future.successful(None))

        val successAddress: Option[Address] = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))

        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(any()))
          .thenReturn(successAddress)

        when(mockDataCacheConnector.saveFormData[CachedData](eqTo(RetrieveSubscriptionDataId), any[CachedData]())(any(), any()))
          .thenReturn(Future.successful(successData))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.getRegisteredDetails
        val data: Option[RegisteredDetails] = await(result)
        data.isDefined must be(false)

        verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(any(), any())
        verify(mockDataCacheConnector, times(1)).saveFormData[CachedData](any(), any())(any(), any())
        verify(mockDetailsService, times(1)).getRegisteredDetailsFromSafeId(any())(any(), any())
      }

      "return business partner data as Some from etmp if we have none in cache" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val successResponse: SubscriptionData = SubscriptionData("", "", address = Nil, emailConsent = Some(true))
        val successData: CachedData = CachedData(successResponse)

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))
          (any(), any())).thenReturn(Future.successful(None))

        val successSubscriptionData: SubscriptionData =
          SubscriptionData("", "", address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)), emailConsent = Some(true))

        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(any(), any()))
          .thenReturn(Future.successful(Some(successSubscriptionData)))

        when(mockDetailsService.getRegisteredDetailsFromSafeId(any())(any(), any()))
          .thenReturn(Future.successful(Some(registrationDetails)))

        val successAddress: Option[Address] = Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails))

        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(any()))
          .thenReturn(successAddress)

        when(mockDataCacheConnector.saveFormData[CachedData](eqTo(RetrieveSubscriptionDataId), any[CachedData]())(any(), any()))
          .thenReturn(Future.successful(successData))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.getRegisteredDetails
        val data: Option[RegisteredDetails] = await(result)
        data.isDefined must be(true)

        verify(mockSubscriptionDataAdapterService, times(1)).retrieveSubscriptionData(any(), any())
        verify(mockDetailsService, times(1)).getRegisteredDetailsFromSafeId(any())(any(), any())
      }
    }


    "save correspondence address" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

      "return None we have no data to update" in new Setup {

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        when(mockSubscriptionDataAdapterService.createUpdateCorrespondenceAddressRequest(any(), any()))
          .thenReturn(None)

        val updatedDetails: AddressDetails = AddressDetails("Correspondence", "line1", "line2", None, None, None, "GB")
        val result: Future[Option[AddressDetails]] = testSubscriptionDataService.updateCorrespondenceAddressDetails(updatedDetails)
        await(result).isDefined must be(false)

        verify(mockSubscriptionDataAdapterService, times(1)).createUpdateCorrespondenceAddressRequest(any(), any())
        verify(mockDataCacheConnector, times(0)).clearCache()(any())
      }

      "save the data and clear the cache if it was successful" in new Setup {

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        val updateRequest: UpdateSubscriptionDataRequest = UpdateSubscriptionDataRequest(emailConsent = true, ChangeIndicators(), Nil)

        when(mockSubscriptionDataAdapterService.createUpdateCorrespondenceAddressRequest(any(), any()))
          .thenReturn(Some(updateRequest))

        when(mockSubscriptionDataAdapterService.updateSubscriptionData(any())(any(), any()))
          .thenReturn(Future.successful(Some(updateRequest)))

        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val updatedDetails: AddressDetails = AddressDetails("Correspondence", "line1", "line2", None, None, None, "GB")
        val result: Future[Option[AddressDetails]] = testSubscriptionDataService.updateCorrespondenceAddressDetails(updatedDetails)

        await(result).isDefined must be(true)

        verify(mockSubscriptionDataAdapterService, times(1)).createUpdateCorrespondenceAddressRequest(any(), any())
        verify(mockSubscriptionDataAdapterService, times(1)).updateSubscriptionData(any())(any(), any())
        verify(mockDataCacheConnector, times(1)).clearCache()(any())
      }
    }

    "save registered details" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cachedOrgResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)),
        registrationDetails = Some(RegistrationBuilder.getEtmpRegistrationForOrganisation("testName")))

      "save the data when we have no cached data" in new Setup {
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDetailsService.updateOrganisationRegisteredDetails(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))
        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
        await(result).isDefined must be(false)

        verify(mockDetailsService, times(0)).updateOrganisationRegisteredDetails(any(), any())(any(), any())
        verify(mockDataCacheConnector, times(0)).clearCache()(any())
      }

      "save the data when we have no registered detailsl" in new Setup {
        val cachedOrgNoRegistered: CachedData = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cachedOrgNoRegistered)))

        when(mockDetailsService.updateOrganisationRegisteredDetails(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
        await(result).isDefined must be(false)

        verify(mockDetailsService, times(0)).updateOrganisationRegisteredDetails(any(), any())(any(), any())
        verify(mockDataCacheConnector, times(0)).clearCache()(any())
      }

      "save the data don't clear the cache if it was not successful" in new Setup {
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cachedOrgResponse)))

        when(mockDetailsService.updateOrganisationRegisteredDetails(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
        await(result).isDefined must be(false)

        verify(mockDetailsService, times(1)).updateOrganisationRegisteredDetails(any(), any())(any(), any())
        verify(mockDataCacheConnector, times(0)).clearCache()(any())
      }

      "save the data and clear the cache if it was successful" in new Setup {
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))
          (any(), any())).thenReturn(Future.successful(Some(cachedOrgResponse)))

        val updateDataResponse: UpdateRegistrationDetailsRequest = RegistrationBuilder.getEtmpRegistrationUpdateRequest("testResonse")
        when(mockDetailsService.updateOrganisationRegisteredDetails(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(updateDataResponse)))
        when(mockDataCacheConnector.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK, "")))

        val result: Future[Option[RegisteredDetails]] = testSubscriptionDataService.updateRegisteredDetails(registrationDetails.registeredDetails)
        await(result).isDefined must be(true)

        verify(mockDetailsService, times(1)).updateOrganisationRegisteredDetails(any(), any())(any(), any())
        verify(mockDataCacheConnector, times(1)).clearCache()(any())
      }
    }

    "updateOverseasCompanyRegistration" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cachedOrgNoRegistered = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

      "not update when can't retrieve data" in new Setup {
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cachedOrgNoRegistered)))

        val updateDataResponse: UpdateRegistrationDetailsRequest = RegistrationBuilder.getEtmpRegistrationUpdateRequest("testResonse")

        when(mockDetailsService.updateOverseasCompanyRegistration(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(updateDataResponse)))

        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val result: Future[Option[Identification]] = testSubscriptionDataService.updateOverseasCompanyRegistration(registrationDetails.nonUKIdentification.get)
        await(result).isDefined must be(false)

        verify(mockDetailsService, times(0)).updateOverseasCompanyRegistration(any(), any())(any(), any())
        verify(mockDataCacheConnector, times(0)).clearCache()(any())
      }
    }

    "updateOverseasCompanyRegistration" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cachedOrgResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)),
        registrationDetails = Some(RegistrationBuilder.getEtmpRegistrationForOrganisation("testName")))

      "save the data when we have no cached data" in new Setup {
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cachedOrgResponse)))

        val updateDataResponse: UpdateRegistrationDetailsRequest = RegistrationBuilder.getEtmpRegistrationUpdateRequest("testResonse")

        when(mockDetailsService.updateOverseasCompanyRegistration(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(updateDataResponse)))

        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val result: Future[Option[Identification]] = testSubscriptionDataService.updateOverseasCompanyRegistration(registrationDetails.nonUKIdentification.get)
        await(result).isDefined must be(true)

        verify(mockDetailsService, times(1)).updateOverseasCompanyRegistration(any(), any())(any(), any())
        verify(mockDataCacheConnector, times(1)).clearCache()(any())
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
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        when(mockSubscriptionDataAdapterService.getOrganisationName(any()))
          .thenReturn(Some("BusinessName"))

        val response: Future[Option[String]] = testSubscriptionDataService.getOrganisationName

        await(response) must be(Some("BusinessName"))
        verify(mockSubscriptionDataAdapterService, times(1)).getOrganisationName(eqTo(cacheDataResponse.registrationDetails))
      }
    }

    "getSafeId" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

      "retrieve cached data and return safe id" in new Setup {
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        when(mockSubscriptionDataAdapterService.getSafeId(any()))
          .thenReturn(Some(cacheDataResponse.subscriptionData.safeId))

        val response: Future[Option[String]] = testSubscriptionDataService.getSafeId

        await(response) must be(Some(cacheDataResponse.subscriptionData.safeId))
        verify(mockSubscriptionDataAdapterService, times(1)).getSafeId(eqTo(Some(cacheDataResponse.subscriptionData)))
      }
    }

    "getOverseasCompanyRegistration" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      "returns info from cache" in new Setup {
        when(mockDataCacheConnector.fetchAndGetData[CachedData](any())
          (any(), any())).thenReturn(Future.successful(None))

        val successSubscriptionData: SubscriptionData = SubscriptionData("", "",
          address = Seq(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)), emailConsent = Some(true))

        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(any(), any()))
          .thenReturn(Future.successful(Some(successSubscriptionData)))

        when(mockDetailsService.getRegisteredDetailsFromSafeId(any())(any(), any()))
          .thenReturn(Future.successful(Some(registrationDetails)))

        val response: Future[Option[Identification]] = testSubscriptionDataService.getOverseasCompanyRegistration
        await(response).get.idNumber must be("AAAAAAAA")
      }
    }

    "email consent" must {

      "return true, if emailConsent is true in cached data" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val cacheDataResponse: CachedData = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        val response: Future[Boolean] = testSubscriptionDataService.getEmailConsent
        await(response) must be(true)
      }

      "return false, if no cached data is found" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(None))

        val successSubscriptionData: Option[SubscriptionData] = None

        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(any(), any()))
          .thenReturn(Future.successful(successSubscriptionData))

        when(mockDetailsService.getRegisteredDetailsFromSafeId(any())(any(), any()))
          .thenReturn(Future.successful(None))

        val response: Future[Boolean] = testSubscriptionDataService.getEmailConsent
        await(response) must be(false)
      }
    }

    "get email with consent" must {
      "return None, if no cached data is found" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))
          (any(), any())).thenReturn(Future.successful(None))

        val successSubscriptionData: Option[SubscriptionData] = None

        when(mockSubscriptionDataAdapterService.retrieveSubscriptionData(any(), any()))
          .thenReturn(Future.successful(successSubscriptionData))

        when(mockDetailsService.getRegisteredDetailsFromSafeId(any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(any()))
          .thenReturn(None)

        val result: Option[EditContactDetailsEmail] = await(testSubscriptionDataService.getEmailWithConsent)
        result.isDefined must be(false)
      }

      "return None, if we have cached data but no address" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val cacheDataResponse: CachedData = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(any()))
          .thenReturn(None)

        val result: Option[EditContactDetailsEmail] = await(testSubscriptionDataService.getEmailWithConsent)
        result.isDefined must be(false)
      }


      "return EmailWithConsent, if we have one" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val cacheDataResponse: CachedData = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        val successAddress: Option[Address] =
          Some(Address(Some("name1"), Some("name2"), addressDetails = addressDetails, contactDetails = Some(ContactDetails(emailAddress = Some("a@b.c")))))

        when(mockSubscriptionDataAdapterService.getCorrespondenceAddress(any()))
          .thenReturn(successAddress)

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
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        when(mockSubscriptionDataAdapterService.createEditEmailWithConsentRequest(any(), any()))
          .thenReturn(None)

        val editDetails: EditContactDetailsEmail = EditContactDetailsEmail(emailAddress = "aa@mail.com", emailConsent = true)
        val result: Future[Option[EditContactDetailsEmail]] = testSubscriptionDataService.editEmailWithConsent(editDetails)

        await(result).isDefined must be(false)

        verify(mockSubscriptionDataAdapterService, times(1)).createEditEmailWithConsentRequest(any(), any())
        verify(mockDataCacheConnector, times(0)).clearCache()(any())
      }

      "save the data and clear the cache if it was successful" in new Setup {
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        val updateRequest: UpdateSubscriptionDataRequest = UpdateSubscriptionDataRequest(emailConsent = true, ChangeIndicators(), Nil)

        when(mockSubscriptionDataAdapterService.createEditEmailWithConsentRequest(any(), any()))
          .thenReturn(Some(updateRequest))

        when(mockSubscriptionDataAdapterService.updateSubscriptionData(any())(any(), any()))
          .thenReturn(Future.successful(Some(updateRequest)))

        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val editDetails: EditContactDetailsEmail = EditContactDetailsEmail(emailAddress = "aa@mail.com", emailConsent = true)

        val result: Future[Option[EditContactDetailsEmail]] = testSubscriptionDataService.editEmailWithConsent(editDetails)

        await(result).isDefined must be(true)

        verify(mockSubscriptionDataAdapterService, times(1)).createEditEmailWithConsentRequest(any(), any())
        verify(mockSubscriptionDataAdapterService, times(1)).updateSubscriptionData(any())(any(), any())
        verify(mockDataCacheConnector, times(1)).clearCache()(any())
      }
    }

    "edit contact details" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val cacheDataResponse = CachedData(SubscriptionData("XA0001234567899", "BusinessName", address = List(), emailConsent = Some(true)))

      "return None we have no data to edit" in new Setup {
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        when(mockSubscriptionDataAdapterService.createEditContactDetailsRequest(any(), any()))
          .thenReturn(None)

        val editDetails: EditContactDetails = EditContactDetails("name1", "name2", phoneNumber = "123456779")
        val result: Future[Option[EditContactDetails]] = testSubscriptionDataService.editContactDetails(editDetails)

        await(result).isDefined must be(false)

        verify(mockSubscriptionDataAdapterService, times(1)).createEditContactDetailsRequest(any(), any())
        verify(mockDataCacheConnector, times(0)).clearCache()(any())
      }

      "save the data and clear the cache if it was successful" in new Setup {
        when(mockDataCacheConnector.fetchAndGetData[CachedData](eqTo(RetrieveSubscriptionDataId))(any(), any()))
          .thenReturn(Future.successful(Some(cacheDataResponse)))

        val updateRequest: UpdateSubscriptionDataRequest = UpdateSubscriptionDataRequest(emailConsent = true, ChangeIndicators(), Nil)

        when(mockSubscriptionDataAdapterService.createEditContactDetailsRequest(any(), any()))
          .thenReturn(Some(updateRequest))

        when(mockSubscriptionDataAdapterService.updateSubscriptionData(any())(any(), any()))
          .thenReturn(Future.successful(Some(updateRequest)))

        when(mockDataCacheConnector.clearCache()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val editDetails: EditContactDetails = EditContactDetails(firstName = "TestFirstName", lastName = "TestLastName", phoneNumber = "123456779")
        val result: Future[Option[EditContactDetails]] = testSubscriptionDataService.editContactDetails(editDetails)

        await(result).isDefined must be(true)

        verify(mockSubscriptionDataAdapterService, times(1)).createEditContactDetailsRequest(any(), any())
        verify(mockSubscriptionDataAdapterService, times(1)).updateSubscriptionData(any())(any(), any())
        verify(mockDataCacheConnector, times(1)).clearCache()(any())
      }
    }
  }
}
