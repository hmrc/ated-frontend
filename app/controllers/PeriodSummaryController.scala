/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.auth.AuthAction

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, BackLinkService, ServiceInfoService, SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.PeriodUtils

import scala.concurrent.ExecutionContext

class PeriodSummaryController @Inject()(mcc: MessagesControllerComponents,
                                        authAction: AuthAction,
                                        summaryReturnsService: SummaryReturnsService,
                                        subscriptionDataService: SubscriptionDataService,
                                        serviceInfoService: ServiceInfoService,
                                        val backLinkCacheService: BackLinkCacheService,
                                        template: views.html.periodSummary)
                                       (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkService with ControllerIds {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PeriodSummaryController"

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        periodSummaries <- summaryReturnsService.getPeriodSummaryReturns(periodKey)
        organisationName <- subscriptionDataService.getOrganisationName
        serviceInfoContent <- serviceInfoService.getPartial
      } yield {
        val currentSummaries = periodSummaries.map(summaryReturnsService.filterPeriodSummaryReturnReliefs(_, past = false))
        val previousSummaries = periodSummaries.map(summaryReturnsService.filterPeriodSummaryReturnReliefs(_, past = true))
        Ok(template(periodKey, currentSummaries,previousSummaries, organisationName, serviceInfoContent, getBackLink(periodKey)))
      }
    }
  }

  def createReturn(periodKey: Int, fromAccountSummary: Boolean = false): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { _ =>
      val backLink = if (!fromAccountSummary) {
        routes.PeriodSummaryController.view(periodKey).url
      } else {
        routes.AccountSummaryController.view().url
      }

      redirectWithBackLink(returnTypeControllerId,
        routes.ReturnTypeController.view(periodKey),
        Some(backLink)
      )
    }
  }

  def viewReturn(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { _ =>
      redirectWithBackLink(reliefsSummaryControllerId,
        controllers.reliefs.routes.ReliefsSummaryController.view(periodKey),
        Some(routes.PeriodSummaryController.view(periodKey).url)
      )
    }
  }

  def viewChargeable(periodKey: Int, propertyKey: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { _ =>
      redirectWithBackLink(propertyDetailsSummaryControllerId,
        controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(propertyKey),
        Some(routes.PeriodSummaryController.view(periodKey).url),
        List(addressLookupId)
      )
    }
  }

  def viewChargeableEdit(periodKey: Int, propertyKey: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { _ =>
      redirectWithBackLink(propertyDetailsSummaryControllerId,
        controllers.editLiability.routes.EditLiabilitySummaryController.view(propertyKey),
        Some(routes.PeriodSummaryController.view(periodKey).url),
        List(addressLookupId)
      )
    }
  }

  def viewDisposal(periodKey: Int, propertyKey: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { _ =>
      redirectWithBackLink(disposePropertyControllerId,
        controllers.editLiability.routes.DisposeLiabilitySummaryController.view(propertyKey),
        Some(routes.PeriodSummaryController.view(periodKey).url)
      )
    }
  }

  private def getBackLink(periodKey: Int): Some[String] = {
    if (periodKey.equals(PeriodUtils.calculatePeakStartYear())) {
      Some(routes.AccountSummaryController.view().url)
    } else {
      Some(routes.PrevPeriodsSummaryController.view().url)
    }
  }
}
