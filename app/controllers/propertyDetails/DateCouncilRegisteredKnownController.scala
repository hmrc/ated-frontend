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
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import javax.inject.{Singleton, Inject}
import models.{DateFirstOccupiedKnown, DateCouncilRegisteredKnown, DateCouncilRegistered}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.{SelectedPreviousReturn, NewBuildFirstOccupiedDateKnown, NewBuildCouncilRegisteredDateKnown, NewBuildCouncilRegisteredDate}
import utils.AtedUtils
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.ExecutionContext

@Singleton
class DateCouncilRegisteredKnownController @Inject() (val mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      serviceInfoService: ServiceInfoService,
                                                      val propertyDetailsService: PropertyDetailsService,
                                                      val dataCacheService: DataCacheService,
                                                      val backLinkCacheService: BackLinkCacheService,
                                                      view: views.html.propertyDetails.dateCouncilRegisteredKnown)(implicit
    val appConfig: ApplicationConfig)
    extends FrontendController(mcc)
    with PropertyDetailsHelpers
    with ClientHelper
    with WithUnsafeDefaultFormBinding
    with StoreNewBuildDates {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String          = DateCouncilRegisteredKnownControllerId

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {

        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) { case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                dataCacheService.fetchAndGetData[DateCouncilRegisteredKnown](NewBuildCouncilRegisteredDateKnown).map { councilRegistered =>
                  val councilRegisteredDateKnown: Option[Boolean] = propertyDetails.value.flatMap(_.isLocalAuthRegDateKnown)

                  val displayData = councilRegistered.getOrElse(DateCouncilRegisteredKnown(councilRegisteredDateKnown))

                  Ok(
                    view(
                      id,
                      dateCouncilRegisteredKnownForm.fill(displayData),
                      AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                      serviceInfoContent,
                      backLink))
                }
              }
            }
          }
        }
      }
    }
  }

  def save(id: String, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          dateCouncilRegisteredKnownForm
            .bindFromRequest()
            .fold(
              formWithError => currentBackLink.map(backLink => BadRequest(view(id, formWithError, mode, serviceInfoContent, backLink))),
              form => {
                dataCacheService.saveFormData[DateCouncilRegisteredKnown](NewBuildCouncilRegisteredDateKnown, form).flatMap {
                  case DateCouncilRegisteredKnown(Some(true)) =>
                    redirectWithBackLink(
                      DateCouncilRegisteredControllerId,
                      controllers.propertyDetails.routes.DateCouncilRegisteredController.view(id),
                      Some(controllers.propertyDetails.routes.DateCouncilRegisteredKnownController.view(id).url)
                    )
                  case _ =>
                    // Date not known => Clear any previously store date from cache
                    dataCacheService.saveFormData[DateCouncilRegistered](NewBuildCouncilRegisteredDate, DateCouncilRegistered(None)).flatMap { _ =>
                      // Fetch status of First occupied date
                      dataCacheService.fetchAndGetData[DateFirstOccupiedKnown](NewBuildFirstOccupiedDateKnown).flatMap {
                        // First occupied date known => store date from current cached values
                        case Some(DateFirstOccupiedKnown(Some(true))) =>
                          storeNewBuildDatesFromCache(id).flatMap { _ =>
                            redirectWithBackLink(
                              NewBuildValueControllerId,
                              controllers.propertyDetails.routes.PropertyDetailsNewBuildValueController.view(id),
                              Some(controllers.propertyDetails.routes.DateCouncilRegisteredKnownController.view(id).url)
                            )
                          }
                        case _ =>
                          // No First occupied date and no council registration date => Inform user via kickout page
                          redirectWithBackLink(
                            NoStartDateControllerId,
                            controllers.propertyDetails.routes.NewBuildNoStartDateController.view(id),
                            Some(controllers.propertyDetails.routes.DateCouncilRegisteredKnownController.view(id).url)
                          )
                      }
                    }
                }
              }
            )
        }
      }
    }
  }

}
