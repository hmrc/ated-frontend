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

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper, ExternalUrls}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import services.ReliefsService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions

import scala.concurrent.Future


trait ReliefDeclarationController extends BackLinkController
  with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  def reliefsService: ReliefsService

  def view(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        currentBackLink.flatMap(
          backLink =>
            Future.successful(Ok(views.html.reliefs.reliefDeclaration(periodKey, backLink)))
        )
      }
  }

  def submit(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        reliefsService.submitDraftReliefs(atedContext.user.atedReferenceNumber, periodKey) flatMap { response =>
          response.status match {
            case OK => Future.successful(Redirect(controllers.reliefs.routes.ReliefsSentController.view(periodKey)))
            case BAD_REQUEST if(response.body.contains("Agent not Valid")) =>
              Future.successful(BadRequest(views.html.global_error(Messages("ated.client-problem.title"),
                Messages("ated.client-problem.header"), Messages("ated.client-problem.body", ExternalUrls.agentRedirectedToMandate))))
          }
        }
      }
  }

}

object ReliefDeclarationController extends ReliefDeclarationController {
  val reliefsService = ReliefsService
  val delegationConnector = FrontendDelegationConnector
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "ReliefDeclarationController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
