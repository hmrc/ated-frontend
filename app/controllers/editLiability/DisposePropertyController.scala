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
import forms.AtedForms
import forms.AtedForms.disposeLiabilityForm
import models.DisposeLiability
import play.api.Logger
import services.DisposeLiabilityReturnService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait DisposePropertyController extends BackLinkController
  with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  def disposeLiabilityReturnService: DisposeLiabilityReturnService

  def view(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
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

  def editFromSummary(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
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

  def save(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        AtedForms.validateDisposedProperty(disposeLiabilityForm.bindFromRequest).fold(
          formWithErrors =>
            currentBackLink.map(backLink => BadRequest(views.html.editLiability.dataOfDisposal(formWithErrors, oldFormBundleNo, backLink))),
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

object DisposePropertyController extends DisposePropertyController {
  val delegationConnector = FrontendDelegationConnector
  val disposeLiabilityReturnService = DisposeLiabilityReturnService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "DisposePropertyController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
