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
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import models.PropertyDetailsNewBuild
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services._
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.Future

trait PropertyDetailsNewBuildController extends PropertyDetailsHelpers with ClientHelper with AuthAction {

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                val displayData = PropertyDetailsNewBuild(propertyDetails.value.flatMap(_.isNewBuild),
                  newBuildValue = propertyDetails.value.flatMap(_.newBuildValue),
                  newBuildDate = propertyDetails.value.flatMap(_.newBuildDate),
                  localAuthRegDate = propertyDetails.value.flatMap(_.localAuthRegDate),
                  notNewBuildDate = propertyDetails.value.flatMap(_.notNewBuildDate),
                  notNewBuildValue = propertyDetails.value.flatMap(_.notNewBuildValue)
                )
                Future.successful(Ok(views.html.propertyDetails.propertyDetailsNewBuild(id,
                  propertyDetails.periodKey,
                  propertyDetailsNewBuildForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  backLink)
                ))
              }
            }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey, propertyDetailsNewBuildForm.bindFromRequest).fold(
          formWithError => {
            currentBackLink.map(backLink =>
              BadRequest(views.html.propertyDetails.propertyDetailsNewBuild(id, periodKey, formWithError, mode, backLink))
            )
          },
          propertyDetails => {
            for {
              savedData <- propertyDetailsService.saveDraftPropertyDetailsNewBuild(id, propertyDetails)
              result <-
              RedirectWithBackLink(
                PropertyDetailsProfessionallyValuedController.controllerId,
                controllers.propertyDetails.routes.PropertyDetailsProfessionallyValuedController.view(id),
                Some(controllers.propertyDetails.routes.PropertyDetailsNewBuildController.view(id).url)
              )
            } yield result
          }
        )
      }
    }
  }
}

object PropertyDetailsNewBuildController extends PropertyDetailsNewBuildController {
  val delegationService: DelegationService = DelegationService
  val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId: String = "PropertyDetailsNewBuildController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
