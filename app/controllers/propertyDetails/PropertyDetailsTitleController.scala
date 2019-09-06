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
import controllers.editLiability.EditLiabilityHasValueChangedController
import forms.PropertyDetailsForms._
import models.PropertyDetailsTitle
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services._
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.Future

trait PropertyDetailsTitleController extends PropertyDetailsHelpers with ClientHelper with AuthAction {

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                val displayData = propertyDetails.title.getOrElse(new PropertyDetailsTitle(""))
                Ok(views.html.propertyDetails.propertyDetailsTitle(id, propertyDetails.periodKey, propertyDetailsTitleForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  backLink))
              }
            }
        }
      }
    }
  }


  def editFromSummary(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      propertyDetailsCacheResponse(id) {
        case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
          dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
            val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
            Future.successful(
              Ok(views.html.propertyDetails.propertyDetailsTitle(
                id,
                propertyDetails.periodKey,
                propertyDetailsTitleForm.fill(propertyDetails.title.getOrElse(PropertyDetailsTitle(""))),
                mode,
                AtedUtils.getSummaryBackLink(id, None))
              )
            )
          }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsTitleForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.map(backLink => BadRequest(views.html.propertyDetails.propertyDetailsTitle(id, periodKey, formWithError, mode, backLink)))
          },
          propertyDetails => {
            val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsTitleController.view(id).url)
            for {
              savedData <- propertyDetailsService.saveDraftPropertyDetailsTitle(id, propertyDetails)
              result <-
              if (AtedUtils.isEditSubmittedMode(mode)) {
                RedirectWithBackLink(
                  EditLiabilityHasValueChangedController.controllerId,
                  controllers.editLiability.routes.EditLiabilityHasValueChangedController.view(id),
                  backLink)
            } else {
                RedirectWithBackLink(
                  PropertyDetailsOwnedBeforeController.controllerId,
                  controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(id),
                  backLink)
              }
            } yield result
          }
        )
      }
    }
  }
}

object PropertyDetailsTitleController extends PropertyDetailsTitleController {
  val delegationService: DelegationService = DelegationService
  val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId: String = "PropertyDetailsTitleController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
