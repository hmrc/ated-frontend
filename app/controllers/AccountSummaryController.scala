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

import config.ApplicationConfig
import connectors.{AgentClientMandateFrontendConnector, DataCacheConnector}
import controllers.auth.AuthAction
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DetailsService, SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.http.ForbiddenException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global

class AccountSummaryController @Inject()(mcc: MessagesControllerComponents,
                                         authAction: AuthAction,
                                         summaryReturnsService: SummaryReturnsService,
                                         subscriptionDataService: SubscriptionDataService,
                                         mandateFrontendConnector: AgentClientMandateFrontendConnector,
                                         detailsService: DetailsService,
                                         dataCacheConnector: DataCacheConnector)
                                        (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) {

  def view(): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
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
      case _: ForbiddenException     =>
        Logger.warn("[AccountSummaryController][view] Forbidden exception")
        authAction.unauthorisedUrl()
    }
  }
}
