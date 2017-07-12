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

package controllers.propertyDetails

import config.FrontendDelegationConnector
import connectors.DataCacheConnector
import controllers.AtedBaseController
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime}
import models.SubmitReturnsResponse
import play.api.Logger
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import utils.AtedConstants._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import services.SubscriptionDataService

trait ChargeableReturnConfirmationController extends AtedBaseController with AtedFrontendAuthHelpers with DelegationAwareActions {

  def subscriptionDataService: SubscriptionDataService
  def dataCacheConnector: DataCacheConnector

  def confirmation = AuthAction(AtedRegime) {
    implicit atedContext =>
        dataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](SubmitReturnsResponseFormId) map {
          case Some(submitResponse) =>
            Ok(views.html.propertyDetails.chargeableReturnsConfirmation(submitResponse))
          case None =>
            Logger.warn("[ChargeableReturnConfirmationController][confirmation] - Return Response not found in cache")
            Redirect(controllers.routes.AccountSummaryController.view)
        }

  }


  def viewPrintFriendlyChargeableConfirmation = AuthAction(AtedRegime) {
    implicit atedContext =>
      for {
        submitedResponse <- dataCacheConnector.fetchClientData[SubmitReturnsResponse](SubmitReturnsResponseFormId)
        organisationName <- subscriptionDataService.getOrganisationName
      } yield {
        Ok(views.html.propertyDetails.chargeableConfirmationPrintFriendly(submitedResponse, organisationName))
      }

  }

}

object ChargeableReturnConfirmationController extends ChargeableReturnConfirmationController {
  override val dataCacheConnector = DataCacheConnector
  val delegationConnector = FrontendDelegationConnector
  val subscriptionDataService = SubscriptionDataService
}
