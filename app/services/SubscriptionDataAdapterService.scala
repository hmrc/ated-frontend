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

import connectors.AtedConnector
import models._
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, InternalServerException}
import utils.AtedConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SubscriptionDataAdapterService {

  def atedConnector: AtedConnector

  def retrieveSubscriptionData(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[SubscriptionData]] = {
    atedConnector.retrieveSubscriptionData() map { response =>
      response.status match {
        case OK => Some(response.json.as[SubscriptionData])
        case NOT_FOUND => None
        case BAD_REQUEST =>
          Logger.warn(s"[SubscriptionDataService] [retrieveSubscriptionData] BadRequestException: [response.body] = ${response.body}")
          throw new BadRequestException(s"[SubscriptionDataService] [retrieveSubscriptionData] " +
            s"Bad Request: Failed to retrieve Subscription Data [response.body] = ${response.body}")
        case status =>
          Logger.warn(s"[SubscriptionDataService] [retrieveSubscriptionData] [status] = $status && [response.body] = ${response.body}")
          throw new InternalServerException(s"[SubscriptionDataService] [retrieveSubscriptionData]" +
            s"Internal Server Exception : Failed to retrieve Subscription Data [status] = $status && [response.body] = ${response.body}")
      }
    }
  }

  def updateSubscriptionData(request: UpdateSubscriptionDataRequest)
                            (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[UpdateSubscriptionDataRequest]] = {
    atedConnector.updateSubscriptionData(request).map { response =>
      response.status match {
        case OK => Some(request)
        case status =>
          Logger.warn(s"[SubscriptionDataService] [updateSubscriptionData] [status] = $status && [response.body] = ${response.body}")
          None
      }
    }
  }

  def createEditEmailWithConsentRequest(oldData: SubscriptionData, editedContactDetails: EditContactDetailsEmail): Option[UpdateSubscriptionDataRequest] = {
    getCorrespondenceAddress(Some(oldData)).map { foundCorrespondence =>
      val editedContactDetail = foundCorrespondence.contactDetails.map(_.copy(emailAddress = Some(editedContactDetails.emailAddress)))
      val updatedAddress = foundCorrespondence.copy(contactDetails = editedContactDetail)
      val filteredAddresses = oldData.address.filterNot(_.addressDetails.addressType == AtedConstants.AddressTypeCorrespondence)
      val emailConsent = editedContactDetails.emailConsent
      new UpdateSubscriptionDataRequest(emailConsent = emailConsent, changeIndicators = ChangeIndicators(nameChanged = false,contactDetailsChanged = true),
        address = filteredAddresses :+ updatedAddress)
    }
  }

  def createEditContactDetailsRequest(oldData: SubscriptionData, editedContactDetails: EditContactDetails): Option[UpdateSubscriptionDataRequest] = {
    getCorrespondenceAddress(Some(oldData)).map { foundCorrespondence =>
      val editedContactDetail = foundCorrespondence.contactDetails.map(_.copy(phoneNumber = Some(editedContactDetails.phoneNumber)))
      val postCode = foundCorrespondence.addressDetails.postalCode match {
        case Some(pc) if(pc == "") => None
        case Some(pc) => Some(pc)
        case None => None
      }
      val editedCorrespondenceDetails = foundCorrespondence.addressDetails.copy(postalCode = postCode)
      val updatedAddress = foundCorrespondence.copy(name1 = Some(editedContactDetails.firstName),
                                                    name2 = Some(editedContactDetails.lastName),
                                                    addressDetails = editedCorrespondenceDetails,
                                                    contactDetails = editedContactDetail)
      val filteredAddresses = oldData.address.filterNot(_.addressDetails.addressType == AtedConstants.AddressTypeCorrespondence)
      val emailConsent = oldData.emailConsent.getOrElse(false)
      new UpdateSubscriptionDataRequest(emailConsent = emailConsent, changeIndicators = ChangeIndicators(nameChanged = true, contactDetailsChanged = true),
        address = filteredAddresses :+ updatedAddress)
    }
  }

  def createUpdateCorrespondenceAddressRequest(oldData: SubscriptionData, updatedAddressDetails: AddressDetails): Option[UpdateSubscriptionDataRequest] = {
    getCorrespondenceAddress(Some(oldData)).map { foundCorrespondence =>
      val updatedAddress = foundCorrespondence.copy(addressDetails = updatedAddressDetails)
      val filteredAddresses = oldData.address.filterNot(_.addressDetails.addressType == AtedConstants.AddressTypeCorrespondence)
      val emailConsent = oldData.emailConsent.getOrElse(false)
      new UpdateSubscriptionDataRequest(emailConsent = emailConsent, changeIndicators = ChangeIndicators(correspondenceChanged = true), address =
        filteredAddresses :+ updatedAddress)
    }
  }

  def getCorrespondenceAddress(subscriptionData: Option[SubscriptionData]): Option[Address] = {
    subscriptionData.flatMap(_.address.find(_.addressDetails.addressType == AtedConstants.AddressTypeCorrespondence))
  }

  def getOrganisationName(etmpRegDetails: Option[EtmpRegistrationDetails]): Option[String] = etmpRegDetails.flatMap(_.name)

  def getSafeId(subscriptionData: Option[SubscriptionData]): Option[String] =  subscriptionData.map(_.safeId)

}

object SubscriptionDataAdapterService extends SubscriptionDataAdapterService {
  val atedConnector = AtedConnector
}
