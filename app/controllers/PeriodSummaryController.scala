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

package controllers

import config.FrontendDelegationConnector
import connectors.BackLinkCacheConnector
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime}
import controllers.editLiability.DisposePropertyController
import controllers.propertyDetails.{AddressLookupController, PropertyDetailsAddressController, PropertyDetailsSummaryController}
import controllers.reliefs.{ChooseReliefsController, ReliefsSummaryController}
import services.{SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

trait PeriodSummaryController extends BackLinkController with AtedFrontendAuthHelpers with DelegationAwareActions {

  def summaryReturnsService: SummaryReturnsService
  def subscriptionDataService: SubscriptionDataService

  def view(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      for {
        periodSummaries <- summaryReturnsService.getPeriodSummaryReturns(periodKey)
        organisationName <- subscriptionDataService.getOrganisationName
      } yield {
        Ok(views.html.periodSummary(periodKey, periodSummaries,organisationName, getBackLink))
      }
  }

  def viewPastReturns(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      for {
        periodSummaries <- summaryReturnsService.getPeriodSummaryReturns(periodKey)
        organisationName <- subscriptionDataService.getOrganisationName
      } yield {
        Ok(views.html.periodSummaryPastReturns(periodKey, periodSummaries,organisationName, getBackLink))
      }
  }

  def createReturn(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      RedirectWithBackLink(ReturnTypeController.controllerId,
        routes.ReturnTypeController.view(periodKey),
        Some(routes.PeriodSummaryController.view(periodKey).url)
      )
  }

  def viewReturn(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      RedirectWithBackLink(ReliefsSummaryController.controllerId,
        controllers.reliefs.routes.ReliefsSummaryController.view(periodKey),
        Some(routes.PeriodSummaryController.view(periodKey).url)
      )
  }

  def viewChargeable(periodKey: Int, propertyKey: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      RedirectWithBackLink(PropertyDetailsSummaryController.controllerId,
        controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(propertyKey),
        Some(routes.PeriodSummaryController.view(periodKey).url),
        List(AddressLookupController.controllerId)
      )
  }

  def viewChargeableEdit(periodKey: Int, propertyKey: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      RedirectWithBackLink(PropertyDetailsSummaryController.controllerId,
        controllers.editLiability.routes.EditLiabilitySummaryController.view(propertyKey),
        Some(routes.PeriodSummaryController.view(periodKey).url),
        List(AddressLookupController.controllerId)
      )
  }

  def viewDisposal(periodKey: Int, propertyKey: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      RedirectWithBackLink(DisposePropertyController.controllerId,
        controllers.editLiability.routes.DisposeLiabilitySummaryController.view(propertyKey),
        Some(routes.PeriodSummaryController.view(periodKey).url)
      )
  }

  private def getBackLink() = {
    Some(routes.AccountSummaryController.view().url)
  }
}

object PeriodSummaryController extends PeriodSummaryController {
  val delegationConnector = FrontendDelegationConnector
  val summaryReturnsService = SummaryReturnsService
  val subscriptionDataService = SubscriptionDataService
  override val controllerId = "PeriodSummaryController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
