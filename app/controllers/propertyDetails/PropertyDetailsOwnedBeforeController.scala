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
import models.PropertyDetailsOwnedBefore
import services._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.Future

trait PropertyDetailsOwnedBeforeController extends PropertyDetailsHelpers with ClientHelper {

  def view(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                val displayData = PropertyDetailsOwnedBefore(propertyDetails.value.flatMap(_.isOwnedBefore2012), propertyDetails.value.flatMap(_.ownedBefore2012Value))
                Future.successful(Ok(views.html.propertyDetails.propertyDetailsOwnedBefore(id,
                  propertyDetails.periodKey,
                  propertyDetailsOwnedBeforeForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  backLink)
                ))
              }
            }
        }
      }
  }

  def editFromSummary(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
              val displayData = PropertyDetailsOwnedBefore(propertyDetails.value.flatMap(_.isOwnedBefore2012), propertyDetails.value.flatMap(_.ownedBefore2012Value))
              val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
              Future.successful(Ok(views.html.propertyDetails.propertyDetailsOwnedBefore(id,
                propertyDetails.periodKey,
                propertyDetailsOwnedBeforeForm.fill(displayData),
                mode,
                AtedUtils.getSummaryBackLink(id, None))
              ))
            }
        }
      }
  }

  def save(id: String, periodKey: Int, mode: Option[String]) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        PropertyDetailsForms.validatePropertyDetailsOwnedBefore(propertyDetailsOwnedBeforeForm.bindFromRequest).fold(
          formWithError => {
            currentBackLink.map(backLink =>
              BadRequest(views.html.propertyDetails.propertyDetailsOwnedBefore(id, periodKey, formWithError, mode, backLink))
            )
          },
          propertyDetails => {
            for {
              savedData <- propertyDetailsService.saveDraftPropertyDetailsOwnedBefore(id, propertyDetails)
              result <-
              if (propertyDetails.isOwnedBefore2012.getOrElse(false))
                RedirectWithBackLink(
                  PropertyDetailsProfessionallyValuedController.controllerId,
                  controllers.propertyDetails.routes.PropertyDetailsProfessionallyValuedController.view(id),
                  Some(controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(id).url)
                )
              else
                RedirectWithBackLink(
                  PropertyDetailsNewBuildController.controllerId,
                  controllers.propertyDetails.routes.PropertyDetailsNewBuildController.view(id),
                  Some(controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(id).url)
                )
            } yield result
          }
        )
      }
  }

}

object PropertyDetailsOwnedBeforeController extends PropertyDetailsOwnedBeforeController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsOwnedBeforeController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
