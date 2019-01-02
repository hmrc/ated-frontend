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

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.{AtedBaseController, BackLinkController}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import forms.BankDetailForms.hasBankDetailsForm
import models.HasBankDetails
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.ChangeLiabilityReturnService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import utils.AtedUtils

import scala.concurrent.Future
trait HasBankDetailsController extends BackLinkController
  with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  def changeLiabilityReturnService: ChangeLiabilityReturnService

  def view(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
          case Some(x) =>
            currentBackLink.map { backLink =>
              val bankDetails = x.bankDetails.map(_.hasBankDetails)
              Ok(views.html.editLiability.hasBankDetails(hasBankDetailsForm.fill(HasBankDetails(bankDetails)), oldFormBundleNo, backLink))
            }
          case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
        }
      }
  }

  def editFromSummary(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
          case Some(x) =>
            Future.successful {
              val mode = AtedUtils.getEditSubmittedMode(x)
              val bankDetails = x.bankDetails.map(_.hasBankDetails)
              Ok(views.html.editLiability.hasBankDetails(hasBankDetailsForm.fill(HasBankDetails(bankDetails)), oldFormBundleNo,
                AtedUtils.getSummaryBackLink(oldFormBundleNo, mode)))
            }
          case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
        }
      }
  }

  def save(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        hasBankDetailsForm.bindFromRequest.fold(
          formWithErrors => currentBackLink.map(backLink => BadRequest(views.html.editLiability.hasBankDetails(formWithErrors, oldFormBundleNo, backLink))),
          bankData => {
            val hasBankDetails = bankData.hasBankDetails.getOrElse(false)
            val backLink = Some(controllers.editLiability.routes.HasBankDetailsController.view(oldFormBundleNo).url)
            changeLiabilityReturnService.cacheChangeLiabilityReturnHasBankDetails(oldFormBundleNo, hasBankDetails) flatMap {
              response => {
                if (hasBankDetails)
                  RedirectWithBackLink(
                    BankDetailsController.controllerId,
                    controllers.editLiability.routes.BankDetailsController.view(oldFormBundleNo),
                    backLink
                  )
                else
                  RedirectWithBackLink(
                    EditLiabilitySummaryController.controllerId,
                    controllers.editLiability.routes.EditLiabilitySummaryController.viewSummary(oldFormBundleNo),
                    backLink
                  )
              }
            }
          }
        )
      }
  }

}

object HasBankDetailsController extends HasBankDetailsController {
  val delegationConnector = FrontendDelegationConnector
  val changeLiabilityReturnService = ChangeLiabilityReturnService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "HasBankDetailsController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
