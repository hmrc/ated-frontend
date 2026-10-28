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
import forms.PropertyDetailsForms.*

import javax.inject.Inject
import models.{PropertyDetailsValueOnAcquisition, PropertyDetailsWhenAcquiredDates}

import java.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import utils.AtedUtils.EDIT_FROM_SUMMARY
import views.html

import scala.concurrent.{ExecutionContext, Future}


class PropertyDetailsValueAcquiredController @Inject()(mcc: MessagesControllerComponents,
                                                       authAction: AuthAction,
                                                       propertyDetailsProfessionallyValuedController: PropertyDetailsProfessionallyValuedController,
                                                       serviceInfoService: ServiceInfoService,
                                                       val propertyDetailsService: PropertyDetailsService,
                                                       val dataCacheService: DataCacheService,
                                                       val backLinkCacheService: BackLinkCacheService,
                                                       template: html.propertyDetails.propertyDetailsValueAcquired)
                                                      (using val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  given ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsValueAcquiredController"

  def view(id: String, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => currentBackLink.flatMap { backLink =>
              dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                val displayData = PropertyDetailsValueOnAcquisition(propertyDetails.value.flatMap(_.notNewBuildValue))
                val dynamicDate = PropertyDetailsWhenAcquiredDates(propertyDetails.value.flatMap(_.notNewBuildDate)).acquiredDate.getOrElse(LocalDate.now())
                val modeView = if (!mode.contains(EDIT_FROM_SUMMARY)) {
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
                } else {
                  mode
                }
                Future.successful(Ok(template(id,
                  propertyDetails.periodKey,
                  propertyDetailsValueAcquiredForm.fill(displayData),
                  modeView,
                  serviceInfoContent,
                  backLink,
                  dynamicDate)
                ))
              }
            }
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String], date: LocalDate): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction {
      implicit authContext => {
        ensureClientContext {
          serviceInfoService.getPartial.flatMap { serviceInfoContent =>
            propertyDetailsValueAcquiredForm.bindFromRequest().fold(
              formWithError => {
                currentBackLink.map(backLink =>
                  BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink, date)))
              },
              propertyDetails => {
                for {
                  _ <- propertyDetailsService.saveDraftPropertyDetailsValueAcquired(id, propertyDetails)
                  result <-
                    redirectWithBackLink(
                      propertyDetailsProfessionallyValuedController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsProfessionallyValuedController.view(id, mode),
                      Some(controllers.propertyDetails.routes.PropertyDetailsValueAcquiredController.view(id, mode).url)
                    )
                } yield result
              }
            )
          }
        }
      }
    }
  }
}

