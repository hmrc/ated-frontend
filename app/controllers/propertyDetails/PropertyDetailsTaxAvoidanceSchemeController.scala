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
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms.*
import models.*
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, DataCacheService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import utils.AtedUtils.EDIT_FROM_SUMMARY

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsTaxAvoidanceSchemeController @Inject()(mcc: MessagesControllerComponents,
                                                            authAction: AuthAction,
                                                            propertyDetailsTaxAvoidanceReferencesController: PropertyDetailsTaxAvoidanceReferencesController,
                                                            propertyDetailsSupportingInfoController: PropertyDetailsSupportingInfoController,
                                                            serviceInfoService: ServiceInfoService,
                                                            val propertyDetailsService: PropertyDetailsService,
                                                            val dataCacheService: DataCacheService,
                                                            val backLinkCacheService: BackLinkCacheService,
                                                            template: views.html.propertyDetails.propertyDetailsTaxAvoidanceScheme)
                                                           (using val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  given ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "PropertyDetailsTaxAvoidanceSchemeController"

  def view(id: String, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              val displayData = PropertyDetailsTaxAvoidanceScheme(propertyDetails.period.flatMap(_.isTaxAvoidance))
              currentBackLink.flatMap(backLink =>
                dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                  dataCacheService.fetchAndGetData[String]("EditSummaryEntryController").map { entryController =>

                    val modeView = if (!mode.contains(EDIT_FROM_SUMMARY)) {
                      AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
                    } else {
                      mode
                    }

                    val backLinkView =
                      if (
                        mode.contains(EDIT_FROM_SUMMARY) &&
                          entryController.contains(controllerId)
                      ) {
                        AtedUtils.getSummaryBackLink(id, Some(EDIT_FROM_SUMMARY))
                      } else {
                        backLink
                      }

                    Ok(template(
                      id,
                      propertyDetails.periodKey,
                      propertyDetailsTaxAvoidanceSchemeForm.fill(displayData),
                      modeView,
                      serviceInfoContent,
                      backLinkView))
                  }
                }
              )
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

              dataCacheService
                .saveFormData[String](
                  "EditSummaryEntryController",
                  controllerId
                )
                .flatMap { _ =>

                  dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                    val displayData = PropertyDetailsTaxAvoidanceScheme(propertyDetails.period.flatMap(_.isTaxAvoidance))

                    val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn).getOrElse(EDIT_FROM_SUMMARY)

                    Future.successful(
                      Ok(
                        template(
                          id,
                          propertyDetails.periodKey,
                          propertyDetailsTaxAvoidanceSchemeForm.fill(displayData),
                          Some(mode),
                          serviceInfoContent,
                          AtedUtils.getSummaryBackLink(id, None)
                        )
                      )
                    )
                  }
                }
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsTaxAvoidanceSchemeForm.bindFromRequest().fold(
            formWithError =>
              currentBackLink.map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink))),
            propertyDetails => {
              propertyDetails.isTaxAvoidance match {
                case Some(true) =>
                  propertyDetailsService.saveDraftPropertyDetailsTaxAvoidanceScheme(id, propertyDetails).flatMap(_ =>
                    redirectWithBackLink(
                      propertyDetailsTaxAvoidanceReferencesController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceReferencesController.view(id, mode),
                      Some(controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceSchemeController.view(id, mode).url))
                  )
                case _ =>
                  propertyDetailsService.saveDraftPropertyDetailsTaxAvoidanceScheme(id, propertyDetails).flatMap(_ =>
                    redirectWithBackLink(
                      propertyDetailsSupportingInfoController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsSupportingInfoController.view(id, mode),
                      Some(controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceSchemeController.view(id, mode).url))
                  )
              }
            }
          )
        }
      }
    }
  }
}
