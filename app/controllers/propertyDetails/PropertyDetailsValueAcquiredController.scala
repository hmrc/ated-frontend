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
import models.{PropertyDetailsValueOnAcquisition, PropertyDetailsWhenAcquiredDates}
import org.joda.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import views.html
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}


class PropertyDetailsValueAcquiredController @Inject()(mcc: MessagesControllerComponents,
                                                       authAction: AuthAction,
                                                       propertyDetailsProfessionallyValuedController: PropertyDetailsProfessionallyValuedController,
                                                       serviceInfoService: ServiceInfoService,
                                                       val propertyDetailsService: PropertyDetailsService,
                                                       val dataCacheConnector: DataCacheConnector,
                                                       val backLinkCacheConnector: BackLinkCacheConnector,
                                                       template: html.propertyDetails.propertyDetailsValueAcquired)
                                                      (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsValueAcquiredController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                val displayData = PropertyDetailsValueOnAcquisition(propertyDetails.value.flatMap(_.notNewBuildValue))
                val dynamicDate = PropertyDetailsWhenAcquiredDates(propertyDetails.value.flatMap(_.notNewBuildDate)).acquiredDate.getOrElse(new LocalDate())
                Future.successful(Ok(template(id,
                  propertyDetails.periodKey,
                  propertyDetailsValueAcquiredForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
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
            propertyDetailsValueAcquiredForm.bindFromRequest.fold(
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
                      controllers.propertyDetails.routes.PropertyDetailsProfessionallyValuedController.view(id),
                      Some(controllers.propertyDetails.routes.PropertyDetailsValueAcquiredController.view(id).url)
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

