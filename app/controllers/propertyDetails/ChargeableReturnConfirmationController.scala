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

import connectors.DataCacheConnector
import controllers.AtedBaseController
import controllers.auth.AuthAction
import models.SubmitReturnsResponse
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, SubscriptionDataService}
import utils.AtedConstants._

trait ChargeableReturnConfirmationController extends AtedBaseController with AuthAction {

  def subscriptionDataService: SubscriptionDataService
  def dataCacheConnector: DataCacheConnector

  def confirmation : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      dataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](SubmitReturnsResponseFormId) map {
        case Some(submitResponse) =>
          Ok(views.html.propertyDetails.chargeableReturnsConfirmation(submitResponse))
        case None =>
          Logger.warn("[ChargeableReturnConfirmationController][confirmation] - Return Response not found in cache")
          Redirect(controllers.routes.AccountSummaryController.view())
      }
    }
  }

  def viewPrintFriendlyChargeableConfirmation : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      for {
        submitedResponse <- dataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](SubmitReturnsResponseFormId)
        organisationName <- subscriptionDataService.getOrganisationName
      } yield {
        Ok(views.html.propertyDetails.chargeableConfirmationPrintFriendly(submitedResponse, organisationName))
      }
    }
  }

}

object ChargeableReturnConfirmationController extends ChargeableReturnConfirmationController {
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val delegationService: DelegationService = DelegationService
  val subscriptionDataService: SubscriptionDataService = SubscriptionDataService
}
