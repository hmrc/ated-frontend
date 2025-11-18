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
import connectors.{BackLinkCacheService, DataCacheService}
import controllers.auth.{AuthAction, ClientHelper}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import forms.PropertyDetailsForms._
import models.DateOfChange
import play.api.i18n.{Messages, MessagesImpl}
import utils.AtedConstants.{FortyThousandValueDateOfChange, SelectedPreviousReturn}
import utils.AtedUtils

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PropertyDetailsDateOfChangeController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      serviceInfoService: ServiceInfoService,
                                                      template: views.html.propertyDetails.propertyDetailsDateOfChange,
                                                      val propertyDetailsService: PropertyDetailsService,
                                                      val backLinkCacheService: BackLinkCacheService,
                                                      val dataCacheService: DataCacheService,
                                                      newValuationController: PropertyDetailsNewValuationController)
                                                     (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsDateOfChangeController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => {}
              currentBackLink.flatMap { backlink =>
                dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                  dataCacheService.fetchAndGetData[DateOfChange](FortyThousandValueDateOfChange).map { cachedDateOfChange =>
                    val dateOfChange = cachedDateOfChange.flatMap(_.dateOfChange)
                    Ok(template(id,
                      propertyDetails.periodKey,
                      propertyDetailsDateOfChangeForm.fill(DateOfChange(dateOfChange)),
                      AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                      serviceInfoContent,
                      backlink))
                  }
                }
              }
          }
        }
      }
    }
  }


  implicit lazy val messages: Messages = MessagesImpl(mcc.langs.availables.head, messagesApi)

  val dateFields: (String, String) = ("dateOfChange", messages("ated.property-details-value.dateOfChange.messageKey"))

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          validateDateOfChange(periodKey, propertyDetailsDateOfChangeForm.bindFromRequest(), dateFields).fold(
            formWithError => {
              currentBackLink.map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink)))
            },
            dateOfChange => {
              dataCacheService.saveFormData[DateOfChange](FortyThousandValueDateOfChange, dateOfChange)
              redirectWithBackLink(
                newValuationController.controllerId,
                controllers.propertyDetails.routes.PropertyDetailsNewValuationController.view(id),
                Some(controllers.propertyDetails.routes.PropertyDetailsDateOfChangeController.view(id).url)
              )
            }
          )
        }
      }
    }
  }
}
