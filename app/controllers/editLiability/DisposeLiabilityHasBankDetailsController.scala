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

package controllers.editLiability

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.{AtedBaseController, BackLinkController}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import forms.BankDetailForms.hasBankDetailsForm
import models.HasBankDetails
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.DisposeLiabilityReturnService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions

import scala.concurrent.Future

trait DisposeLiabilityHasBankDetailsController extends BackLinkController
  with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  def disposeLiabilityReturnService: DisposeLiabilityReturnService

  def view(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo) flatMap {
          case Some(x) =>
            currentBackLink.map { backLink =>
              val bankDetails = x.bankDetails.map(_.hasBankDetails)
              Ok(views.html.editLiability.disposeLiabilityHasBankDetails(hasBankDetailsForm.fill(HasBankDetails(bankDetails)), oldFormBundleNo, backLink))
            }
          case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
        }
      }
  }

  def editFromSummary(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo) flatMap {
          case Some(x) =>
            Future.successful {
              val bankDetails = x.bankDetails.map(_.hasBankDetails)
              val backLink = Some(controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo).url)
              Ok(views.html.editLiability.disposeLiabilityHasBankDetails(hasBankDetailsForm.fill(HasBankDetails(bankDetails)), oldFormBundleNo, backLink))
            }
          case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
        }
      }
  }

  def save(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        hasBankDetailsForm.bindFromRequest.fold(
          formWithErrors =>
            currentBackLink.map(backLink =>
              BadRequest(views.html.editLiability.disposeLiabilityHasBankDetails(formWithErrors, oldFormBundleNo, backLink))
            ),
          bankData => {
            val hasBankDetails = bankData.hasBankDetails.getOrElse(false)
            val backLink = Some(controllers.editLiability.routes.DisposeLiabilityHasBankDetailsController.view(oldFormBundleNo).url)
            disposeLiabilityReturnService.cacheDisposeLiabilityReturnHasBankDetails(oldFormBundleNo, hasBankDetails) flatMap {
              response => {
                if (hasBankDetails) {
                  disposeLiabilityReturnService.calculateDraftDisposal(oldFormBundleNo) flatMap {
                    case Some(x) =>
                      RedirectWithBackLink(
                        DisposeLiabilityBankDetailsController.controllerId,
                        controllers.editLiability.routes.DisposeLiabilityBankDetailsController.view(oldFormBundleNo),
                        backLink
                      )
                    case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
                  }
                }
                else
                  RedirectWithBackLink(
                    DisposeLiabilitySummaryController.controllerId,
                    controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo),
                    backLink
                  )
              }
            }
          }
        )
      }
  }

}

object DisposeLiabilityHasBankDetailsController extends DisposeLiabilityHasBankDetailsController {
  val delegationConnector = FrontendDelegationConnector
  val disposeLiabilityReturnService = DisposeLiabilityReturnService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "DisposeLiabilityDeclarationController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
