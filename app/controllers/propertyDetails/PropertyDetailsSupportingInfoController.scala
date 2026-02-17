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
import controllers.auth.{AuthAction, ClientHelper}
import controllers.editLiability.EditLiabilitySummaryController
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, DataCacheService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants._
import utils.AtedUtils
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}
import utils.PeriodUtils

class PropertyDetailsSupportingInfoController @Inject()(mcc: MessagesControllerComponents,
                                                        authAction: AuthAction,
                                                        editLiabilitySummaryController: EditLiabilitySummaryController,
                                                        propertyDetailsSummaryController: PropertyDetailsSummaryController,
                                                        serviceInfoService: ServiceInfoService,
                                                        val propertyDetailsService: PropertyDetailsService,
                                                        val dataCacheService: DataCacheService,
                                                        val backLinkCacheService: BackLinkCacheService,
                                                        template: views.html.propertyDetails.propertyDetailsSupportingInfo,
                                                        templateError: views.html.global_error)
                                                       (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsSupportingInfoController"

  def view(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              val filledForm = propertyDetails.period.flatMap(_.supportingInfo) match {
                case Some(info) => propertyDetailsSupportingInfoForm.fill(PropertyDetailsSupportingInfo(info))
                case _ => propertyDetailsSupportingInfoForm
              }
              currentBackLink.flatMap(backLink =>
                dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                  Ok(template(id, propertyDetails.periodKey, filledForm,
                    AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn), serviceInfoContent, backLink))
                }
              )
          }
        }
      }
    }
  }

  def editFromSummary(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                val filledForm = propertyDetails.period.flatMap(_.supportingInfo) match {
                  case Some(info) => propertyDetailsSupportingInfoForm.fill(PropertyDetailsSupportingInfo(info))
                  case _ => propertyDetailsSupportingInfoForm
                }
                val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
                Future.successful(Ok(template(id, propertyDetails.periodKey, filledForm,
                  mode, serviceInfoContent, AtedUtils.getSummaryBackLink(id, None))))
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
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              val period: Option[PropertyDetailsPeriod] = propertyDetails.period
              propertyDetailsSupportingInfoForm.bindFromRequest().fold(
                formWithError => {
                  currentBackLink.map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink)))
                },
                propertyDetails => {
                  val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsSupportingInfoController.view(id).url)
                  for {
                    cachedData <- dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn)
                    _ <- propertyDetailsService.validateCalculateDraftPropertyDetails(id, AtedUtils.isEditSubmittedMode(mode) && cachedData.isEmpty)
                    _ <- propertyDetailsService.saveDraftPropertyDetailsSupportingInfo(id, propertyDetails)
                    result <-
                      if (AtedUtils.isEditSubmittedMode(mode) && cachedData.isEmpty) {
                        redirectWithBackLink(
                          editLiabilitySummaryController.controllerId,
                          controllers.editLiability.routes.EditLiabilitySummaryController.view(id),
                          backLink)
                      } else if (PeriodUtils.getDisplayPeriods(period, periodKey).nonEmpty) {
                        propertyDetailsService.calculateDraftPropertyDetails(id).flatMap { response =>
                          response.status match {
                            case OK =>
                              redirectWithBackLink(
                                propertyDetailsSummaryController.controllerId,
                                controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(id),
                                backLink)
                            case BAD_REQUEST if response.body.contains("Agent not Valid") =>
                              Future.successful(BadRequest(templateError("ated.client-problem.title",
                                "ated.client-problem.header", "ated.client-problem.message", None, Some(appConfig.agentRedirectedToMandate), None, None, serviceInfoContent)))
                            case status =>
                              logger.error(s"[PropertyDetailsSupportingInfoController][save] UNKNOWN_SAVE_STATUS - $status - ${Option(response.body).getOrElse("No response body")}")
                              Future.successful(InternalServerError(templateError("ated.client-problem.title",
                                "ated.client-problem.header", "ated.client-problem.message", None, Some(appConfig.agentRedirectedToMandate), None, None, serviceInfoContent)))
                          }
                        }
                      } else {
                        redirectWithBackLink(
                          propertyDetailsSummaryController.controllerId,
                          controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(id),
                          backLink)
                      }
                  } yield result
                }
              )
          }
        }
      }
    }
  }
}
