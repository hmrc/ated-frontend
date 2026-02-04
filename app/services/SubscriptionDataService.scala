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

import javax.inject.Inject
import models._
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants._

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionDataService @Inject()(dataCacheService: DataCacheService,
                                        subscriptionDataAdapterService: SubscriptionDataAdapterService,
                                        detailsService: DetailsService)
                                       (implicit ec: ExecutionContext){

  private def retrieveCachedData(implicit hc: HeaderCarrier): Future[Option[CachedData]] = {
    dataCacheService.fetchAndGetData[CachedData](RetrieveSubscriptionDataId)
  }

  private def retrieveAndCacheData(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[CachedData]] = {
    for {
      cachedReturns <- retrieveCachedData
      etmpOrCachedData <- cachedReturns match {
        case Some(x) => Future.successful(Some(x))
        case None => retrieveDataToCache
      }
    } yield {
      etmpOrCachedData
    }
  }

  private def retrieveDataToCache(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[CachedData]] = {
    for {
      subscriptionData <- subscriptionDataAdapterService.retrieveSubscriptionData
      registrationDetails <- subscriptionData match {
        case Some(x) => detailsService.getRegisteredDetailsFromSafeId(x.safeId)
        case _ => Future.successful(None)
      }
    } yield {
      subscriptionData.map { data =>
        val dataToCache = CachedData(data, registrationDetails)
        dataCacheService.saveFormData[CachedData](RetrieveSubscriptionDataId, dataToCache)
        dataToCache
      }
    }
  }

  def getCorrespondenceAddress(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[Address]] = {
    retrieveAndCacheData.map { cachedData =>
      subscriptionDataAdapterService.getCorrespondenceAddress(cachedData.map(_.subscriptionData))
    }
  }

  def getOrganisationName(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[String]] = {
    retrieveAndCacheData.map { cachedData =>
      subscriptionDataAdapterService.getOrganisationName(cachedData.flatMap(_.registrationDetails))
    }
  }

  def getSafeId(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[String]] = {
    retrieveAndCacheData.map { cachedData =>
      subscriptionDataAdapterService.getSafeId(cachedData.map(_.subscriptionData))
    }
  }

  def updateCorrespondenceAddressDetails(updatedAddressDetails: AddressDetails)
                                        (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[AddressDetails]] = {
    def performCorrespondenceAddressUpdate(cachedData: CachedData, updatedData: AddressDetails): Future[Option[Any]] = {
      subscriptionDataAdapterService.createUpdateCorrespondenceAddressRequest(cachedData.subscriptionData, updatedData) match {
        case Some(x) => subscriptionDataAdapterService.updateSubscriptionData(x)
        case None => Future.successful(None)
      }
    }

    updateDetails[AddressDetails](performCorrespondenceAddressUpdate)(updatedAddressDetails)
  }

  def getRegisteredDetails(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[RegisteredDetails]] = {
    retrieveAndCacheData.map(_.flatMap(_.registrationDetails.map(_.registeredDetails)))
  }

  def getOverseasCompanyRegistration(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[Identification]] = {
    retrieveAndCacheData.map(_.flatMap(_.registrationDetails.flatMap(_.nonUKIdentification)))
  }

  def updateRegisteredDetails(updatedRegisteredDetails: RegisteredDetails)
                             (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[RegisteredDetails]] = {
    def performRegisteredDetailsUpdate(cachedData: CachedData, updatedData: RegisteredDetails): Future[Option[Any]] = {
      cachedData.registrationDetails match {
        case Some(x) => detailsService.updateOrganisationRegisteredDetails(x, updatedData)
        case None => Future.successful(None)
      }
    }

    updateDetails[RegisteredDetails](performRegisteredDetailsUpdate)(updatedRegisteredDetails)
  }

  def updateOverseasCompanyRegistration(updatedDetails: Identification)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[Identification]] = {
    def performUpdate(cachedData: CachedData, updatedDetails: Identification): Future[Option[Any]] = {
      cachedData.registrationDetails match {
        case Some(x) => detailsService.updateOverseasCompanyRegistration(x, Some(updatedDetails))
        case None => Future.successful(None)
      }
    }

    updateDetails[Identification](performUpdate)(updatedDetails)
  }

  private def updateDetails[T](performUpdate: (CachedData, T) => Future[Option[Any]])(updatedData: T)
                              (implicit hc: HeaderCarrier): Future[Option[T]] = {
    for {
      cachedData <- retrieveCachedData
      updatedDataResponse <- {
        cachedData match {
          case Some(x) => performUpdate(x, updatedData)
          case None => Future.successful(None)
        }
      }
      _ <- updatedDataResponse match {
        case Some(x) => dataCacheService.clearCache().flatMap(r => Future.successful(r))
        case None => Future.successful(None)
      }
    } yield {
      updatedDataResponse.map(x => updatedData)
    }
  }

  def getEmailConsent(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Boolean] = {
    retrieveAndCacheData.map { cachedData =>
      cachedData.fold(false)(a => a.subscriptionData.emailConsent.getOrElse(false))
    }
  }

  def getEmailWithConsent(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[EditContactDetailsEmail]] = {
    for {
      address <- getCorrespondenceAddress
      emailConsent <- getEmailConsent
    } yield {
      address.map { x =>
        EditContactDetailsEmail(
          emailAddress = x.contactDetails.flatMap(_.emailAddress).getOrElse(""),
          emailConsent = emailConsent)
      }
    }
  }

  def editEmailWithConsent(editEmailWithConsent: EditContactDetailsEmail)
                          (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[EditContactDetailsEmail]] = {
    def performEmailWithConsentUpdate(cachedData: CachedData, editedData: EditContactDetailsEmail): Future[Option[Any]] = {
      subscriptionDataAdapterService.createEditEmailWithConsentRequest(cachedData.subscriptionData, editedData) match {
        case Some(x) => subscriptionDataAdapterService.updateSubscriptionData(x)
        case None => Future.successful(None)
      }
    }

    updateDetails[EditContactDetailsEmail](performEmailWithConsentUpdate)(editEmailWithConsent)
  }

  def editContactDetails(editContactDetails: EditContactDetails)
                        (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[EditContactDetails]] = {
    def performContactDetailsUpdate(cachedData: CachedData, editedData: EditContactDetails): Future[Option[Any]] = {
      subscriptionDataAdapterService.createEditContactDetailsRequest(cachedData.subscriptionData, editedData) match {
        case Some(x) => subscriptionDataAdapterService.updateSubscriptionData(x)
        case None => Future.successful(None)
      }
    }

    updateDetails[EditContactDetails](performContactDetailsUpdate)(editContactDetails)
  }
}
