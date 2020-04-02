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
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models.PropertyDetailsWhenAcquiredDates
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import views.html

import scala.concurrent.{ExecutionContext, Future}


class PropertyDetailsWhenAcquiredController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      propertyDetailsValueAcquiredController: PropertyDetailsValueAcquiredController,
                                                      val propertyDetailsService: PropertyDetailsService,
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val backLinkCacheConnector: BackLinkCacheConnector)
                                                     (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsWhenAcquiredController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) => currentBackLink.flatMap { backLink =>
            dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
              val displayData = PropertyDetailsWhenAcquiredDates(propertyDetails.value.flatMap(_.notNewBuildDate))
              Future.successful(Ok(html.propertyDetails.propertyDetailsWhenAcquired(id,
                propertyDetails.periodKey,
                propertyDetailsWhenAcquiredDatesForm.fill(displayData),
                AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                backLink)
              ))
            }
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction {
      implicit authContext => {
        ensureClientContext {
          propertyDetailsWhenAcquiredDatesForm.bindFromRequest.fold(
            formWithError => {
              currentBackLink.map(backLink =>
                BadRequest(views.html.propertyDetails.propertyDetailsWhenAcquired(id, periodKey, formWithError, mode, backLink))
              )
            },
            propertyDetails => {
              for {
                _ <- propertyDetailsService.saveDraftPropertyDetailsWhenAcquiredDates(id, propertyDetails)
                result <-
                  redirectWithBackLink(
                    propertyDetailsValueAcquiredController.controllerId,
                    controllers.propertyDetails.routes.PropertyDetailsValueAcquiredController.view(id),
                    Some(controllers.propertyDetails.routes.PropertyDetailsWhenAcquiredController.view(id).url)
                  )
              } yield result
            }
          )
        }
      }
    }
  }
}
