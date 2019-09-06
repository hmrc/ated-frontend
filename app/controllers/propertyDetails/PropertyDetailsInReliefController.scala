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
import controllers.editLiability.EditLiabilityDatesLiableController
import forms.PropertyDetailsForms._
import models._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

trait PropertyDetailsInReliefController extends PropertyDetailsHelpers with ClientHelper with AuthAction {

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val filledForm = periodsInAndOutReliefForm.fill(PropertyDetailsInRelief(propertyDetails.period.flatMap(_.isInRelief)))
            currentBackLink.flatMap(backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                Ok(views.html.propertyDetails.propertyDetailsInRelief(id, propertyDetails.periodKey, filledForm,
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn), backLink)
                )
              }
            )
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        periodsInAndOutReliefForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.map(backLink =>
              BadRequest(views.html.propertyDetails.propertyDetailsInRelief(id, periodKey, formWithError, mode, backLink))
            )
          },
          propertyDetails => {
            val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsInReliefController.view(id).url)
            for {
              savedData <- propertyDetailsService.saveDraftPropertyDetailsInRelief(id, propertyDetails)
              result <-
              (propertyDetails.isInRelief.getOrElse(false), AtedUtils.isEditSubmittedMode(mode)) match {
                case (true, _) =>
                  RedirectWithBackLink(PeriodsInAndOutReliefController.controllerId,
                    controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id),
                    backLink
                  )
                case (false, false) =>
                  RedirectWithBackLink(PeriodDatesLiableController.controllerId,
                    controllers.propertyDetails.routes.PeriodDatesLiableController.view(id),
                    backLink
                  )
                case (false, true) =>
                  RedirectWithBackLink(EditLiabilityDatesLiableController.controllerId,
                    controllers.editLiability.routes.EditLiabilityDatesLiableController.view(id),
                    backLink
                  )
              }
            } yield result
          }
        )
      }
    }
  }
}

object PropertyDetailsInReliefController extends PropertyDetailsInReliefController {
  val delegationService: DelegationService = DelegationService
  val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsInReliefController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
