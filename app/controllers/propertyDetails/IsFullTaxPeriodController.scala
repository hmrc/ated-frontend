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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._

import javax.inject.Inject
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants._
import utils.{AtedUtils, PeriodUtils}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding

import scala.concurrent.{ExecutionContext, Future}

class IsFullTaxPeriodController @Inject()(mcc: MessagesControllerComponents,
                                          authAction: AuthAction,
                                          propertyDetailsInReliefController: PropertyDetailsInReliefController,
                                          propertyDetailsTaxAvoidanceSchemeController : PropertyDetailsTaxAvoidanceSchemeController,
                                          serviceInfoService: ServiceInfoService,
                                          val propertyDetailsService: PropertyDetailsService,
                                          val dataCacheConnector: DataCacheConnector,
                                          val backLinkCacheConnector: BackLinkCacheConnector,
                                          template: views.html.propertyDetails.isFullTaxPeriod)
                                         (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "IsFullTaxPeriodController"

  def view(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { answer =>
                val filledForm = isFullTaxPeriodForm.fill(PropertyDetailsFullTaxPeriod(propertyDetails.period.flatMap(_.isFullPeriod)))
                currentBackLink.flatMap(backLink =>
                  answer match {
                    case Some(true) =>
                      Future.successful(Ok(template(id, propertyDetails.periodKey, isFullTaxPeriodForm,
                        PeriodUtils.periodStartDate(propertyDetails.periodKey), PeriodUtils.periodEndDate(propertyDetails.periodKey), None, serviceInfoContent, backLink)))
                    case _ =>
                      Future.successful(Ok(template(id, propertyDetails.periodKey, filledForm,
                        PeriodUtils.periodStartDate(propertyDetails.periodKey), PeriodUtils.periodEndDate(propertyDetails.periodKey), None,serviceInfoContent, backLink)))
                  }
                )
              }
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
              val filledForm = isFullTaxPeriodForm.fill(PropertyDetailsFullTaxPeriod(propertyDetails.period.flatMap(_.isFullPeriod)))
              Future.successful(Ok(template(id, propertyDetails.periodKey, filledForm,
                PeriodUtils.periodStartDate(propertyDetails.periodKey), PeriodUtils.periodEndDate(propertyDetails.periodKey),
                None,
                serviceInfoContent,
                AtedUtils.getSummaryBackLink(id, None)))
              )
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          isFullTaxPeriodForm.bindFromRequest().fold(
            formWithError => {
              currentBackLink.map(backLink =>
                BadRequest(template(id, periodKey, formWithError,
                  PeriodUtils.periodStartDate(periodKey), PeriodUtils.periodEndDate(periodKey), None, serviceInfoContent, backLink))
              )
            },
            propertyDetails => {
              propertyDetails.isFullPeriod match {
                case Some(true) =>
                  val isFullTaxPeriod = IsFullTaxPeriod(isFullPeriod = true, Some(PropertyDetailsDatesLiable(Some(PeriodUtils.periodStartDate(periodKey)),
                    Some(PeriodUtils.periodEndDate(periodKey)))))
                  propertyDetailsService.saveDraftIsFullTaxPeriod(id, isFullTaxPeriod).flatMap(_ =>
                    redirectWithBackLink(
                      propertyDetailsTaxAvoidanceSchemeController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceSchemeController.view(id),
                      Some(routes.IsFullTaxPeriodController.view(id).url))
                  )
                case _ =>
                  propertyDetailsService.saveDraftIsFullTaxPeriod(id, IsFullTaxPeriod(isFullPeriod = false, None)).flatMap(_ =>
                    redirectWithBackLink(
                      propertyDetailsInReliefController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsInReliefController.view(id),
                      Some(routes.IsFullTaxPeriodController.view(id).url)))
              }
            }
          )
        }
      }
    }
  }
}
