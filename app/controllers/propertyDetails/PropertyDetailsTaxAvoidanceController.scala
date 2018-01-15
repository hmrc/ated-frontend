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

package controllers.propertyDetails

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedRegime, ClientHelper}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import models._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import utils.AtedUtils
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.AtedConstants.SelectedPreviousReturn

import scala.concurrent.Future

trait PropertyDetailsTaxAvoidanceController extends PropertyDetailsHelpers with ClientHelper {

  def view(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val displayData = PropertyDetailsTaxAvoidance(propertyDetails.period.flatMap(_.isTaxAvoidance),
              propertyDetails.period.flatMap(_.taxAvoidanceScheme),
              propertyDetails.period.flatMap(_.taxAvoidancePromoterReference))
            currentBackLink.flatMap(backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                Ok(views.html.propertyDetails.propertyDetailsTaxAvoidance(id,
                  propertyDetails.periodKey,
                  propertyDetailsTaxAvoidanceForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  backLink))
              }
            )
        }
      }
  }

  def editFromSummary(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
              val displayData = PropertyDetailsTaxAvoidance(propertyDetails.period.flatMap(_.isTaxAvoidance),
                propertyDetails.period.flatMap(_.taxAvoidanceScheme),
                propertyDetails.period.flatMap(_.taxAvoidancePromoterReference))

              val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
              Future.successful(Ok(views.html.propertyDetails.propertyDetailsTaxAvoidance(id,
                propertyDetails.periodKey,
                propertyDetailsTaxAvoidanceForm.fill(displayData),
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
        PropertyDetailsForms.validatePropertyDetailsTaxAvoidance(propertyDetailsTaxAvoidanceForm.bindFromRequest).fold(
          formWithError =>
            currentBackLink.map(backLink => BadRequest(views.html.propertyDetails.propertyDetailsTaxAvoidance(id, periodKey, formWithError, mode, backLink))),
          propertyDetails => {
            for {
              savedData <- propertyDetailsService.saveDraftPropertyDetailsTaxAvoidance(id, propertyDetails)
              result <-
              RedirectWithBackLink(
                PropertyDetailsSupportingInfoController.controllerId,
                controllers.propertyDetails.routes.PropertyDetailsSupportingInfoController.view(id),
                Some(controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceController.view(id).url)
              )
            } yield result
          }
        )
      }
  }

}

object PropertyDetailsTaxAvoidanceController extends PropertyDetailsTaxAvoidanceController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsTaxAvoidanceController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
