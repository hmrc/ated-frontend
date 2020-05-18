/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import controllers.propertyDetails._
import forms.PropertyDetailsForms.hasValueChangedForm
import javax.inject.Inject
import models.HasValueChanged
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.{ExecutionContext, Future}

class EditLiabilityHasValueChangedController @Inject()(mcc: MessagesControllerComponents,
                                                       propertyDetailsOwnedBeforeController: PropertyDetailsOwnedBeforeController,
                                                       isFullTaxPeriodController: IsFullTaxPeriodController,
                                                       authAction: AuthAction,
                                                       val propertyDetailsService: PropertyDetailsService,
                                                       val dataCacheConnector: DataCacheConnector,
                                                       val backLinkCacheConnector: BackLinkCacheConnector)
                                                      (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "EditLiabilityHasValueChangedController"

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(oldFormBundleNo) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
              currentBackLink.map { backLink =>
                val filledForm = hasValueChangedForm.fill(HasValueChanged(propertyDetails.value.flatMap(_.hasValueChanged)))
                val previousValue = propertyDetails.formBundleReturn.map(_.lineItem.head.propertyValue)
                Ok(views.html.editLiability.editLiabilityHasValueChanged(previousValue, oldFormBundleNo, filledForm,
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn), backLink))
              }
            }
        }
      }
    }
  }

  def editFromSummary(oldFormBundleNo: String, isPrevReturn: Option[Boolean] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(oldFormBundleNo) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            Future.successful {
              val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
              val filledForm = hasValueChangedForm.fill(HasValueChanged(propertyDetails.value.flatMap(_.hasValueChanged)))
              val previousValue = propertyDetails.formBundleReturn.map(_.lineItem.head.propertyValue)
              Ok(views.html.editLiability.editLiabilityHasValueChanged(previousValue, oldFormBundleNo, filledForm,
                mode, AtedUtils.getSummaryBackLink(oldFormBundleNo, mode)))
            }
        }
      }
    }
  }

  def save(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        hasValueChangedForm.bindFromRequest.fold(
          formWithErrors => {
            propertyDetailsCacheResponse(oldFormBundleNo) {
              case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
                dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                  currentBackLink.map { backLink =>
                    val previousValue = propertyDetails.formBundleReturn.map(_.lineItem.head.propertyValue)
                    BadRequest(views.html.editLiability.editLiabilityHasValueChanged(previousValue, oldFormBundleNo,
                      formWithErrors, AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn), backLink))
                  }
                }
            }
          },
          propertyDetails => {
            val hasValueChanged = propertyDetails.hasValueChanged.getOrElse(false)
            val backLink = Some(controllers.editLiability.routes.EditLiabilityHasValueChangedController.view(oldFormBundleNo).url)
            propertyDetailsService.saveDraftHasValueChanged(oldFormBundleNo, hasValueChanged) flatMap {
              _ =>
                if (hasValueChanged) {
                  redirectWithBackLink(
                    propertyDetailsOwnedBeforeController.controllerId,
                    controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(oldFormBundleNo),
                    backLink
                  )
                } else {
                  redirectWithBackLink(
                    isFullTaxPeriodController.controllerId,
                    controllers.propertyDetails.routes.IsFullTaxPeriodController.view(oldFormBundleNo),
                    backLink
                  )
                }
            }
          }
        )
      }
    }
  }
}

