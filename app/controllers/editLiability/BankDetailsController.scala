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
import forms.BankDetailForms
import forms.BankDetailForms.bankDetailsForm
import models.BankDetails
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{ChangeLiabilityReturnService, DelegationService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BankDetailsController extends BackLinkController with ClientHelper with AuthAction {

  def changeLiabilityReturnService: ChangeLiabilityReturnService

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
        ensureClientContext {
          changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
            case Some(x) =>
              currentBackLink.map { backLink =>
                val bankDetails = x.bankDetails.flatMap(_.bankDetails).fold(BankDetails())(a => a)
                Ok(views.html.editLiability.bankDetails(bankDetailsForm.fill(bankDetails), oldFormBundleNo, backLink))
              }
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
          }
        }
      }
  }

  def save(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        BankDetailForms.validateBankDetails(bankDetailsForm.bindFromRequest).fold(
          formWithErrors =>
            currentBackLink.map(backLink => BadRequest(views.html.editLiability.bankDetails(formWithErrors, oldFormBundleNo, backLink))),
          bankData => {
            changeLiabilityReturnService.cacheChangeLiabilityReturnBank(oldFormBundleNo, bankData) flatMap { _ => {
              RedirectWithBackLink(
                EditLiabilitySummaryController.controllerId,
                controllers.editLiability.routes.EditLiabilitySummaryController.viewSummary(oldFormBundleNo),
                Some(controllers.editLiability.routes.BankDetailsController.view(oldFormBundleNo).url)
              )
            }
            }
          }
        )
      }
    }
  }
}

object BankDetailsController extends BankDetailsController {
  val delegationService: DelegationService = DelegationService
  val changeLiabilityReturnService: ChangeLiabilityReturnService = ChangeLiabilityReturnService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId: String = "BankDetailsController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
