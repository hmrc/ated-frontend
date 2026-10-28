/*
 * Copyright 2026 HM Revenue & Customs
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
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms.propertyDetailsHasBeenRevaluedForm
import models.{HasBeenRevalued, PropertyDetailsRevalued}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, DataCacheService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.{HasPropertyBeenRevalued, SelectedPreviousReturn}
import utils.AtedUtils
import utils.AtedUtils.EDIT_FROM_SUMMARY
import views.html.propertyDetails.propertyDetailsHasBeenRevalued

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsHasBeenRevaluedController @Inject()(mcc: MessagesControllerComponents,
                                                         authAction: AuthAction,
                                                         template: propertyDetailsHasBeenRevalued,
                                                         serviceInfoService: ServiceInfoService,
                                                         val propertyDetailsService: PropertyDetailsService,
                                                         val backLinkCacheService: BackLinkCacheService,
                                                         val dataCacheService: DataCacheService,
                                                         dateOfChangeController: PropertyDetailsDateOfChangeController,
                                                         exitController: PropertyDetailsExitController
                                                        )(
                                                          using val appConfig: ApplicationConfig
                                                        ) extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  given ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsHasBeenRevaluedController"

  def view(id: String,  mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => {
              currentBackLink.flatMap { backLink =>
                dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                  dataCacheService.fetchAndGetData[HasBeenRevalued](HasPropertyBeenRevalued).map {
                    cachedHasBeenRevalued =>
                      val hasBeenRevalued = cachedHasBeenRevalued.flatMap(_.isPropertyRevalued)
                      val modeView = if (!mode.contains(EDIT_FROM_SUMMARY)) {
                        AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
                      } else {
                        mode
                      }
                      val filledForm = propertyDetailsHasBeenRevaluedForm.fill(HasBeenRevalued(hasBeenRevalued))
                      println(s"PropertyDetailsHasBeenRevaluedController.view: filledForm = $filledForm")
                      println(cachedHasBeenRevalued)
                      Ok(template(id,
                        propertyDetails.periodKey,
                        backLink,
                        modeView,
                        filledForm,
                        serviceInfoContent))
                  }
                }
              }
            }
          }
        }
      }
    }
  }


  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authAction.authorisedAction { implicit authContext =>
        ensureClientContext {
          serviceInfoService.getPartial.flatMap { serviceInfoContent =>
            propertyDetailsHasBeenRevaluedForm.bindFromRequest().fold(
              formWithErrors => {
                currentBackLink.map(backLink => BadRequest(template(id, periodKey, backLink, mode, formWithErrors, serviceInfoContent)))
              },
              hasBeenRevalued => {
                dataCacheService.saveFormData[HasBeenRevalued](HasPropertyBeenRevalued, hasBeenRevalued)
                val propertyDetailsFuture: Future[PropertyDetailsRevalued] = for {
                  hasPropertyBeenRevalued <- dataCacheService.fetchAndGetData[HasBeenRevalued](HasPropertyBeenRevalued)
                } yield {
                  PropertyDetailsRevalued(
                    isPropertyRevalued = hasPropertyBeenRevalued.flatMap(_.isPropertyRevalued)
                  )
                }
                  for {
                    _ <- propertyDetailsService.saveDraftPropertyDetailsIsPropertyRevalued(id, hasBeenRevalued)
                    result <- if (hasBeenRevalued.isPropertyRevalued.getOrElse(false))                 {
                      redirectWithBackLink(
                        dateOfChangeController.controllerId,
                        controllers.propertyDetails.routes.PropertyDetailsDateOfChangeController.view(id, mode),
                        Some(controllers.propertyDetails.routes.PropertyDetailsHasBeenRevaluedController.view(id, mode).url)
                      )
                    }  else {
                      redirectWithBackLink(
                        exitController.controllerId,
                        controllers.propertyDetails.routes.PropertyDetailsExitController.view(),
                        Some(controllers.propertyDetails.routes.PropertyDetailsHasBeenRevaluedController.view(id, mode).url)
                      )
                    }
                  } yield result
                
              }
            )
          }
        }
      }
  }
}
