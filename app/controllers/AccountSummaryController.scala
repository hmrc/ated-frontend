/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.{AgentClientMandateFrontendConnector, DataCacheConnector}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.{DetailsService, SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions

trait AccountSummaryController extends AtedBaseController with AtedFrontendAuthHelpers with DelegationAwareActions {

  def summaryReturnsService: SummaryReturnsService

  def subscriptionDataService: SubscriptionDataService

  def mandateFrontendConnector: AgentClientMandateFrontendConnector

  def detailsService: DetailsService

  def dataCacheConnector: DataCacheConnector


  def view() = AuthAction(AtedRegime) {
    implicit atedContext =>
      for {
        _ <- dataCacheConnector.clearCache()
        allReturns <- summaryReturnsService.getSummaryReturns
        _ <- detailsService.cacheClientReference(atedContext.user.atedReferenceNumber)
        correspondenceAddress <- subscriptionDataService.getCorrespondenceAddress
        organisationName <- subscriptionDataService.getOrganisationName
        safeId <- subscriptionDataService.getSafeId
        clientBannerPartial <- mandateFrontendConnector.getClientBannerPartial(safeId.getOrElse(throw new RuntimeException("Could not get safeId")), "ATED")
      } yield {
        Ok(views.html.accountSummary(allReturns, correspondenceAddress, organisationName, clientBannerPartial.successfulContentOrEmpty))
      }
  }

}

object AccountSummaryController extends AccountSummaryController {
  val delegationConnector = FrontendDelegationConnector
  val summaryReturnsService = SummaryReturnsService
  val subscriptionDataService = SubscriptionDataService
  val mandateFrontendConnector = AgentClientMandateFrontendConnector
  val detailsService = DetailsService
  val dataCacheConnector = DataCacheConnector
}
