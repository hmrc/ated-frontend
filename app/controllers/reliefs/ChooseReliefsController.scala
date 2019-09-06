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
import models.Reliefs
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, ReliefsService}
import utils.AtedUtils._
import utils.PeriodUtils

import scala.collection.mutable.ArrayBuffer

trait ChooseReliefsController extends BackLinkController
    with ReliefHelpers with ClientHelper with AuthAction {

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            backLink <- currentBackLink
            retrievedData <- reliefsService.retrieveDraftReliefs(authContext.atedReferenceNumber.get, periodKey)
          } yield {
            retrievedData match {
              case Some(reliefs) =>
                Ok(views.html.reliefs.chooseReliefs(reliefs.periodKey, reliefsForm.fill(reliefs.reliefs), PeriodUtils.periodStartDate(periodKey), backLink))
              case _ => Ok(views.html.reliefs.chooseReliefs(periodKey, reliefsForm.fill(Reliefs(periodKey)), PeriodUtils.periodStartDate(periodKey), backLink))
            }
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
            } yield {
              val backLink = Some(controllers.reliefs.routes.ReliefsSummaryController.view(periodKey).url)
              retrievedData match {
                case Some(reliefs) =>
                  Ok(views.html.reliefs.chooseReliefs(reliefs.periodKey, reliefsForm.fill(reliefs.reliefs), PeriodUtils.periodStartDate(periodKey), backLink))
                case _ => Ok(views.html.reliefs.chooseReliefs(periodKey, reliefsForm.fill(Reliefs(periodKey)), PeriodUtils.periodStartDate(periodKey), backLink))
              }
            }
          }
        }
      }
    }

    def send(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
      authorisedAction { implicit authContext =>
        ensureClientContext {
          validatePeriodKey(periodKey) {
            val data = addParamsToRequest(Map("periodKey" -> ArrayBuffer(periodKey.toString)))
            reliefsForm.bindFromRequest(data.get).fold(
              formWithError =>
                currentBackLink.map { backLink =>
                  BadRequest(views.html.reliefs.chooseReliefs(periodKey, formWithError, PeriodUtils.periodStartDate(periodKey), backLink))
                },
              reliefs => {
                for {
                  savedData <- reliefsService.saveDraftReliefs(authContext.atedReferenceNumber.get, periodKey, reliefs)
                  result <-
                  RedirectWithBackLink(AvoidanceSchemeBeingUsedController.controllerId,
                    controllers.reliefs.routes.AvoidanceSchemeBeingUsedController.view(periodKey),
                    Some(routes.ChooseReliefsController.view(periodKey).url))
                } yield {
                  result
                }
              }
            )
          }
        }
      }
    }
  }

object ChooseReliefsController extends ChooseReliefsController {
  val reliefsService: ReliefsService = ReliefsService
  val delegationService: DelegationService = DelegationService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId: String = "ChooseReliefsController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
