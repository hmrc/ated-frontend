/*
 * Copyright 2017 HM Revenue & Customs
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
import controllers.BackLinkController
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import forms.ReliefForms._
import models.Reliefs
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.ReliefsService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import utils.PeriodUtils

trait ChooseReliefsController extends BackLinkController
  with AtedFrontendAuthHelpers with ReliefHelpers with DelegationAwareActions  with ClientHelper {

  def view(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            backLink <- currentBackLink
            retrievedData <- reliefsService.retrieveDraftReliefs(atedContext.user.atedReferenceNumber, periodKey)
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

  def editFromSummary(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            retrievedData <- reliefsService.retrieveDraftReliefs(atedContext.user.atedReferenceNumber, periodKey)
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

  def send(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          reliefsForm.bindFromRequest.fold(
            formWithError =>
              currentBackLink.map(backLink =>
                BadRequest(views.html.reliefs.chooseReliefs(periodKey, formWithError, PeriodUtils.periodStartDate(periodKey), backLink))
              ),
            reliefs => {
              for {
                savedData <- reliefsService.saveDraftReliefs(atedContext.user.atedReferenceNumber, periodKey, reliefs)
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

object ChooseReliefsController extends ChooseReliefsController {
  val reliefsService = ReliefsService
  val delegationConnector = FrontendDelegationConnector
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "ChooseReliefsController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
