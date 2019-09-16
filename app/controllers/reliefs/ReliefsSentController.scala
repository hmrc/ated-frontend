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

import connectors.DataCacheConnector
import controllers.AtedBaseController
import controllers.auth.AuthAction
import models.SubmitReturnsResponse
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, ReliefsService, SubscriptionDataService}
import utils.AtedConstants._

trait ReliefsSentController extends AtedBaseController with AuthAction {

  def subscriptionDataService: SubscriptionDataService
  def dataCacheConnector: DataCacheConnector
  def reliefsService: ReliefsService

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      dataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](SubmitReturnsResponseFormId) map {
        case Some(submitResponse) =>
          Ok(views.html.reliefs.reliefsSent(periodKey, submitResponse))
        case None =>
          Redirect(controllers.reliefs.routes.ReliefDeclarationController.view(periodKey))
      }
    }
  }

  def viewPrintFriendlyReliefSent(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      for {
        submitedResponse <- dataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](SubmitReturnsResponseFormId)
        organisationName <- subscriptionDataService.getOrganisationName
      } yield {
        Ok(views.html.reliefs.reliefsSentPrintFriendly(periodKey, submitedResponse, organisationName))
      }
    }
  }

}

object ReliefsSentController extends ReliefsSentController {
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val reliefsService: ReliefsService = ReliefsService
  val delegationService: DelegationService = DelegationService
  val subscriptionDataService: SubscriptionDataService = SubscriptionDataService
}
