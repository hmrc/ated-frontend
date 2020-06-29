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

package controllers.propertyDetails

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import javax.inject.Inject
import models.PropertyDetails
import org.joda.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{AtedUtils, PeriodUtils}

import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsSummaryController @Inject()(mcc: MessagesControllerComponents,
                                                 authAction: AuthAction,
                                                 subscriptionDataService: SubscriptionDataService,
                                                 propertyDetailsDeclarationController: PropertyDetailsDeclarationController,
                                                 serviceInfoService: ServiceInfoService,
                                                 val propertyDetailsService: PropertyDetailsService,
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val backLinkCacheConnector: BackLinkCacheConnector)
                                                (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkController with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  override val controllerId = "PropertyDetailsSummaryController"

  def view(propertyKey: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(propertyKey) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              currentBackLink.flatMap(backLink =>
                Future.successful(Ok(views.html.propertyDetails.propertyDetailsSummary(propertyDetails,
                  PeriodUtils.getDisplayPeriods(propertyDetails.period),
                  AtedUtils.canSubmit(propertyDetails.periodKey, LocalDate.now),
                  PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated),
                  serviceInfoContent,
                  backLink)
                )))
          }
        }
      }
    }
  }

  def submit(propertyKey: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext(redirectWithBackLink(
        propertyDetailsDeclarationController.controllerId,
        controllers.propertyDetails.routes.PropertyDetailsDeclarationController.view(propertyKey),
        Some(controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(propertyKey).url)
      ))
    }
  }


  def viewPrintFriendlyLiabilityReturn(propertyKey: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        subscriptionDataService.getOrganisationName.flatMap { organisationName =>
          propertyDetailsCacheResponse(propertyKey) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              Future.successful(Ok(views.html.propertyDetails.propertyDetailsPrintFriendly(propertyDetails,
                PeriodUtils.getDisplayPeriods(propertyDetails.period),
                PeriodUtils.getCalculatedPeriodValues(propertyDetails.calculated),
                organisationName
              )))
          }
        }
      }
    }
  }


  def deleteDraft(propertyKey: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        Future.successful(Redirect(controllers.routes.DraftDeleteConfirmationController.view(Some(propertyKey), periodKey, "charge")))
      }
    }
  }
}
