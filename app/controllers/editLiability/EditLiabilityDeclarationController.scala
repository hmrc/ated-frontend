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
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{ChangeLiabilityReturnService, DelegationService}

import scala.concurrent.Future

trait EditLiabilityDeclarationController extends BackLinkController with ClientHelper with AuthAction {

  def changeLiabilityReturnService: ChangeLiabilityReturnService

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
          case Some(x) =>
            val returnType = getReturnType(x.calculated.flatMap(_.amountDueOrRefund))
            currentBackLink.map(backLink =>
              Ok(views.html.editLiability.editLiabilityDeclaration(oldFormBundleNo, returnType, backLink))
            )
          case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
        }
      }
    }
  }

  def submit(oldFormBundleNo: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        changeLiabilityReturnService.submitDraftChangeLiability(oldFormBundleNo) map {
          response =>
            response.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo) match {
              case Some(resp) =>
                Redirect(controllers.editLiability.routes.EditLiabilitySentController.view(oldFormBundleNo))
              case None =>
                Redirect(controllers.routes.AccountSummaryController.view())
            }
        }
      }
    }
  }

  private def getReturnType(amountDueOrRefund: Option[BigDecimal]) = {
    amountDueOrRefund.fold("C")(a => if (a > 0) "F" else if (a < 0) "A" else "C")
  }
}

object EditLiabilityDeclarationController extends EditLiabilityDeclarationController {
  val delegationService: DelegationService = DelegationService
  val changeLiabilityReturnService: ChangeLiabilityReturnService = ChangeLiabilityReturnService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId = "EditLiabilityDeclarationController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
