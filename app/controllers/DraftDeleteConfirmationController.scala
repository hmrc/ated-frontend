/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import config.FrontendDelegationConnector
import connectors.DataCacheConnector
import controllers.AtedBaseController
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import forms.AtedForms.YesNoQuestionForm
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.{PropertyDetailsService, ReliefsService}
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions

import scala.concurrent.Future

trait DraftDeleteConfirmationController extends AtedBaseController with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  val propertyDetailsService: PropertyDetailsService
  val reliefsService: ReliefsService

  def dataCacheConnector: DataCacheConnector

  def view(id: Option[String], periodKey: Int, returnType: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        Future.successful(Ok(views.html.confirmDeleteDraft(new YesNoQuestionForm("client.agent-change.error").yesNoQuestionForm, id, periodKey,
          returnType, getBackLink(id, periodKey, returnType))))
      }
  }

  def submit(id: Option[String], periodKey: Int, returnType: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        val form = new YesNoQuestionForm("ated.delete-draft.error")
        form.yesNoQuestionForm.bindFromRequest.fold(
          formWithError =>
            Future.successful(BadRequest(views.html.confirmDeleteDraft(formWithError, id, periodKey, returnType, getBackLink(id, periodKey, returnType)))
            ),
          data => {
            val deleteDraft = data.yesNo.getOrElse(false)
            (deleteDraft, returnType) match {
              case (true, "charge") =>
                propertyDetailsService.clearDraftReliefs(id.getOrElse(throw new RuntimeException("No id found for draft return")))
                Future.successful(Redirect(controllers.routes.PeriodSummaryController.view(periodKey)))
              case (true, "relief") =>
                reliefsService.deleteDraftReliefs(periodKey)
                Future.successful(Redirect(controllers.routes.PeriodSummaryController.view(periodKey)))
              case (false, "charge") =>
                Future.successful(Redirect(controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(id.getOrElse(throw new RuntimeException("No id found for draft return")))))
              case (false, "relief") => Future.successful(Redirect(controllers.reliefs.routes.ReliefsSummaryController.view(periodKey)))
            }
          }
        )
      }
  }

  private def getBackLink(id: Option[String], periodKey: Int, returnType: String) = {
    returnType match {
      case "relief" => Some(controllers.reliefs.routes.ReliefsSummaryController.view(periodKey).url)
      case "charge" => Some(controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(id.getOrElse(throw new RuntimeException("No id found for draft return"))).url)
    }
  }
}

object DraftDeleteConfirmationController extends DraftDeleteConfirmationController {
  override val dataCacheConnector = DataCacheConnector
  override val propertyDetailsService = PropertyDetailsService
  override val reliefsService = ReliefsService
  val delegationConnector = FrontendDelegationConnector
}
