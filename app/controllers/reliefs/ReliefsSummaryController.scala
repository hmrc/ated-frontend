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

package controllers.reliefs

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import javax.inject.Inject
import models.ReliefsTaxAvoidance
import org.joda.time.LocalDate
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DelegationService, ReliefsService, SubscriptionDataService}
import uk.gov.hmrc.http.ForbiddenException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedUtils

import scala.concurrent.{ExecutionContext, Future}

class ReliefsSummaryController @Inject()(mcc: MessagesControllerComponents,
                                         authAction: AuthAction,
                                         reliefDeclarationController: ReliefDeclarationController,
                                         subscriptionDataService: SubscriptionDataService,
                                         val reliefsService: ReliefsService,
                                         val dataCacheConnector: DataCacheConnector,
                                         val backLinkCacheConnector: BackLinkCacheConnector)
                                        (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkController with ReliefHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  override val controllerId: String = "ReliefsSummaryController"

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            backLink <- currentBackLink
            retrievedData <- reliefsService.retrieveDraftReliefs(authContext.atedReferenceNumber, periodKey)
          } yield {
            val canSubmit = AtedUtils.canSubmit(periodKey, LocalDate.now)
            Ok(views.html.reliefs.reliefsSummary(retrievedData.map(_.periodKey).getOrElse(periodKey),
              retrievedData, canSubmit,
              isComplete(retrievedData),
              backLink))
          }
        }
      }
    }
  }

  private def isComplete(data: Option[ReliefsTaxAvoidance]) = {
    if (data.isDefined && data.get.reliefs.isAvoidanceScheme.isDefined) {
      if (data.get.reliefs.isAvoidanceScheme.get) {
        data.get.reliefs.isAvoidanceScheme.get &&
          ((data.get.taxAvoidance.rentalBusinessScheme.isDefined && data.get.taxAvoidance.rentalBusinessSchemePromoter.isDefined) ||
            (data.get.taxAvoidance.openToPublicScheme.isDefined && data.get.taxAvoidance.openToPublicSchemePromoter.isDefined) ||
            (data.get.taxAvoidance.propertyDeveloperScheme.isDefined && data.get.taxAvoidance.propertyDeveloperSchemePromoter.isDefined) ||
            (data.get.taxAvoidance.propertyTradingScheme.isDefined && data.get.taxAvoidance.propertyTradingSchemePromoter.isDefined) ||
            (data.get.taxAvoidance.lendingScheme.isDefined && data.get.taxAvoidance.lendingSchemePromoter.isDefined) ||
            (data.get.taxAvoidance.employeeOccupationScheme.isDefined && data.get.taxAvoidance.employeeOccupationSchemePromoter.isDefined) ||
            (data.get.taxAvoidance.farmHousesScheme.isDefined && data.get.taxAvoidance.farmHousesSchemePromoter.isDefined) ||
            (data.get.taxAvoidance.socialHousingScheme.isDefined && data.get.taxAvoidance.socialHousingSchemePromoter.isDefined) ||
            (data.get.taxAvoidance.equityReleaseScheme.isDefined && data.get.taxAvoidance.equityReleaseSchemePromoter.isDefined))
      } else {
        true
      }
    }
    else {
      false
    }
  }

  def continue(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          redirectWithBackLink(
            reliefDeclarationController.controllerId,
            controllers.reliefs.routes.ReliefDeclarationController.view(periodKey),
            Some(routes.ReliefsSummaryController.view(periodKey).url)
          )
        }
      }
    }
  }

  def viewPrintFriendlyReliefReturn(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            retrievedData <- reliefsService.retrieveDraftReliefs(authContext.atedReferenceNumber, periodKey)
            organisationName <- subscriptionDataService.getOrganisationName
          } yield {
            Ok(views.html.reliefs.reliefsPrintFriendly(periodKey, retrievedData, organisationName))
          }
        }
      }
    } recover {
      case _: ForbiddenException     =>
        Logger.warn("[ReliefsSummaryController][viewPrintFriendlyReliefReturn] Forbidden exception")
        authAction.unauthorisedUrl()
    }
  }

  def deleteDraft(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        ensureClientContext {
          Future.successful(Redirect(controllers.routes.DraftDeleteConfirmationController.view(None, periodKey, "relief")))
        }
      }
    }
  }

}
