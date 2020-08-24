/*
 * Copyright 2020 HM Revenue & Customs
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
import org.joda.time.LocalDate
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.ForbiddenException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.PeriodUtils

import scala.concurrent.ExecutionContext.Implicits.global

class PrevPeriodsSummaryController @Inject()(mcc: MessagesControllerComponents,
                                             authAction: AuthAction,
                                             summaryReturnsService: SummaryReturnsService,
                                             subscriptionDataService: SubscriptionDataService,
                                             mandateFrontendConnector: AgentClientMandateFrontendConnector,
                                             detailsService: DetailsService,
                                             dataCacheConnector: DataCacheConnector,
                                             serviceInfoService: ServiceInfoService,
                                             dateService: DateService,
                                             template: views.html.prevPeriodsSummary)
                                            (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with Logging {

  def view(): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        _ <- dataCacheConnector.clearCache()
        allReturns <- summaryReturnsService.getSummaryReturns
        previousReturns = allReturns.returnsOtherTaxYears.filterNot(_.periodKey.equals(PeriodUtils.calculatePeakStartYear() + 1))
        _ <- detailsService.cacheClientReference(authContext.atedReferenceNumber)
        correspondenceAddress <- subscriptionDataService.getCorrespondenceAddress
        organisationName <- subscriptionDataService.getOrganisationName
        safeId <- subscriptionDataService.getSafeId
        serviceInfoContent <- serviceInfoService.getPartial
        clientBannerPartial <- mandateFrontendConnector.getClientBannerPartial(safeId.getOrElse(
          throw new RuntimeException("Could not get safeId")), "ated"
        )
      } yield {
        Ok(template(
          allReturns.copy(returnsOtherTaxYears = previousReturns),
          correspondenceAddress,
          organisationName,
          serviceInfoContent,
          clientBannerPartial.successfulContentOrEmpty,
          duringPeak,
          currentYear = dateService.now().getYear,
          taxYearStartingYear = PeriodUtils.calculatePeakStartYear(dateService.now()),
          Some(controllers.routes.AccountSummaryController.view().url)
        ))
      }
    } recover {
      case _: ForbiddenException     =>
        logger.warn("[AccountSummaryController][view] Forbidden exception")
        authAction.unauthorisedUrl()
    }
  }

  def duringPeak: Boolean = {
    val currentDate = dateService.now()
    val currentYear = currentDate.getYear

    val peakStartDate = LocalDate.parse(
      s"""$currentYear-03-${appConfig.atedPeakStartDay}"""
    )

    val peakEndDate = LocalDate.parse(s"""$currentYear-04-30""")

    !currentDate.isBefore(peakStartDate) && !currentDate.isAfter(peakEndDate)

  }

}
