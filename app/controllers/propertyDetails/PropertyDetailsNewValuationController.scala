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

package controllers.propertyDetails


import config.ApplicationConfig
import connectors.{BackLinkCacheService, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms.propertyDetailsNewValuationForm
import models.PropertyDetailsNewValuation
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.{SelectedPreviousReturn, propertyDetailsNewValuationValue}
import utils.AtedUtils

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PropertyDetailsNewValuationController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      serviceInfoService: ServiceInfoService,
                                                      val backLinkCacheConnector: BackLinkCacheService,
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val propertyDetailsService: PropertyDetailsService,
                                                      propertyDetailsDateOfRevalueController: PropertyDetailsDateOfRevalueController,
                                                      template: views.html.propertyDetails.propertyDetailsNewValuation)
                                                     (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with WithUnsafeDefaultFormBinding with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsNewValuationController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {

        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => {
              currentBackLink.flatMap { backLink =>
                dataCacheConnector.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                  dataCacheConnector.fetchAndGetData[PropertyDetailsNewValuation](propertyDetailsNewValuationValue).map { cachedNewValuation =>
                    val newValuation = cachedNewValuation.flatMap(_.revaluedValue)
                    Ok(template(id,
                      propertyDetails.periodKey,
                      AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
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
              dataCacheConnector.saveFormData[PropertyDetailsNewValuation](propertyDetailsNewValuationValue, revaluedValue)
              redirectWithBackLink(
                propertyDetailsDateOfRevalueController.controllerId,
                controllers.propertyDetails.routes.PropertyDetailsDateOfRevalueController.view(id),
                Some(controllers.propertyDetails.routes.PropertyDetailsNewValuationController.view(id).url)
              )
            }
          )
        }
      }
    }
  }
  }
}


