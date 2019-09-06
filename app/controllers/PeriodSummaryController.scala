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

package controllers

import connectors.BackLinkCacheConnector
import controllers.auth.AuthAction
import controllers.editLiability.DisposePropertyController
import controllers.propertyDetails.{AddressLookupController, PropertyDetailsSummaryController}
import controllers.reliefs.ReliefsSummaryController
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, SubscriptionDataService, SummaryReturnsService}

trait PeriodSummaryController extends BackLinkController with AuthAction {

  def summaryReturnsService: SummaryReturnsService
  def subscriptionDataService: SubscriptionDataService

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      for {
        periodSummaries <- summaryReturnsService.getPeriodSummaryReturns(periodKey)
        organisationName <- subscriptionDataService.getOrganisationName
      } yield {
        Ok(views.html.periodSummary(periodKey, periodSummaries, organisationName, getBackLink))
      }
    }
  }

  def viewPastReturns(periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      for {
        periodSummaries <- summaryReturnsService.getPeriodSummaryReturns(periodKey)
        organisationName <- subscriptionDataService.getOrganisationName
      } yield {
        Ok(views.html.periodSummaryPastReturns(periodKey, periodSummaries, organisationName, getBackLink))
      }
    }
  }

  def createReturn(periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      RedirectWithBackLink(ReturnTypeController.controllerId,
        routes.ReturnTypeController.view(periodKey),
        Some(routes.PeriodSummaryController.view(periodKey).url)
      )
    }
  }

  def viewReturn(periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      RedirectWithBackLink(ReliefsSummaryController.controllerId,
        controllers.reliefs.routes.ReliefsSummaryController.view(periodKey),
        Some(routes.PeriodSummaryController.view(periodKey).url)
      )
    }
  }

  def viewChargeable(periodKey: Int, propertyKey: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      RedirectWithBackLink(PropertyDetailsSummaryController.controllerId,
        controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(propertyKey),
        Some(routes.PeriodSummaryController.view(periodKey).url),
        List(AddressLookupController.controllerId)
      )
    }
  }

  def viewChargeableEdit(periodKey: Int, propertyKey: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      RedirectWithBackLink(PropertyDetailsSummaryController.controllerId,
        controllers.editLiability.routes.EditLiabilitySummaryController.view(propertyKey),
        Some(routes.PeriodSummaryController.view(periodKey).url),
        List(AddressLookupController.controllerId)
      )
    }
  }

  def viewDisposal(periodKey: Int, propertyKey: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      RedirectWithBackLink(DisposePropertyController.controllerId,
        controllers.editLiability.routes.DisposeLiabilitySummaryController.view(propertyKey),
        Some(routes.PeriodSummaryController.view(periodKey).url)
      )
    }
  }

  private def getBackLink() = {
    Some(routes.AccountSummaryController.view().url)
  }
}

object PeriodSummaryController extends PeriodSummaryController {
  val delegationService: DelegationService = DelegationService
  val summaryReturnsService: SummaryReturnsService = SummaryReturnsService
  val subscriptionDataService: SubscriptionDataService = SubscriptionDataService
  override val controllerId: String = "PeriodSummaryController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
