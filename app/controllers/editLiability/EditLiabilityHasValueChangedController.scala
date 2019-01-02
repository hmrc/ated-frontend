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

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import controllers.propertyDetails._
import models.HasValueChanged
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.AtedUtils

import scala.concurrent.Future
import forms.PropertyDetailsForms.hasValueChangedForm
import utils.AtedConstants.SelectedPreviousReturn

trait EditLiabilityHasValueChangedController extends PropertyDetailsHelpers
  with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  def view(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(oldFormBundleNo) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
              currentBackLink.map { backLink =>
                val filledForm = hasValueChangedForm.fill(HasValueChanged(propertyDetails.value.flatMap(_.hasValueChanged)))
                val previousValue = propertyDetails.formBundleReturn.map(_.lineItem.head.propertyValue)
                Ok(views.html.editLiability.editLiabilityHasValueChanged(previousValue, oldFormBundleNo, filledForm, AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn), backLink))
              }
            }
        }
      }
  }

  def editFromSummary(oldFormBundleNo: String, isPrevReturn: Option[Boolean] = None) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(oldFormBundleNo) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            Future.successful {
              val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
              val filledForm = hasValueChangedForm.fill(HasValueChanged(propertyDetails.value.flatMap(_.hasValueChanged)))
              val previousValue = propertyDetails.formBundleReturn.map(_.lineItem.head.propertyValue)
              Ok(views.html.editLiability.editLiabilityHasValueChanged(previousValue, oldFormBundleNo, filledForm, mode, AtedUtils.getSummaryBackLink(oldFormBundleNo, mode)))
            }
        }
      }
  }

  def save(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        hasValueChangedForm.bindFromRequest.fold(
          formWithErrors => {
            propertyDetailsCacheResponse(oldFormBundleNo) {
              case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
                dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                  currentBackLink.map { backLink =>
                    val previousValue = propertyDetails.formBundleReturn.map(_.lineItem.head.propertyValue)
                    BadRequest(views.html.editLiability.editLiabilityHasValueChanged(previousValue, oldFormBundleNo, formWithErrors, AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn), backLink))
                  }
                }
            }
          },
          propertyDetails => {
            val hasValueChanged = propertyDetails.hasValueChanged.getOrElse(false)
            val backLink = Some(controllers.editLiability.routes.EditLiabilityHasValueChangedController.view(oldFormBundleNo).url)
            propertyDetailsService.saveDraftHasValueChanged(oldFormBundleNo, hasValueChanged) flatMap {
              response =>
                if (hasValueChanged)
                  RedirectWithBackLink(
                    PropertyDetailsOwnedBeforeController.controllerId,
                    controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(oldFormBundleNo),
                    backLink
                  )
                else
                  RedirectWithBackLink(
                    IsFullTaxPeriodController.controllerId,
                    controllers.propertyDetails.routes.IsFullTaxPeriodController.view(oldFormBundleNo),
                    backLink
                  )
            }
          }
        )
      }
  }
}

object EditLiabilityHasValueChangedController extends EditLiabilityHasValueChangedController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "EditLiabilityHasValueChangedController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
