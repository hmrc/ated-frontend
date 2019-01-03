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

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedRegime, ClientHelper}
import controllers.editLiability.EditLiabilityHasValueChangedController
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import models.PropertyDetailsTitle
import services._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.Future

trait PropertyDetailsTitleController extends PropertyDetailsHelpers with ClientHelper {

  def view(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map{ isPrevReturn =>
                val displayData = propertyDetails.title.getOrElse(new PropertyDetailsTitle(""))
                Ok(views.html.propertyDetails.propertyDetailsTitle(id, propertyDetails.periodKey, propertyDetailsTitleForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  backLink))
              }
            }
        }
      }
  }


  def editFromSummary(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
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

  def save(id: String, periodKey: Int, mode: Option[String] = None) = AuthAction(AtedRegime) {
    implicit atedContext =>
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
              if (AtedUtils.isEditSubmittedMode(mode))
                RedirectWithBackLink(
                  EditLiabilityHasValueChangedController.controllerId,
                  controllers.editLiability.routes.EditLiabilityHasValueChangedController.view(id),
                  backLink)
              else
                RedirectWithBackLink(
                  PropertyDetailsOwnedBeforeController.controllerId,
                  controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(id),
                  backLink)
            } yield result
          }
        )
      }
  }

}

object PropertyDetailsTitleController extends PropertyDetailsTitleController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsTitleController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
