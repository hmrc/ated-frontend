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

package controllers.reliefs

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import forms.ReliefForms._
import models.IsTaxAvoidance
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, ReliefsService}
import utils.{AtedUtils, PeriodUtils}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

trait AvoidanceSchemeBeingUsedController extends BackLinkController
   with ReliefHelpers with ClientHelper with AuthAction {

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            backLink <- currentBackLink
            retrievedData <- reliefsService.retrieveDraftReliefs(authContext.atedReferenceNumber.get, periodKey)
          } yield {
            val isAvoidanceScheme = IsTaxAvoidance(retrievedData.flatMap(_.reliefs.isAvoidanceScheme))
            Ok(views.html.reliefs.avoidanceSchemeBeingUsed(periodKey, isTaxAvoidanceForm.fill(isAvoidanceScheme),
              PeriodUtils.periodStartDate(periodKey), backLink))
          }
        }
      }
    }
  }

  def editFromSummary(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            retrievedData <- reliefsService.retrieveDraftReliefs(authContext.atedReferenceNumber.get, periodKey)
            result <- retrievedData.flatMap(_.reliefs.isAvoidanceScheme) match {
              case Some(true) =>
                RedirectWithBackLink(
                  AvoidanceSchemesController.controllerId,
                  controllers.reliefs.routes.AvoidanceSchemesController.view(periodKey),
                  Some(routes.ReliefsSummaryController.view(periodKey).url)
                )
              case _ =>
                Future.successful(Redirect(controllers.reliefs.routes.AvoidanceSchemeBeingUsedController.view(periodKey)))
            }
          } yield result
        }
      }
    }
  }

  def send(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          val data = AtedUtils.addParamsToRequest(Map("periodKey" -> ArrayBuffer(periodKey.toString)))
          isTaxAvoidanceForm.bindFromRequest(data.get).fold(
            formWithError =>
              currentBackLink.map(backLink =>
                BadRequest(views.html.reliefs.avoidanceSchemeBeingUsed(periodKey, formWithError, PeriodUtils.periodStartDate(periodKey), backLink))
              ),
            isTaxAvoidance => {
              reliefsService.saveDraftIsTaxAvoidance(authContext.atedReferenceNumber.get, periodKey, isTaxAvoidance.isAvoidanceScheme.getOrElse(false)).
                flatMap {
                  x =>
                    RedirectWithBackLink(
                      AvoidanceSchemesController.controllerId,
                      controllers.reliefs.routes.AvoidanceSchemesController.view(periodKey),
                      Some(routes.AvoidanceSchemeBeingUsedController.view(periodKey).url)
                    )
                }
            }
          )
        }
      }
    }
  }

}

object AvoidanceSchemeBeingUsedController extends AvoidanceSchemeBeingUsedController {
  val delegationService: DelegationService = DelegationService
  val reliefsService: ReliefsService = ReliefsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId: String = "AvoidanceSchemeBeingUsedController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
