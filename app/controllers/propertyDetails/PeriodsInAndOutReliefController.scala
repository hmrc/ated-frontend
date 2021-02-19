/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import javax.inject.Inject
import org.joda.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.{AtedUtils, PeriodUtils}

import scala.concurrent.{ExecutionContext, Future}

class PeriodsInAndOutReliefController @Inject()(mcc: MessagesControllerComponents,
                                                authAction: AuthAction,
                                                propertyDetailsTaxAvoidanceController: PropertyDetailsTaxAvoidanceController,
                                                serviceInfoService: ServiceInfoService,
                                                val propertyDetailsService: PropertyDetailsService,
                                                val dataCacheConnector: DataCacheConnector,
                                                val backLinkCacheConnector: BackLinkCacheConnector,
                                                template: views.html.propertyDetails.periodsInAndOutRelief)
                                               (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PeriodsInAndOutReliefController"

  def view(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              currentBackLink.flatMap { backLink =>
                dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                  Future.successful(Ok(template(id, propertyDetails.periodKey,
                    periodsInAndOutReliefForm,
                    PeriodUtils.getDisplayPeriods(propertyDetails.period, propertyDetails.periodKey),
                    AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                    serviceInfoContent,
                    backLink)))
                }
              }
          }
        }
      }
    }
  }

  def deletePeriod(id: String, startDate: LocalDate) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          propertyDetails <- propertyDetailsService.deleteDraftPropertyDetailsPeriod(id, startDate)
          serviceInfoContent <- serviceInfoService.getPartial
          result <-
          currentBackLink.flatMap(backLink =>
            Future.successful(Ok(template(id, propertyDetails.periodKey,
              periodsInAndOutReliefForm,
              PeriodUtils.getDisplayPeriods(propertyDetails.period, propertyDetails.periodKey),
              AtedUtils.getEditSubmittedMode(propertyDetails),
              serviceInfoContent,
              backLink))
            ))
        } yield {
          result
        }
      }
    }
  }

  def continue(id: String, periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext(redirectWithBackLink(
        propertyDetailsTaxAvoidanceController.controllerId,
        controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceController.view(id),
        Some(controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id).url)
      ))
    }
  }
}


