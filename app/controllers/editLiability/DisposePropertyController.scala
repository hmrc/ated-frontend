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

package controllers.editLiability

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import forms.AtedForms.disposeLiabilityForm
import models.DisposeLiability
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, DisposeLiabilityReturnService}

import scala.concurrent.Future

trait DisposePropertyController extends BackLinkController with ClientHelper with AuthAction {

  def disposeLiabilityReturnService: DisposeLiabilityReturnService

  def view(oldFormBundleNo: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          disposeLiabilityOpt <- disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo)
          result <-
          disposeLiabilityOpt match {
            case Some(x) =>
              currentBackLink.map { backLink =>
                val filledForm = disposeLiabilityForm.fill(x.disposeLiability.fold(DisposeLiability(periodKey = x.formBundleReturn.periodKey.toInt))(a => a))
                Ok(views.html.editLiability.dataOfDisposal(filledForm, oldFormBundleNo, backLink))
              }
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
          }
        } yield result
      }
    }
  }

  def editFromSummary(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          disposeLiabilityOpt <- disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo)
          result <-
          disposeLiabilityOpt.flatMap(_.disposeLiability) match {
            case Some(x) =>
              Future.successful {
                val backLink = Some(controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo).url)
                Ok(views.html.editLiability.dataOfDisposal(disposeLiabilityForm.fill(x), oldFormBundleNo, backLink))
              }
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
          }
        } yield result
      }
    }
  }

  def save(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        disposeLiabilityForm.bindFromRequest.fold(
          formWithErrors => {
            currentBackLink.map(backLink => BadRequest(views.html.editLiability.dataOfDisposal(formWithErrors, oldFormBundleNo, backLink)))
          },
          disposalDate => disposeLiabilityReturnService.cacheDisposeLiabilityReturnDate(oldFormBundleNo, disposalDate) flatMap {
            response =>
              RedirectWithBackLink(
                DisposeLiabilityHasBankDetailsController.controllerId,
                controllers.editLiability.routes.DisposeLiabilityHasBankDetailsController.view(oldFormBundleNo),
                Some(controllers.editLiability.routes.DisposePropertyController.view(oldFormBundleNo).url)
              )
          }
        )
      }
    }
  }
}

object DisposePropertyController extends DisposePropertyController {
  val delegationService: DelegationService = DelegationService
  val disposeLiabilityReturnService : DisposeLiabilityReturnService = DisposeLiabilityReturnService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId: String = "DisposePropertyController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
