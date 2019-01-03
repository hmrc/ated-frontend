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
import controllers.BackLinkController
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import services.DisposeLiabilityReturnService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait DisposeLiabilityDeclarationController extends BackLinkController
  with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  def disposeLiabilityReturnService: DisposeLiabilityReturnService

  def view(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        currentBackLink.map(backLink =>
          Ok(views.html.editLiability.disposeLiabilityDeclaration(oldFormBundleNo, backLink))
        )
      }
  }

  def submit(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        disposeLiabilityReturnService.submitDraftDisposeLiability(oldFormBundleNo) map {
          response =>
            response.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo) match {
              case Some(resp) => Redirect(controllers.editLiability.routes.DisposeLiabilitySentController.view(oldFormBundleNo))
              case None => Redirect(controllers.routes.AccountSummaryController.view())
            }
        }
      }
  }

}

object DisposeLiabilityDeclarationController extends DisposeLiabilityDeclarationController {
  val delegationConnector = FrontendDelegationConnector
  val dataCacheConnector = DataCacheConnector
  val disposeLiabilityReturnService = DisposeLiabilityReturnService
  override val controllerId = "DisposeLiabilityDeclarationController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
