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

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper, ExternalUrls}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, ReliefsService}

import scala.concurrent.Future


trait ReliefDeclarationController extends BackLinkController
   with ClientHelper with AuthAction {

  def reliefsService: ReliefsService

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        currentBackLink.flatMap(
          backLink =>
            Future.successful(Ok(views.html.reliefs.reliefDeclaration(periodKey, backLink)))
        )
      }
    }
  }

  def submit(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        reliefsService.submitDraftReliefs(authContext.atedReferenceNumber.get, periodKey) flatMap { response =>
          response.status match {
            case OK => Future.successful(Redirect(controllers.reliefs.routes.ReliefsSentController.view(periodKey)))
            case BAD_REQUEST if (response.body.contains("Agent not Valid")) =>
              Future.successful(BadRequest(views.html.global_error(Messages("ated.client-problem.title"),
                Messages("ated.client-problem.header"), Messages("ated.client-problem.body", ExternalUrls.agentRedirectedToMandate))))
          }
        }
      }
    }
  }

}

object ReliefDeclarationController extends ReliefDeclarationController {
  val reliefsService: ReliefsService = ReliefsService
  val delegationService: DelegationService = DelegationService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId: String = "ReliefDeclarationController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
