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

package controllers.editLiability

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import controllers.propertyDetails.{PropertyDetailsHelpers, PropertyDetailsTaxAvoidanceController}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import models._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}

trait EditLiabilityDatesLiableController extends PropertyDetailsHelpers with ClientHelper with AuthAction {

  def view(formBundleNo: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(formBundleNo) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val liabilityPeriod = propertyDetails.period.flatMap(_.liabilityPeriods.headOption)

            val filledForm = liabilityPeriod match {
              case Some(lineItem) => periodDatesLiableForm.fill(PropertyDetailsDatesLiable(lineItem.startDate, lineItem.endDate))
              case _ => periodDatesLiableForm
            }
            currentBackLink.map(backLink =>
              Ok(views.html.editLiability.editLiabilityDatesLiable(formBundleNo, propertyDetails.periodKey, filledForm, backLink))
            )
        }
      }
    }
  }


  def save(formBundleNo: String, periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, periodDatesLiableForm.bindFromRequest, periodsCheck = false).fold(
          formWithError => {
            currentBackLink.map(backLink =>
              BadRequest(views.html.editLiability.editLiabilityDatesLiable(formBundleNo, periodKey, formWithError, backLink))
            )
          },
          propertyDetails => {
            for {
              _ <- propertyDetailsService.saveDraftPropertyDetailsDatesLiable(formBundleNo, propertyDetails)
              result <-
              RedirectWithBackLink(
                PropertyDetailsTaxAvoidanceController.controllerId,
                controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceController.view(formBundleNo),
                Some(controllers.editLiability.routes.EditLiabilityDatesLiableController.view(formBundleNo).url)
              )
            } yield result
          }
        )
      }
    }
  }

}

object EditLiabilityDatesLiableController extends EditLiabilityDatesLiableController {
  val delegationService: DelegationService = DelegationService
  val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId = "EditLiabilityDatesLiableController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
