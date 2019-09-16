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

import connectors.{AgentClientMandateFrontendConnector, DataCacheConnector}
import controllers.auth.AuthAction
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, DetailsService, SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http.ForbiddenException

import scala.concurrent.ExecutionContext.Implicits.global

trait AccountSummaryController extends AtedBaseController with AuthAction {

  def summaryReturnsService: SummaryReturnsService

  def subscriptionDataService: SubscriptionDataService

  def mandateFrontendConnector: AgentClientMandateFrontendConnector

  def detailsService: DetailsService

  def dataCacheConnector: DataCacheConnector


  def view(): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      for {
        _ <- dataCacheConnector.clearCache()
        allReturns <- summaryReturnsService.getSummaryReturns
        _ <- detailsService.cacheClientReference(authContext.atedReferenceNumber)
        correspondenceAddress <- subscriptionDataService.getCorrespondenceAddress
        organisationName <- subscriptionDataService.getOrganisationName
        safeId <- subscriptionDataService.getSafeId
        clientBannerPartial <- mandateFrontendConnector.getClientBannerPartial(safeId.getOrElse(throw new RuntimeException("Could not get safeId")), "ated")
      } yield {
        Ok(views.html.accountSummary(allReturns, correspondenceAddress, organisationName, clientBannerPartial.successfulContentOrEmpty))
      }
    } recover {
      case fe: ForbiddenException     =>
        Logger.warn("[AccountSummaryController][view] Forbidden exception")
        unauthorisedUrl()
    }
  }

}

object AccountSummaryController extends AccountSummaryController {
  val delegationService: DelegationService = DelegationService
  val summaryReturnsService: SummaryReturnsService = SummaryReturnsService
  val subscriptionDataService: SubscriptionDataService = SubscriptionDataService
  val mandateFrontendConnector: AgentClientMandateFrontendConnector = AgentClientMandateFrontendConnector
  val detailsService: DetailsService = DetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
