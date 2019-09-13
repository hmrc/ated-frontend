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

package controllers.editLiability

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import models.{PropertyDetails, StandardAuthRetrievals}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import services.{DelegationService, PropertyDetailsService, SubscriptionDataService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.PeriodUtils

import scala.concurrent.Future

trait EditLiabilitySummaryController extends BackLinkController
  with AuthAction with ClientHelper {

  def propertyDetailsService: PropertyDetailsService
  def subscriptionDataService: SubscriptionDataService

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsService.calculateDraftChangeLiability(oldFormBundleNo).flatMap { x =>
          x.calculated.flatMap(_.amountDueOrRefund) match {
            case Some(amount) if amount < 0 =>
              ForwardBackLinkToNextPage(HasBankDetailsController.controllerId, controllers.editLiability.routes.HasBankDetailsController.view(oldFormBundleNo))
            case Some(amount) => viewSummaryDetails(x)
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
          }
        }
      }
    }
  }

  def viewSummary(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsService.calculateDraftChangeLiability(oldFormBundleNo).flatMap(viewSummaryDetails(_))
      }
    }
  }

  private def viewSummaryDetails(propertyDetails: PropertyDetails)
                                (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, request: Request[AnyContent]) = {
    currentBackLink.map(
      backLink =>
        Ok(views.html.editLiability.editLiabilitySummary(propertyDetails,
          getReturnType(propertyDetails),
          PeriodUtils.getDisplayPeriods(propertyDetails.period),
          PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated),
          backLink))
    )
  }

  private def getReturnType(propertyDetails: PropertyDetails) = {
    propertyDetails.calculated.flatMap(_.amountDueOrRefund).fold("C")(a => if (a > 0) "F" else if (a < 0) "A" else "C")
  }

  def submit(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        RedirectWithBackLink(
          EditLiabilityDeclarationController.controllerId,
          controllers.editLiability.routes.EditLiabilityDeclarationController.view(oldFormBundleNo),
          Some(controllers.editLiability.routes.EditLiabilitySummaryController.viewSummary(oldFormBundleNo).url)
        )
      }
    }
  }

  def viewPrintFriendlyEditLiabilityReturn(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          calculateDraftLiability <- propertyDetailsService.calculateDraftChangeLiability(oldFormBundleNo)
          organisationName <- subscriptionDataService.getOrganisationName
        } yield {
          Ok(views.html.editLiability.editLiabilityPrintFriendly(calculateDraftLiability, getReturnType(calculateDraftLiability),
            PeriodUtils.getDisplayPeriods(calculateDraftLiability.period),
            PeriodUtils.getCalculatedPeriodValues(calculateDraftLiability.calculated),
            organisationName
          ))
        }
      }
    }
  }

}

object EditLiabilitySummaryController extends EditLiabilitySummaryController {
  val delegationService: DelegationService = DelegationService
  val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val subscriptionDataService: SubscriptionDataService = SubscriptionDataService
  override val controllerId = "EditLiabilitySummaryController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
