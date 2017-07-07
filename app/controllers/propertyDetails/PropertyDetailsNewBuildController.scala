/*
 * Copyright 2017 HM Revenue & Customs
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

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedRegime, ClientHelper}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import models.PropertyDetailsNewBuild
import services._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.Future

trait PropertyDetailsNewBuildController extends PropertyDetailsHelpers with ClientHelper {

  def view(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
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

  def save(id: String, periodKey: Int, mode: Option[String]) = AuthAction(AtedRegime) {
    implicit atedContext =>
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

object PropertyDetailsNewBuildController extends PropertyDetailsNewBuildController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsNewBuildController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
