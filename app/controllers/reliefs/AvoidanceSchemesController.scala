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
import connectors.BackLinkCacheConnector
import controllers.{AtedBaseController, BackLinkController}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime}
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.{AtedBaseController, BackLinkController}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import forms.ReliefForms._
import models.TaxAvoidance
import services.ReliefsService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

trait AvoidanceSchemesController extends BackLinkController
  with AtedFrontendAuthHelpers with ReliefHelpers with DelegationAwareActions  with ClientHelper {

  def view(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      validatePeriodKey(periodKey) {
        reliefsService.retrieveDraftReliefs(atedContext.user.atedReferenceNumber, periodKey).flatMap {
          case Some(x) if (x.reliefs.isAvoidanceScheme.contains(true))  =>
            currentBackLink.map(backLink =>
              Ok(views.html.reliefs.avoidanceSchemes(x.periodKey, taxAvoidanceForm.fill(x.taxAvoidance), backLink)(Some(x)))
            )
          case _ =>
           reliefsService.saveDraftTaxAvoidance(atedContext.user.atedReferenceNumber, periodKey, TaxAvoidance())
            ForwardBackLinkToNextPage(
              ReliefsSummaryController.controllerId,
              controllers.reliefs.routes.ReliefsSummaryController.view(periodKey)
            )
        }
      }
  }

  def submit(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          validateTaxAvoidance(taxAvoidanceForm.bindFromRequest).fold(
            formWithError => {
              for {
                backLink <- currentBackLink
                retrievedData <- reliefsService.retrieveDraftReliefs(atedContext.user.atedReferenceNumber, periodKey)
              } yield {
                BadRequest(views.html.reliefs.avoidanceSchemes(periodKey, formWithError, backLink)(retrievedData))
              }
            },
            taxAvoidance => {
              for {
                _ <- reliefsService.saveDraftTaxAvoidance(atedContext.user.atedReferenceNumber, periodKey, taxAvoidance)
                result <-
                RedirectWithBackLink(
                  ReliefsSummaryController.controllerId,
                  controllers.reliefs.routes.ReliefsSummaryController.view(periodKey),
                  Some(routes.AvoidanceSchemesController.view(periodKey).url)
                )
              } yield {
                result
              }
            }
          )
        }
      }
  }

}

object AvoidanceSchemesController extends AvoidanceSchemesController {
  val reliefsService = ReliefsService
  val dataCacheConnector = DataCacheConnector
  val delegationConnector = FrontendDelegationConnector
  override val controllerId = "AvoidanceSchemesController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
