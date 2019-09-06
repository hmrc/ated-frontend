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

package controllers.propertyDetails

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import models.PropertyDetailsProfessionallyValued
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services._
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

trait PropertyDetailsProfessionallyValuedController extends PropertyDetailsHelpers with ClientHelper with AuthAction {

  def view(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                val displayData = PropertyDetailsProfessionallyValued(propertyDetails.value.flatMap(_.isValuedByAgent))
                Ok(views.html.propertyDetails.propertyDetailsProfessionallyValued(id,
                  propertyDetails.periodKey,
                  propertyDetailsProfessionallyValuedForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  backLink))
              }
            }
        }
      }
    }
  }

  def editFromSummary(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
              val displayData = PropertyDetailsProfessionallyValued(propertyDetails.value.flatMap(_.isValuedByAgent))
              Ok(views.html.propertyDetails.propertyDetailsProfessionallyValued(id,
                propertyDetails.periodKey,
                propertyDetailsProfessionallyValuedForm.fill(displayData),
                AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                AtedUtils.getSummaryBackLink(id, None)))
            }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsProfessionallyValuedForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.map(backLink => BadRequest(views.html.propertyDetails.propertyDetailsProfessionallyValued(id, periodKey, formWithError, mode, backLink)))
          },
          propertyDetails => {
            for {
              savedData <- propertyDetailsService.saveDraftPropertyDetailsProfessionallyValued(id, propertyDetails)
              result <-
              RedirectWithBackLink(
                PropertyDetailsAcquisitionController.controllerId,
                controllers.propertyDetails.routes.PropertyDetailsAcquisitionController.view(id),
                Some(controllers.propertyDetails.routes.PropertyDetailsProfessionallyValuedController.view(id).url)
              )
            } yield result
          }
        )
      }
    }
  }
}

object PropertyDetailsProfessionallyValuedController extends PropertyDetailsProfessionallyValuedController {
  val delegationService: DelegationService = DelegationService
  val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId: String = "PropertyDetailsProfessionallyValuedController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
