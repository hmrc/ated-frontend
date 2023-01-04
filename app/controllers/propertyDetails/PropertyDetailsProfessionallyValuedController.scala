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
import models.PropertyDetailsProfessionallyValued
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding
import scala.concurrent.ExecutionContext

class PropertyDetailsProfessionallyValuedController @Inject()(mcc: MessagesControllerComponents,
                                                              authAction: AuthAction,
                                                              propertyDetailsAcquisitionController: PropertyDetailsAcquisitionController,
                                                              serviceInfoService: ServiceInfoService,
                                                              val propertyDetailsService: PropertyDetailsService,
                                                              val dataCacheConnector: DataCacheConnector,
                                                              val backLinkCacheConnector: BackLinkCacheConnector,
                                                              template: views.html.propertyDetails.propertyDetailsProfessionallyValued)
                                                             (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsProfessionallyValuedController"


  def view(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              currentBackLink.flatMap { backLink =>
                dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                  val displayData = PropertyDetailsProfessionallyValued(propertyDetails.value.flatMap(_.isValuedByAgent))
                  Ok(template(id,
                    propertyDetails.periodKey,
                    propertyDetailsProfessionallyValuedForm.fill(displayData),
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

  def editFromSummary(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                val displayData = PropertyDetailsProfessionallyValued(propertyDetails.value.flatMap(_.isValuedByAgent))
                Ok(template(id,
                  propertyDetails.periodKey,
                  propertyDetailsProfessionallyValuedForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  serviceInfoContent,
                  AtedUtils.getSummaryBackLink(id, None)))
              }
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsProfessionallyValuedForm.bindFromRequest.fold(
            formWithError => {
              currentBackLink
                .map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink)))
            },
            propertyDetails => {
              for {
                _ <- propertyDetailsService.saveDraftPropertyDetailsProfessionallyValued(id, propertyDetails)
                result <-
                  redirectWithBackLink(
                    propertyDetailsAcquisitionController.controllerId,
                    controllers.propertyDetails.routes.PropertyDetailsAcquisitionController.view(id),
                    Some(controllers.propertyDetails.routes.PropertyDetailsProfessionallyValuedController.view(id).url)
                  )
              } yield result
            }
          )
        }
      }
    }
  }
}
