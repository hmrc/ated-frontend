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

package controllers.propertyDetails

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper, ExternalUrls}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}

import scala.concurrent.Future

trait PropertyDetailsDeclarationController extends PropertyDetailsHelpers with ClientHelper with AuthAction {

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(_) =>
            currentBackLink.map(backLink =>
              Ok(views.html.propertyDetails.propertyDetailsDeclaration(id, backLink))
            )
        }
      }
    }
  }

  def submit(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsService.submitDraftPropertyDetails(id) flatMap { response =>
          response.status match {
            case OK => Future.successful(Redirect(controllers.propertyDetails.routes.ChargeableReturnConfirmationController.confirmation()))
            case BAD_REQUEST if response.body.contains("Agent not Valid") =>
              Future.successful(BadRequest(views.html.global_error(Messages("ated.client-problem.title"),
                Messages("ated.client-problem.header"), Messages("ated.client-problem.body", ExternalUrls.agentRedirectedToMandate))))
          }
        }
      }
    }
  }
}

object PropertyDetailsDeclarationController extends PropertyDetailsDeclarationController {
  val delegationService: DelegationService = DelegationService
  val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsDeclarationController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
