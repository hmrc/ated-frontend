/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.{BackLinkCacheService, DataCacheService}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models.PropertyDetailsAcquisition
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
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
                                                    (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsAcquisitionController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              currentBackLink.flatMap { backLink =>
                dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                  val filledForm = propertyDetailsAcquisitionForm.fill(PropertyDetailsAcquisition(propertyDetails.value.flatMap(_.anAcquisition)))
                  Ok(template(id,
                    propertyDetails.periodKey,
                    filledForm,
                    AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
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
              val mode = AtedUtils.getEditSubmittedMode(propertyDetails)
              val filledForm = propertyDetailsAcquisitionForm.fill(PropertyDetailsAcquisition(propertyDetails.value.flatMap(_.anAcquisition)))
              Future.successful(Ok(template(id,
                propertyDetails.periodKey,
                filledForm,
                mode,
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
              val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsAcquisitionController.view(id).url)
              for {
                _ <- propertyDetailsService.saveDraftPropertyDetailsAcquisition(id, anAcquisition)
                result <-
                  if (anAcquisition) {
                    redirectWithBackLink(
                      propertyDetailsHasBeenRevaluedController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsHasBeenRevaluedController.view(id),
                      backLink
                    )
                  } else {
                    redirectWithBackLink(
                      isFullTaxPeriodController.controllerId,
                      controllers.propertyDetails.routes.IsFullTaxPeriodController.view(id),
                      backLink
                    )
                  }
              } yield result
            }
          )
        }
      }
    }
  }

}
