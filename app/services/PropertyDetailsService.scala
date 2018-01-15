/*
 * Copyright 2018 HM Revenue & Customs
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
import models._
import org.joda.time.LocalDate
import play.api.Logger
import play.api.http.Status._
import play.mvc.Http.Status.OK
import utils.AtedConstants.SubmitReturnsResponseFormId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, InternalServerException }

sealed trait PropertyDetailsCacheResponse

case class PropertyDetailsCacheSuccessResponse(propertyDetails: PropertyDetails) extends PropertyDetailsCacheResponse

case object PropertyDetailsCacheNotFoundResponse extends PropertyDetailsCacheResponse

case object PropertyDetailsCacheErrorResponse extends PropertyDetailsCacheResponse

trait PropertyDetailsService {

  def atedConnector: PropertyDetailsConnector

  def dataCacheConnector: DataCacheConnector

  def createDraftPropertyDetailsAddress(periodKey: Int, propertyDetailsAddress: PropertyDetailsAddress)
                                       (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[String] = {
    for {
      propertyDetailsResponse <- atedConnector.createDraftPropertyDetails(periodKey, propertyDetailsAddress)
    } yield {
      propertyDetailsResponse.status match {
        case OK => propertyDetailsResponse.json.as[PropertyDetails].id
        case status =>
          Logger.warn(s"[PropertyDetailsService][createDraftPropertyDetails] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[createDraftPropertyDetails] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsAddress(id: String, propertyDetailsAddress: PropertyDetailsAddress)
                                     (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[String] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsAddressRef(id, propertyDetailsAddress)
    } yield {
      propertyDetailsResponse.status match {
        case OK => id
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsAddressRef] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsAddressRef] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsTitle(id: String, propertyDetails: PropertyDetailsTitle)
                                        (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    val trimmedPropertyDetails = propertyDetails.copy(titleNumber = propertyDetails.titleNumber.replaceAll(" ", ""))
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsTitle(id, trimmedPropertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsTitle] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsTitle] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftHasValueChanged(id: String, hasValueChanged: Boolean)
                              (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftHasValueChanged(id, hasValueChanged)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftHasValueChanged] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftHasValueChanged] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsAcquisition(id: String, overLimit: Boolean)
                                         (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsAcquisition(id, overLimit)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsAcquisition] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsAcquisition] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsOwnedBefore(id: String, updated: PropertyDetailsOwnedBefore)
                                         (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsOwnedBefore(id, updated)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsOwnedBefore] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsOwnedBefore] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsRevalued(id: String, revalued: PropertyDetailsRevalued)
                                      (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsRevalued(id, revalued)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsRevalued] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsRevalued] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsProfessionallyValued(id: String, updated: PropertyDetailsProfessionallyValued)
                                                  (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsProfessionallyValued(id, updated)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsProfessionallyValued] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsProfessionallyValued] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsNewBuild(id: String, updated: PropertyDetailsNewBuild)
                                                  (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsNewBuild(id, updated)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsNewBuild] " +
            s"Invalid status when saving Property Details - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService]" +
            s"[saveDraftPropertyDetailsNewBuild] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftIsFullTaxPeriod(id: String, isFullPeriod: IsFullTaxPeriod)
                               (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftIsFullTaxPeriod(id, isFullPeriod)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftIsFullTaxPeriod] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftIsFullTaxPeriod] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsSupportingInfo(id: String, propertyDetails: PropertyDetailsSupportingInfo)
                                           (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsSupportingInfo(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsSupportingInfo] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftPropertyDetailsSupportingInfo] Invalid status when saving Property Details :$status")
      }
    }
  }

  def calculateDraftChangeLiability(id: String)(implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[PropertyDetails] = {
        atedConnector.calculateDraftChangeLiability(id) map { propertyDetailsResponse =>
          propertyDetailsResponse.status match {
            case OK => propertyDetailsResponse.json.as[PropertyDetails]
            case status =>
              Logger.warn(s"[PropertyDetailsService][calculateDraftChangeLiability] Invalid status when calculating Property Details" +
                s" - status: $status , response.body : ${propertyDetailsResponse.body}")
              throw new InternalServerException(s"[PropertyDetailsService][calculateDraftChangeLiability] Invalid status when calculating Property Details :$status")
          }
        }
  }

  def calculateDraftPropertyDetails(id: String)(implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    validateCalculateDraftPropertyDetails(id).flatMap {
      case true => atedConnector.calculateDraftPropertyDetails(id )
      case false => Future.successful(HttpResponse(OK, None))
    }

  }

  def saveDraftPropertyDetailsInRelief(id: String, propertyDetails: PropertyDetailsInRelief)
                                      (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsInRelief(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsInRelief] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftPropertyDetailsInRelief] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsTaxAvoidance(id: String, propertyDetails: PropertyDetailsTaxAvoidance)
                                         (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsTaxAvoidance(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsTaxAvoidance] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftPropertyDetailsTaxAvoidance] Invalid status when saving Property Details :$status")
      }
    }
  }

  def saveDraftPropertyDetailsDatesLiable(id: String, propertyDetails: PropertyDetailsDatesLiable)
                                           (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.saveDraftPropertyDetailsDatesLiable(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][saveDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][saveDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details :$status")
      }
    }
  }

  def addDraftPropertyDetailsDatesLiable(id: String, propertyDetails: PropertyDetailsDatesLiable)
                                        (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      propertyDetailsResponse <- atedConnector.addDraftPropertyDetailsDatesLiable(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][addDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][addDraftPropertyDetailsDatesLiable] Invalid status when saving Property Details :$status")
      }
    }
  }


  val CHOSEN_RELIEF_ID = "PROPERTY-DETAILS-CHOSEN-RELIEF"
  def storeChosenRelief(chosenRelief: PeriodChooseRelief)(implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[PeriodChooseRelief] = {
    for {
      result <- dataCacheConnector.saveFormData[PeriodChooseRelief](CHOSEN_RELIEF_ID, chosenRelief)
    } yield {
      result
    }
  }

  def addDraftPropertyDetailsDatesInRelief(id: String, propertyDetails: PropertyDetailsDatesInRelief)
                                        (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[Int] = {
    for {
      chosenRelief <- dataCacheConnector.fetchAndGetFormData[PeriodChooseRelief](CHOSEN_RELIEF_ID)
      propertyDetailsResponse <- atedConnector.addDraftPropertyDetailsDatesInRelief(id, propertyDetails.copy(description = chosenRelief.map(_.reliefDescription)))
    } yield {
      propertyDetailsResponse.status match {
        case OK => OK
        case status =>
          Logger.warn(s"[PropertyDetailsService][addDraftPropertyDetailsDatesInRelief] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][addDraftPropertyDetailsDatesInRelief] Invalid status when saving Property Details :$status")
      }
    }
  }

  def deleteDraftPropertyDetailsPeriod(id: String, propertyDetails: LocalDate)
                                           (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[PropertyDetails] = {
    for {
      propertyDetailsResponse <- atedConnector.deleteDraftPropertyDetailsPeriod(id, propertyDetails)
    } yield {
      propertyDetailsResponse.status match {
        case OK => propertyDetailsResponse.json.as[PropertyDetails]
        case status =>
          Logger.warn(s"[PropertyDetailsService][deleteDraftPropertyDetailsPeriod] Invalid status when saving Property Details" +
            s" - status: $status , response.body : ${propertyDetailsResponse.body}")
          throw new InternalServerException(s"[PropertyDetailsService][deleteDraftPropertyDetailsPeriod] Invalid status when saving Property Details :$status")
      }
    }
  }

  def retrieveDraftPropertyDetails(id: String)
                                  (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[PropertyDetailsCacheResponse] = {
    for {
      propertyDetailsResponse <- atedConnector.retrieveDraftPropertyDetails(id)
    } yield {
      propertyDetailsResponse.status match {
        case OK =>
          PropertyDetailsCacheSuccessResponse(propertyDetailsResponse.json.as[PropertyDetails])
        case NOT_FOUND =>
          Logger.warn(s"[PropertyDetailsService][retrieveDraftPropertyDetails] NOT FOUND when retrieving Property Details" +
            s" - status = 404, response.body = ${propertyDetailsResponse.body}")
          PropertyDetailsCacheNotFoundResponse
        case status =>
          Logger.warn(s"[PropertyDetailsService][retrieveDraftPropertyDetails] Invalid status when retrieving Property Details" +
            s" - status = $status, response.body = ${propertyDetailsResponse.body}")
          PropertyDetailsCacheErrorResponse
      }
    }
  }

  def submitDraftPropertyDetails(id: String)
                                (implicit atedContext: AtedContext, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    for {
      httpResponse <- atedConnector.submitDraftPropertyDetails(id)
    } yield {
      dataCacheConnector.clearCache() flatMap { clearCacheResponse =>
        dataCacheConnector.saveFormData[SubmitReturnsResponse](formId = SubmitReturnsResponseFormId,
          data = httpResponse.json.as[SubmitReturnsResponse])}
      httpResponse
    }
  }

  def clearDraftReliefs(id: String)(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[HttpResponse] = atedConnector.deleteDraftChargeable(id)

  def validateCalculateDraftPropertyDetails(id : String)(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[Boolean] = {
    retrieveDraftPropertyDetails(id).map {
      case PropertyDetailsCacheSuccessResponse(propertDetailsDraft) =>
        propertDetailsDraft.value match {
          case Some(propVal) =>  propVal.isValuedByAgent.isDefined
          case None =>  false
        }
    }
  }
}

object PropertyDetailsService extends PropertyDetailsService {
  val atedConnector = PropertyDetailsConnector
  val dataCacheConnector = DataCacheConnector
}
