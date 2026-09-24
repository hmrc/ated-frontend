/*
 * Copyright 2026 HM Revenue & Customs
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

import config.ApplicationConfig
import controllers.ControllerIds
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms.*

import javax.inject.Inject
import models.PropertyDetailsAcquisition
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import utils.AtedUtils.EDIT_FROM_SUMMARY

import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsAcquisitionController @Inject()(mcc: MessagesControllerComponents,
                                                     authAction: AuthAction,
                                                     isFullTaxPeriodController: IsFullTaxPeriodController,
                                                     propertyDetailsHasBeenRevaluedController: PropertyDetailsHasBeenRevaluedController,
                                                     serviceInfoService: ServiceInfoService,
                                                     val propertyDetailsService: PropertyDetailsService,
                                                     val dataCacheService: DataCacheService,
                                                     val backLinkCacheService: BackLinkCacheService,
                                                     template: views.html.propertyDetails.propertyDetailsAcquisition)
                                                    (using val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with ControllerIds {

  given ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsAcquisitionController"

  def view(id: String, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              currentBackLink.flatMap { backLink =>
                dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                  val modeView = if (!mode.contains(EDIT_FROM_SUMMARY)) {
                    AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
                  } else {
                    mode
                  }
                  val filledForm = propertyDetailsAcquisitionForm.fill(PropertyDetailsAcquisition(propertyDetails.value.flatMap(_.anAcquisition)))
                  Ok(template(id,
                    propertyDetails.periodKey,
                    filledForm,
                    modeView,
                    serviceInfoContent,
                    backLink)
                  )
                }
              }
          }
        }
      }
    }
  }

  def editFromSummary(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              val mode = AtedUtils.getEditSubmittedMode(propertyDetails).getOrElse(EDIT_FROM_SUMMARY)
              val filledForm = propertyDetailsAcquisitionForm.fill(PropertyDetailsAcquisition(propertyDetails.value.flatMap(_.anAcquisition)))
              Future.successful(Ok(template(id,
                propertyDetails.periodKey,
                filledForm,
                Some(mode),
                serviceInfoContent,
                AtedUtils.getSummaryBackLink(id, None))
              ))
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsAcquisitionForm.bindFromRequest().fold(
            formWithError => {
              currentBackLink.map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink)))
            },
            propertyDetails => {
              val anAcquisition = propertyDetails.anAcquisition.getOrElse(false)
              val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsAcquisitionController.view(id, mode).url)
              for {
                _ <- propertyDetailsService.saveDraftPropertyDetailsAcquisition(id, anAcquisition)
                result <-
                  if (anAcquisition) {
                    redirectWithBackLink(
                      propertyDetailsHasBeenRevaluedController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsHasBeenRevaluedController.view(id, mode),
                      backLink
                    )
                  } else {
                    if (mode.contains(EDIT_FROM_SUMMARY)) {
                      redirectWithBackLink(
                        propertyDetailsSummaryControllerId,
                        controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(id),
                        backLink
                      )
                    } else {
                      redirectWithBackLink(
                        isFullTaxPeriodController.controllerId,
                        controllers.propertyDetails.routes.IsFullTaxPeriodController.view(id, mode),
                        backLink
                      )
                    }
                  }
              } yield result
            }
          )
        }
      }
    }
  }

}
