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
import models.PropertyDetailsWhenAcquiredDates
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import views.html

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class PropertyDetailsWhenAcquiredController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      propertyDetailsValueAcquiredController: PropertyDetailsValueAcquiredController,
                                                      serviceInfoService: ServiceInfoService,
                                                      val propertyDetailsService: PropertyDetailsService,
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val backLinkCacheConnector: BackLinkCacheConnector,
                                                      template: html.propertyDetails.propertyDetailsWhenAcquired)
                                                     (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsWhenAcquiredController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                val displayData = PropertyDetailsWhenAcquiredDates(propertyDetails.value.flatMap(_.notNewBuildDate))
                Future.successful(Ok(template(id,
                  propertyDetails.periodKey,
                  propertyDetailsWhenAcquiredDatesForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  serviceInfoContent,
                  backLink)
                ))
              }
            }
          }
        }
      }
    }
  }

  implicit lazy val messages: Messages = MessagesImpl(mcc.langs.availables.head, messagesApi)

  val dateFields: Seq[(String, String)] = Seq(("acquiredDate", Messages("ated.property-details.whenAcquired.messageKey")))

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction {
      implicit authContext => {
        ensureClientContext {
          serviceInfoService.getPartial.flatMap { serviceInfoContent =>

            validateWhenAcquiredDate(periodKey, propertyDetailsWhenAcquiredDatesForm.bindFromRequest(), dateFields).fold(
              formWithError => {
                currentBackLink.map(backLink =>
                  BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink))
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
}

