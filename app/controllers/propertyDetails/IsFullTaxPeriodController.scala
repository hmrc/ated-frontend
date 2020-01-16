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

package controllers.propertyDetails

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants._
import utils.{AtedUtils, PeriodUtils}

import scala.concurrent.{ExecutionContext, Future}

class IsFullTaxPeriodController @Inject()(mcc: MessagesControllerComponents,
                                          authAction: AuthAction,
                                          propertyDetailsInReliefController: PropertyDetailsInReliefController,
                                          propertyDetailsTaxAvoidanceController : PropertyDetailsTaxAvoidanceController,
                                          val propertyDetailsService: PropertyDetailsService,
                                          val dataCacheConnector: DataCacheConnector,
                                          val backLinkCacheConnector: BackLinkCacheConnector)
                                         (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "IsFullTaxPeriodController"

  def view(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { answer =>
              val filledForm = isFullTaxPeriodForm.fill(PropertyDetailsFullTaxPeriod(propertyDetails.period.flatMap(_.isFullPeriod)))
              currentBackLink.flatMap(backLink =>
                answer match {
                  case Some(true) =>
                    Future.successful(Ok(views.html.propertyDetails.isFullTaxPeriod(id, propertyDetails.periodKey, isFullTaxPeriodForm,
                      PeriodUtils.periodStartDate(propertyDetails.periodKey), PeriodUtils.periodEndDate(propertyDetails.periodKey), backLink)))
                  case _ =>
                    Future.successful(Ok(views.html.propertyDetails.isFullTaxPeriod(id, propertyDetails.periodKey, filledForm,
                      PeriodUtils.periodStartDate(propertyDetails.periodKey), PeriodUtils.periodEndDate(propertyDetails.periodKey), backLink)))
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
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val filledForm = isFullTaxPeriodForm.fill(PropertyDetailsFullTaxPeriod(propertyDetails.period.flatMap(_.isFullPeriod)))
            Future.successful(Ok(views.html.propertyDetails.isFullTaxPeriod(id, propertyDetails.periodKey, filledForm,
              PeriodUtils.periodStartDate(propertyDetails.periodKey), PeriodUtils.periodEndDate(propertyDetails.periodKey),
              AtedUtils.getSummaryBackLink(id, None)))
            )
        }
      }
    }
  }

  def save(id: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        isFullTaxPeriodForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.map(backLink =>
              BadRequest(views.html.propertyDetails.isFullTaxPeriod(id, periodKey, formWithError,
                PeriodUtils.periodStartDate(periodKey), PeriodUtils.periodEndDate(periodKey), backLink))
            )
          },
          propertyDetails => {
            propertyDetails.isFullPeriod match {
              case Some(true) =>
                val isFullTaxPeriod = IsFullTaxPeriod(isFullPeriod = true, Some(PropertyDetailsDatesLiable(PeriodUtils.periodStartDate(periodKey),
                  PeriodUtils.periodEndDate(periodKey))))
                propertyDetailsService.saveDraftIsFullTaxPeriod(id, isFullTaxPeriod).flatMap(_ =>
                  redirectWithBackLink(
                    propertyDetailsTaxAvoidanceController.controllerId,
                    controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceController.view(id),
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
