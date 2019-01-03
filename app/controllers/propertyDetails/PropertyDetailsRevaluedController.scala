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
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import models.PropertyDetailsRevalued
import services._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.Future

trait PropertyDetailsRevaluedController extends PropertyDetailsHelpers with ClientHelper {

  def view(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                val displayData = PropertyDetailsRevalued(isPropertyRevalued = propertyDetails.value.flatMap(_.isPropertyRevalued),
                  revaluedValue = propertyDetails.value.flatMap(_.revaluedValue),
                  revaluedDate = propertyDetails.value.flatMap(_.revaluedDate),
                  partAcqDispDate = propertyDetails.value.flatMap(_.partAcqDispDate))

                Ok(views.html.propertyDetails.propertyDetailsRevalued(id,
                  propertyDetails.periodKey,
                  propertyDetailsRevaluedForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  backLink))
              }
            }
        }
      }
  }

  def save(id: String, periodKey: Int, mode: Option[String]) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        PropertyDetailsForms.validatePropertyDetailsRevalued(periodKey, propertyDetailsRevaluedForm.bindFromRequest).fold(
          formWithError => {
            currentBackLink.map(backLink => BadRequest(views.html.propertyDetails.propertyDetailsRevalued(id, periodKey, formWithError, mode, backLink)))
          },
          propertyDetails => {
            for {
              savedData <- propertyDetailsService.saveDraftPropertyDetailsRevalued(id, propertyDetails)
              result <-
              RedirectWithBackLink(
                IsFullTaxPeriodController.controllerId,
                controllers.propertyDetails.routes.IsFullTaxPeriodController.view(id),
                Some(controllers.propertyDetails.routes.PropertyDetailsRevaluedController.view(id).url)
              )
            } yield result
          }
        )
      }
  }

}

object PropertyDetailsRevaluedController extends PropertyDetailsRevaluedController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsRevaluedController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
