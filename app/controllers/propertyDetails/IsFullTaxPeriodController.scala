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

package controllers.propertyDetails

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import models._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import utils.AtedConstants._
import utils.{AtedUtils, PeriodUtils}

import scala.concurrent.Future

trait IsFullTaxPeriodController extends PropertyDetailsHelpers with ClientHelper with AuthAction {

  def view(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { answer =>
              if (AtedUtils.isEditSubmitted(propertyDetails) && answer.isEmpty) {
                ForwardBackLinkToNextPage(
                  PropertyDetailsInReliefController.controllerId,
                  controllers.propertyDetails.routes.PropertyDetailsInReliefController.view(id)
                )
              } else {
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
  }

  def editFromSummary(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
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
    authorisedAction { implicit authContext =>
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
                propertyDetailsService.saveDraftIsFullTaxPeriod(id, isFullTaxPeriod).flatMap(x =>
                  RedirectWithBackLink(
                    PropertyDetailsTaxAvoidanceController.controllerId,
                    controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceController.view(id),
                    Some(routes.IsFullTaxPeriodController.view(id).url))
                )
              case _ =>
                propertyDetailsService.saveDraftIsFullTaxPeriod(id, IsFullTaxPeriod(isFullPeriod = false, None)).flatMap(x =>
                  RedirectWithBackLink(
                    PropertyDetailsInReliefController.controllerId,
                    controllers.propertyDetails.routes.PropertyDetailsInReliefController.view(id),
                    Some(routes.IsFullTaxPeriodController.view(id).url)))
            }
          }
        )
      }
    }
  }
}

object IsFullTaxPeriodController extends IsFullTaxPeriodController {
  val delegationService: DelegationService = DelegationService
  val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId: String = "IsFullTaxPeriodController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
