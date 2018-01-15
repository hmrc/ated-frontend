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

package controllers.editLiability

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedRegime, ClientHelper}
import controllers.propertyDetails.{PropertyDetailsHelpers, PropertyDetailsTaxAvoidanceController}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import models._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait EditLiabilityDatesLiableController extends PropertyDetailsHelpers with ClientHelper {

  def view(formBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
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


  def save(formBundleNo: String, periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, periodDatesLiableForm.bindFromRequest, false).fold(
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

object EditLiabilityDatesLiableController extends EditLiabilityDatesLiableController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "EditLiabilityDatesLiableController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
