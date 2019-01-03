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

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.{AtedBaseController, BackLinkController}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import forms.ReliefForms._
import models.IsTaxAvoidance
import services.ReliefsService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import utils.{AtedUtils, PeriodUtils}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

trait AvoidanceSchemeBeingUsedController extends BackLinkController
  with AtedFrontendAuthHelpers with ReliefHelpers with DelegationAwareActions with ClientHelper {

  def view(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            backLink <- currentBackLink
            retrievedData <- reliefsService.retrieveDraftReliefs(atedContext.user.atedReferenceNumber, periodKey)
          } yield {
            val isAvoidanceScheme = IsTaxAvoidance(retrievedData.flatMap(_.reliefs.isAvoidanceScheme))
            Ok(views.html.reliefs.avoidanceSchemeBeingUsed(periodKey, isTaxAvoidanceForm.fill(isAvoidanceScheme), PeriodUtils.periodStartDate(periodKey), backLink))
          }
        }
      }
  }

  def editFromSummary(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            retrievedData <- reliefsService.retrieveDraftReliefs(atedContext.user.atedReferenceNumber, periodKey)
            result <-  retrievedData.flatMap(_.reliefs.isAvoidanceScheme) match {
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

  def send(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          val data = AtedUtils.addParamsToRequest(atedContext, Map("periodKey" -> ArrayBuffer(periodKey.toString)))
          isTaxAvoidanceForm.bindFromRequest(data.get).fold(
            formWithError =>
              currentBackLink.map(backLink =>
                BadRequest(views.html.reliefs.avoidanceSchemeBeingUsed(periodKey, formWithError, PeriodUtils.periodStartDate(periodKey), backLink))
              ),
            isTaxAvoidance => {
              reliefsService.saveDraftIsTaxAvoidance(atedContext.user.atedReferenceNumber, periodKey, isTaxAvoidance.isAvoidanceScheme.getOrElse(false)).
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

object AvoidanceSchemeBeingUsedController extends AvoidanceSchemeBeingUsedController {
  val reliefsService = ReliefsService
  val delegationConnector = FrontendDelegationConnector
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "AvoidanceSchemeBeingUsedController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
