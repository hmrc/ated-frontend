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
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import models.PropertyDetails
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService, SubscriptionDataService}
import utils.{AtedUtils, PeriodUtils}

import scala.concurrent.Future

trait PropertyDetailsSummaryController extends BackLinkController with PropertyDetailsHelpers with ClientHelper with AuthAction {

  val propertyDetailsService: PropertyDetailsService

  def subscriptionDataService: SubscriptionDataService

  def view(propertyKey: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(propertyKey) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap(backLink =>
              Future.successful(Ok(views.html.propertyDetails.propertyDetailsSummary(propertyDetails,
                PeriodUtils.getDisplayPeriods(propertyDetails.period),
                AtedUtils.canSubmit(propertyDetails.periodKey, LocalDate.now),
                PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated),
                backLink)
              )))
        }
      }
    }
  }

  def submit(propertyKey: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext(RedirectWithBackLink(
        PropertyDetailsDeclarationController.controllerId,
        controllers.propertyDetails.routes.PropertyDetailsDeclarationController.view(propertyKey),
        Some(controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(propertyKey).url)
      ))
    }
  }


  def viewPrintFriendlyLiabilityReturn(propertyKey: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          calculateDraft <- propertyDetailsService.calculateDraftPropertyDetails(propertyKey)
          organisationName <- subscriptionDataService.getOrganisationName
        }
          yield {
            val propertyDetails = calculateDraft.json.as[PropertyDetails]
            Ok(views.html.propertyDetails.propertyDetailsPrintFriendly(propertyDetails,
              PeriodUtils.getDisplayPeriods(propertyDetails.period),
              PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated),
              organisationName
            ))
          }
      }
    }
  }


  def deleteDraft(propertyKey: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        Future.successful(Redirect(controllers.routes.DraftDeleteConfirmationController.view(Some(propertyKey), periodKey, "charge")))
      }
    }
  }
}

object PropertyDetailsSummaryController extends PropertyDetailsSummaryController {
  override val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val delegationService: DelegationService = DelegationService
  val subscriptionDataService: SubscriptionDataService = SubscriptionDataService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsSummaryController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
