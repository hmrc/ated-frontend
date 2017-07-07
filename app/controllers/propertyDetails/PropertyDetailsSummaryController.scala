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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper, ExternalUrls}
import models.PropertyDetails
import org.joda.time.LocalDate
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, SubscriptionDataService}
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import utils.{AtedUtils, PeriodUtils}

import scala.concurrent.Future

trait PropertyDetailsSummaryController extends BackLinkController
  with AtedFrontendAuthHelpers with DelegationAwareActions with PropertyDetailsHelpers with ClientHelper {

  val propertyDetailsService: PropertyDetailsService

  def subscriptionDataService: SubscriptionDataService

  def view(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
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

  def submit(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext(RedirectWithBackLink(
        PropertyDetailsDeclarationController.controllerId,
        controllers.propertyDetails.routes.PropertyDetailsDeclarationController.view(id),
        Some(controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(id).url)
      ))
  }


  def viewPrintFriendlyLiabilityReturn(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        for {
          calculateDraft <- propertyDetailsService.calculateDraftPropertyDetails(id)
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


  def deleteDraft(id: String, periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        Future.successful(Redirect(controllers.routes.DraftDeleteConfirmationController.view(Some(id), periodKey, "charge")))
      }
  }
}

object PropertyDetailsSummaryController extends PropertyDetailsSummaryController {
  override val propertyDetailsService = PropertyDetailsService
  val delegationConnector = FrontendDelegationConnector
  val subscriptionDataService = SubscriptionDataService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsSummaryController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
