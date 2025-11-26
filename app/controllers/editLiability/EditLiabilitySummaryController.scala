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

package controllers.editLiability

import config.ApplicationConfig
import controllers.auth.{AuthAction, ClientHelper}
import controllers.ControllerIds
import javax.inject.Inject
import models.{PropertyDetails, StandardAuthRetrievals}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.{BackLinkCacheService, BackLinkService, DataCacheService, PropertyDetailsService, ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.PeriodUtils

import scala.concurrent.{ExecutionContext, Future}

class EditLiabilitySummaryController @Inject()(mcc: MessagesControllerComponents,
                                               propertyDetailsService: PropertyDetailsService,
                                               subscriptionDataService: SubscriptionDataService,
                                               authAction: AuthAction,
                                               serviceInfoService: ServiceInfoService,
                                               val dataCacheService: DataCacheService,
                                               val backLinkCacheService: BackLinkCacheService,
                                               template: views.html.editLiability.editLiabilitySummary)
                                              (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkService with ClientHelper with I18nSupport with ControllerIds {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = editLiabilitySummaryId

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsService.calculateDraftChangeLiability(oldFormBundleNo).flatMap { x =>
          x.fold(
            Future.successful(
              Redirect(controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(oldFormBundleNo)))
          )
          {
            propertyDetails =>
              propertyDetails.calculated.flatMap(_.amountDueOrRefund) match {
                case Some(amount) if amount < 0 =>
                  forwardBackLinkToNextPage(
                    hasBankDetailsId, controllers.editLiability.routes.HasBankDetailsController.view(oldFormBundleNo)
                  )
                case Some(_) => viewSummaryDetails(propertyDetails)
                case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
              }
          }
        }
      }
    }
  }

  def viewSummary(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsService.calculateDraftChangeLiability(oldFormBundleNo).flatMap(
          x => x.fold(
            Future.successful(
              Redirect(controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(oldFormBundleNo))
            )
          )
          (y => viewSummaryDetails(y))
        )
      }
    }
  }

  private def viewSummaryDetails(propertyDetails: PropertyDetails)
                                (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, request: Request[AnyContent]) = {
    serviceInfoService.getPartial.flatMap { serviceInfoContent =>
      currentBackLink.map(
        backLink =>
          Ok(template(propertyDetails,
            getReturnType(propertyDetails),
            PeriodUtils.getDisplayPeriods(propertyDetails.period, propertyDetails.periodKey),
            PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated),
            serviceInfoContent,
            backLink))
      )
    }
  }

  private def getReturnType(propertyDetails: PropertyDetails) = {
    propertyDetails.calculated.flatMap(_.amountDueOrRefund).fold("C")(a => if (a > 0) "F" else if (a < 0) "A" else "C")
  }

  def submit(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        redirectWithBackLink(
          editLiabilityDeclarationId,
          controllers.editLiability.routes.EditLiabilityDeclarationController.view(oldFormBundleNo),
          Some(controllers.editLiability.routes.EditLiabilitySummaryController.viewSummary(oldFormBundleNo).url)
        )
      }
    }
  }

  def viewPrintFriendlyEditLiabilityReturn(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          calculateDraftLiability <- propertyDetailsService.calculateDraftChangeLiability(oldFormBundleNo)
          organisationName <- subscriptionDataService.getOrganisationName
        } yield {
          Ok(views.html.editLiability.editLiabilityPrintFriendly(calculateDraftLiability.get, getReturnType(calculateDraftLiability.get),
            PeriodUtils.getDisplayPeriods(calculateDraftLiability.get.period, calculateDraftLiability.get.periodKey),
            PeriodUtils.getCalculatedPeriodValues(calculateDraftLiability.get.calculated),
            organisationName
          ))
        }
      }
    }
  }

}
