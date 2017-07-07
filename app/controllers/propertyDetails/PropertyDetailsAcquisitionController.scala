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
import models.PropertyDetailsAcquisition
import services._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.Future

trait PropertyDetailsAcquisitionController extends PropertyDetailsHelpers with ClientHelper {

  def view(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                val filledForm = propertyDetailsAcquisitionForm.fill(PropertyDetailsAcquisition(propertyDetails.value.flatMap(_.anAcquisition)))
                Ok(views.html.propertyDetails.propertyDetailsAcquisition(id,
                  propertyDetails.periodKey,
                  filledForm,
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  backLink)
                )
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
            val mode = AtedUtils.getEditSubmittedMode(propertyDetails)
            val filledForm = propertyDetailsAcquisitionForm.fill(PropertyDetailsAcquisition(propertyDetails.value.flatMap(_.anAcquisition)))
            Future.successful(Ok(views.html.propertyDetails.propertyDetailsAcquisition(id,
              propertyDetails.periodKey,
              filledForm,
              mode,
              AtedUtils.getSummaryBackLink(id, None))
            ))
        }
      }
  }

  def save(id: String, periodKey: Int, mode: Option[String]) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsAcquisitionForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.map(backLink => BadRequest(views.html.propertyDetails.propertyDetailsAcquisition(id, periodKey, formWithError, mode, backLink)))
          },
          propertyDetails => {
            val anAcquisition = propertyDetails.anAcquisition.getOrElse(false)
            val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsAcquisitionController.view(id).url)
            for {
              savedData <- propertyDetailsService.saveDraftPropertyDetailsAcquisition(id, anAcquisition)
              result <-
              if (anAcquisition)
                RedirectWithBackLink(
                  PropertyDetailsRevaluedController.controllerId,
                  controllers.propertyDetails.routes.PropertyDetailsRevaluedController.view(id),
                  backLink
                )
              else
                RedirectWithBackLink(
                  IsFullTaxPeriodController.controllerId,
                  controllers.propertyDetails.routes.IsFullTaxPeriodController.view(id),
                  backLink
                )
            } yield result
          }
        )
      }
  }

}

object PropertyDetailsAcquisitionController extends PropertyDetailsAcquisitionController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsAcquisitionController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
