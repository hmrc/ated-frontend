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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models.DateCouncilRegisteredKnown
import play.api.mvc.{Call, Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.NewBuildCouncilRegisteredDateKnown
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}


class DateCouncilRegisteredKnownController @Inject()(mcc: MessagesControllerComponents,
                                                  authAction: AuthAction,
                                                  serviceInfoService: ServiceInfoService,
                                                  val propertyDetailsService: PropertyDetailsService,
                                                  val dataCacheConnector: DataCacheConnector,
                                                  val backLinkCacheConnector: BackLinkCacheConnector,
                                                  view: views.html.propertyDetails.dateCouncilRegisteredKnown)
                                                 (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "DateCouncilRegisteredKnownController"

  def view(id: String, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          currentBackLink.flatMap { backLink =>
            dataCacheConnector.fetchAndGetFormData[DateCouncilRegisteredKnown](NewBuildCouncilRegisteredDateKnown).flatMap { councilRegistered =>
              val displayData = councilRegistered.getOrElse(DateCouncilRegisteredKnown(None))
              Future.successful(Ok(view(id,
                dateCouncilRegisteredKnownForm.fill(displayData),
                mode,
                serviceInfoContent,
                backLink)
              ))
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
          dateCouncilRegisteredKnownForm.bindFromRequest.fold(
            formWithError => currentBackLink.map(backLink => BadRequest(view(id, formWithError, mode, serviceInfoContent, backLink))),
            form => {
              dataCacheConnector.saveFormData[DateCouncilRegisteredKnown](NewBuildCouncilRegisteredDateKnown, form).flatMap{_ =>
                redirectWithBackLink(
                  controllerId,
                  nextPage(id, form.isDateCouncilRegisteredKnown, mode),
                  Some(controllers.propertyDetails.routes.DateCouncilRegisteredKnownController.view(id, mode).url)
                )
              }
            }
          )
        }
      }
    }
  }

  private def nextPage(id: String, isDateCouncilRegisteredKnown: Option[Boolean], mode: Option[String]): Call =
    isDateCouncilRegisteredKnown match {
      case Some(true) => controllers.propertyDetails.routes.PropertyDetailsNewBuildDatesController.view(id)
      case Some(false) => controllers.propertyDetails.routes.PropertyDetailsNewBuildDatesController.view(id)
      case _ => controllers.propertyDetails.routes.PropertyDetailsNewBuildDatesController.view(id)
    }

}