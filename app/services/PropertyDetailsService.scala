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

import connectors.{DataCacheConnector, PropertyDetailsConnector}

import javax.inject.Inject
import models._
import org.joda.time.LocalDate
import play.api.Logging
import play.api.http.Status._
import play.mvc.Http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import utils.AtedConstants.SubmitReturnsResponseFormId

import scala.concurrent.{ExecutionContext, Future}

sealed trait PropertyDetailsCacheResponse

case class PropertyDetailsCacheSuccessResponse(propertyDetails: PropertyDetails) extends PropertyDetailsCacheResponse

case object PropertyDetailsCacheNotFoundResponse extends PropertyDetailsCacheResponse

case object PropertyDetailsCacheErrorResponse extends PropertyDetailsCacheResponse

class PropertyDetailsService @Inject()(propertyDetailsConnector: PropertyDetailsConnector,
                                       dataCacheConnector: DataCacheConnector,
                                       ex: ExecutionContext) extends Logging {


  val CHOSEN_RELIEF_ID = "PROPERTY-DETAILS-CHOSEN-RELIEF"

  def createDraftPropertyDetailsAddress(periodKey: Int, propertyDetailsAddress: PropertyDetailsAddress)
                                       (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[String] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.createDraftPropertyDetails(periodKey, propertyDetailsAddress)
    } yield {
      propertyDetailsResponse.status match {
        case OK => propertyDetailsResponse.json.as[PropertyDetails].id
        case status =>
          logger.warn(s"[PropertyDetailsService][createDraftPropertyDetails] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[createDraftPropertyDetails] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsAddress(id: String, propertyDetailsAddress: PropertyDetailsAddress)
                                     (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[String] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsAddressRef(id, propertyDetailsAddress)
    } yield {
      propertyDetailsResponse.status match {
        case OK => id
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsAddressRef] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsAddressRef] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsTitle(id: String, propertyDetails: PropertyDetailsTitle)
                                   (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    val trimmedPropertyDetails = propertyDetails.copy(titleNumber = propertyDetails.titleNumber.replaceAll(" ", ""))
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsTitle(id, trimmedPropertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsTitle] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsTitle] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftHasValueChanged(id: String, hasValueChanged: Boolean)
                              (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftHasValueChanged(id, hasValueChanged)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftHasValueChanged] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftHasValueChanged] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsAcquisition(id: String, overLimit: Boolean)
                                         (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsAcquisition(id, overLimit)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsAcquisition] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsAcquisition] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsOwnedBefore(id: String, updated: PropertyDetailsOwnedBefore)
                                         (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsOwnedBefore(id, updated)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsOwnedBefore] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsOwnedBefore] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsRevalued(id: String, revalued: PropertyDetailsRevalued)
                                      (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsRevalued(id, revalued)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsRevalued] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsRevalued] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsProfessionallyValued(id: String, updated: PropertyDetailsProfessionallyValued)
                                                  (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsProfessionallyValued(id, updated)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsProfessionallyValued] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsProfessionallyValued] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsNewBuild(id: String, updated: PropertyDetailsNewBuild)
                                      (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsNewBuild(id, updated)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsNewBuild] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsNewBuild] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsNewBuildDates(id: String, updated: PropertyDetailsNewBuildDates)
                                           (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {

    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailNewBuildDates(id, updated)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsNewBuildDates] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsNewBuildDates] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsWhenAcquiredDates(id: String, updated: PropertyDetailsWhenAcquiredDates)
                                               (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {

    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsWhenAcquiredDates(id, updated)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsWhenAcquiredDates] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsWhenAcquiredDates] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsNewBuildValue(id: String, updated: PropertyDetailsNewBuildValue)
                                           (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsNewBuildValue(id, updated)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsNewBuildValue] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsNewBuildValue] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsValueAcquired(id: String, updated: PropertyDetailsValueOnAcquisition)
                                           (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {

    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsValueAcquired(id, updated)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsValueAcquired] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsValueAcquired] Invalid status when saving Property Details :$status")
      }
    }

  }

  def saveDraftIsFullTaxPeriod(id: String, isFullPeriod: IsFullTaxPeriod)
                              (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftIsFullTaxPeriod(id, isFullPeriod)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftIsFullTaxPeriod] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftIsFullTaxPeriod] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsSupportingInfo(id: String, propertyDetails: PropertyDetailsSupportingInfo)
                                            (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsSupportingInfo(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsSupportingInfo] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftPropertyDetailsSupportingInfo]" +
            s" Invalid status when saving Property Details :$status")
      }
    }
  }

  def calculateDraftChangeLiability(id: String)(implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Option[PropertyDetails]] = {

    validateCalculateDraftPropertyDetails(id, isChangeLiability = true).flatMap {
      case true =>
      propertyDetailsConnector.calculateDraftChangeLiability(id) map { propertyDetailsResponse =>
        (propertyDetailsResponse.status: @unchecked) match {
          case OK => Some(propertyDetailsResponse.json.as[PropertyDetails])
          case NO_CONTENT =>
            logger.info("[PropertyDetailsService][calculateDraftChangeLiability] " +
              "Return details incomplete - redirecting to summary without calc")
            None
        }
      }
      case false => Future.successful(None)

    }
  }

  def calculateDraftPropertyDetails(id: String)(implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    validateCalculateDraftPropertyDetails(id, isChangeLiability = false).flatMap {
      case true => propertyDetailsConnector.calculateDraftPropertyDetails(id)
      case false => Future.successful(HttpResponse(OK, ""))
    }
  }

  private def checkOwnedBefore(value: PropertyDetailsValue): Boolean = {
    val ownedBefore = PropertyDetailsOwnedBefore(value.isOwnedBeforePolicyYear, value.ownedBeforePolicyYearValue)
    (ownedBefore.isOwnedBeforePolicyYear, value.isNewBuild, value.isPropertyRevalued) match {
      case (Some(true), _, _) => ownedBefore.ownedBeforePolicyYearValue.isDefined
      case (Some(false), Some(true), _) => value.newBuildValue.isDefined
      case (Some(false), Some(false), _) => value.notNewBuildValue.isDefined
      case (_, _, Some(_)) => value.revaluedValue.isDefined
      case _ => false
    }
  }

  def validateCalculateDraftPropertyDetails(id: String, isChangeLiability: Boolean)
                                           (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Boolean] = {
    retrieveDraftPropertyDetails(id).map {
      case PropertyDetailsCacheSuccessResponse(propertyDetailsDraft) =>
        propertyDetailsDraft.value match {
          case Some(value) =>
            if (isChangeLiability && value.hasValueChanged.isEmpty) {
              false
            } else {
              checkOwnedBefore(value)
            }
          case None => false
        }
      case _ => false
    }
  }

  def retrieveDraftPropertyDetails(id: String)
                                  (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[PropertyDetailsCacheResponse] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.retrieveDraftPropertyDetails(id)
    } yield {
      propertyDetailsResponse.status match {
        case OK =>
          PropertyDetailsCacheSuccessResponse(propertyDetailsResponse.json.as[PropertyDetails])
        case NOT_FOUND =>
          logger.warn(s"[PropertyDetailsService][retrieveDraftPropertyDetails] NOT FOUND when retrieving Property Details" +
            s" - status = 404, response.body = ${propertyDetailsResponse.body}")
          PropertyDetailsCacheNotFoundResponse
        case status =>
          logger.warn(s"[PropertyDetailsService][retrieveDraftPropertyDetails] Invalid status when retrieving Property Details" +
            s" - status = $status, response.body = ${propertyDetailsResponse.body}")
          PropertyDetailsCacheErrorResponse
      }
    }
  }

  def saveDraftPropertyDetailsInRelief(id: String, propertyDetails: PropertyDetailsInRelief)
                                      (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsInRelief(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsInRelief] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftPropertyDetailsInRelief] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsTaxAvoidance(id: String, propertyDetails: PropertyDetailsTaxAvoidance)
                                          (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsTaxAvoidance(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsTaxAvoidance] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftPropertyDetailsTaxAvoidance] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsDatesLiable(id: String, propertyDetails: PropertyDetailsDatesLiable)
                                         (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.saveDraftPropertyDetailsDatesLiable(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details :$status")
      }
    }
  }

  def addDraftPropertyDetailsDatesLiable(id: String, propertyDetails: PropertyDetailsDatesLiable)
                                        (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.addDraftPropertyDetailsDatesLiable(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][addDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][addDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details :$status")
      }
    }
  }

  def storeChosenRelief(chosenRelief: PeriodChooseRelief)
                       (implicit headerCarrier: HeaderCarrier): Future[PeriodChooseRelief] = {
    for {
      result <- dataCacheConnector.saveFormData[PeriodChooseRelief](CHOSEN_RELIEF_ID, chosenRelief)
    } yield {
      result
    }
  }

  def addDraftPropertyDetailsDatesInRelief(id: String, propertyDetails: PropertyDetailsDatesInRelief)
                                          (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      chosenRelief <- dataCacheConnector.fetchAndGetFormData[PeriodChooseRelief](CHOSEN_RELIEF_ID)
      propertyDetailsResponse <- propertyDetailsConnector.addDraftPropertyDetailsDatesInRelief(id, propertyDetails.copy(description = chosenRelief.map(_.reliefDescription)))
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          logger.warn(s"[PropertyDetailsService][addDraftPropertyDetailsDatesInRelief] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][addDraftPropertyDetailsDatesInRelief] Invalid status when saving Property Details :$status")
      }
    }
  }

  def deleteDraftPropertyDetailsPeriod(id: String, propertyDetails: LocalDate)
                                      (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[PropertyDetails] = {
    for {
      propertyDetailsResponse <- propertyDetailsConnector.deleteDraftPropertyDetailsPeriod(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => propertyDetailsResponse.json.as[PropertyDetails]
        case status =>
          logger.warn(s"[PropertyDetailsService][deleteDraftPropertyDetailsPeriod] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][deleteDraftPropertyDetailsPeriod] Invalid status when saving Property Details :$status")
      }
    }
  }

  def submitDraftPropertyDetails(id: String)
                                (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    for {
      httpResponse <- propertyDetailsConnector.submitDraftPropertyDetails(id)
      _ <- dataCacheConnector.clearCache()
      _ <- dataCacheConnector.saveFormData[SubmitReturnsResponse](formId = SubmitReturnsResponseFormId, data = httpResponse.json.as[SubmitReturnsResponse])
    } yield {
      httpResponse
    }
  }

  def clearDraftReliefs(id: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] =
    propertyDetailsConnector.deleteDraftChargeable(id)
}
