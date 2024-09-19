/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.PropertyDetailsForms.propertyDetailsHasBeenRevaluedForm
import models.HasBeenRevalued
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.{HasPropertyBeenRevalued, SelectedPreviousReturn}
import utils.AtedUtils
import views.html.propertyDetails.propertyDetailsHasBeenRevalued

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsHasBeenRevaluedController @Inject()(mcc: MessagesControllerComponents,
                                                         authAction: AuthAction,
                                                         template: propertyDetailsHasBeenRevalued,
                                                         serviceInfoService: ServiceInfoService,
                                                         val propertyDetailsService: PropertyDetailsService,
                                                         val backLinkCacheConnector: BackLinkCacheConnector,
                                                         val dataCacheConnector: DataCacheConnector,
                                                         dateOfChangeController: PropertyDetailsDateOfChangeController,
                                                         exitController: PropertyDetailsExitController
                                                        )(
                                                          implicit val appConfig: ApplicationConfig
                                                        ) extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsHasBeenRevaluedController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        if (appConfig.newRevaluedFeature) {
          serviceInfoService.getPartial.flatMap { serviceInfoContent =>
            propertyDetailsCacheResponse(id) {
              case PropertyDetailsCacheSuccessResponse(propertyDetails) => {
                currentBackLink.flatMap { backLink =>
                  dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                    dataCacheConnector.fetchAndGetFormData[HasBeenRevalued](HasPropertyBeenRevalued).map {
                      cachedHasBeenRevalued =>
                        val hasBeenRevalued = cachedHasBeenRevalued.flatMap(_.isPropertyRevalued)
                        Ok(template(id,
                          propertyDetails.periodKey,
                          backLink,
                          AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                          propertyDetailsHasBeenRevaluedForm.fill(HasBeenRevalued(hasBeenRevalued)),
                          serviceInfoContent))
                    }
                  }
                }
              }
            }
          }
        } else Future.successful(Redirect(controllers.routes.HomeController.home()))
      }
    }
  }


  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authAction.authorisedAction { implicit authContext =>
        ensureClientContext {
          if (appConfig.newRevaluedFeature) {
            serviceInfoService.getPartial.flatMap { serviceInfoContent =>
              propertyDetailsHasBeenRevaluedForm.bindFromRequest().fold(
                formWithErrors => {
                  currentBackLink.map(backLink => BadRequest(template(id, periodKey, backLink, mode, formWithErrors, serviceInfoContent)))
                },
                hasBeenRevalued => {
                  if (hasBeenRevalued.isPropertyRevalued.getOrElse(false)) {
                    dataCacheConnector.saveFormData[HasBeenRevalued](HasPropertyBeenRevalued, hasBeenRevalued)
                    redirectWithBackLink(
                      dateOfChangeController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsDateOfChangeController.view(id),
                      Some(controllers.propertyDetails.routes.PropertyDetailsHasBeenRevaluedController.view(id).url)
                    )
                  } else {
                    redirectWithBackLink(
                      exitController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsExitController.view(),
                      Some(controllers.propertyDetails.routes.PropertyDetailsHasBeenRevaluedController.view(id).url)
                    )
                  }
                }
              )
            }
          } else Future.successful(Redirect(controllers.routes.HomeController.home()))
        }
      }
  }
}
