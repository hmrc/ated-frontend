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
import forms.PropertyDetailsForms._
import javax.inject.{Singleton, Inject}
import models.{DateFirstOccupied, DateFirstOccupiedKnown}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.{SelectedPreviousReturn, NewBuildFirstOccupiedDateKnown, NewBuildFirstOccupiedDate}
import utils.AtedUtils
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.ExecutionContext

@Singleton
class DateFirstOccupiedKnownController @Inject()(mcc: MessagesControllerComponents,
                                                 authAction: AuthAction,
                                                 serviceInfoService: ServiceInfoService,
                                                 val propertyDetailsService: PropertyDetailsService,
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val backLinkCacheConnector: BackLinkCacheService,
                                                 view: views.html.propertyDetails.dateFirstOccupiedKnown)
                                                 (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = DateFirstOccupiedKnownControllerId

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                dataCacheConnector.fetchAndGetFormData[DateFirstOccupiedKnown](NewBuildFirstOccupiedDateKnown).map { firstOccupied =>
                  val firstOccupiedDateKnown: Option[Boolean] = propertyDetails.value.flatMap(_.isBuildDateKnown)
                  val displayData = firstOccupied.getOrElse(DateFirstOccupiedKnown(firstOccupiedDateKnown))

                  Ok(view(id,
                    dateFirstOccupiedKnownForm.fill(displayData),
                    AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                    serviceInfoContent,
                    backLink)
                  )
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
          dateFirstOccupiedKnownForm.bindFromRequest().fold(
            formWithError => currentBackLink.map(backLink => BadRequest(view(id, formWithError, mode, serviceInfoContent, backLink))),
            form =>
              dataCacheConnector.saveFormData[DateFirstOccupiedKnown](NewBuildFirstOccupiedDateKnown, form).flatMap{
                case DateFirstOccupiedKnown(Some(true)) =>
                  redirectWithBackLink(
                    DateFirstOccupiedControllerId,
                    controllers.propertyDetails.routes.DateFirstOccupiedController.view(id),
                    Some(controllers.propertyDetails.routes.DateFirstOccupiedKnownController.view(id).url)
                  )
                case _ =>
                  dataCacheConnector.saveFormData[DateFirstOccupied](NewBuildFirstOccupiedDate, DateFirstOccupied(None)).flatMap{_ =>
                    redirectWithBackLink(
                      DateCouncilRegisteredKnownControllerId,
                      controllers.propertyDetails.routes.DateCouncilRegisteredKnownController.view(id),
                      Some(controllers.propertyDetails.routes.DateFirstOccupiedKnownController.view(id).url)
                    )
                  }
              }
          )
        }
      }
    }
  }

}
