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
import forms.PropertyDetailsForms.propertyDetailsNewValuationForm
import models.PropertyDetailsNewValuation
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.{SelectedPreviousReturn, propertyDetailsNewValuationValue}
import utils.AtedUtils
import utils.AtedUtils.EDIT_FROM_SUMMARY

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PropertyDetailsNewValuationController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      serviceInfoService: ServiceInfoService,
                                                      val backLinkCacheService: BackLinkCacheService,
                                                      val dataCacheService: DataCacheService,
                                                      val propertyDetailsService: PropertyDetailsService,
                                                      propertyDetailsDateOfRevalueController: PropertyDetailsDateOfRevalueController,
                                                      template: views.html.propertyDetails.propertyDetailsNewValuation)
                                                     (using val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  given ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsNewValuationController"

  def view(id: String, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {

        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => {
              currentBackLink.flatMap { backLink =>
                dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                  dataCacheService.fetchAndGetData[PropertyDetailsNewValuation](propertyDetailsNewValuationValue).map { cachedNewValuation =>
                    val newValuation = cachedNewValuation.flatMap(_.revaluedValue)
                    val modeView = if (!mode.contains(EDIT_FROM_SUMMARY)) {
                      AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
                    } else {
                      mode
                    }
                    Ok(template(id,
                      propertyDetails.periodKey,
                      modeView,
                      propertyDetailsNewValuationForm.fill(PropertyDetailsNewValuation(newValuation)),
                      backLink,
                      serviceInfoContent
                    ))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request => {
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsNewValuationForm.bindFromRequest().fold(
            formWithErrors => {
              currentBackLink.map(backLink => BadRequest(template(id, periodKey, mode, formWithErrors, backLink, serviceInfoContent)))
            },
            revaluedValue => {
              dataCacheService.saveFormData[PropertyDetailsNewValuation](propertyDetailsNewValuationValue, revaluedValue)
              redirectWithBackLink(
                propertyDetailsDateOfRevalueController.controllerId,
                controllers.propertyDetails.routes.PropertyDetailsDateOfRevalueController.view(id, mode),
                Some(controllers.propertyDetails.routes.PropertyDetailsNewValuationController.view(id, mode).url)
              )
            }
          )
        }
      }
    }
  }
  }
}


