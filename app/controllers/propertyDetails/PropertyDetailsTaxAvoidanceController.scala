/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsTaxAvoidanceController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      propertyDetailsSupportingInfoController: PropertyDetailsSupportingInfoController,
                                                      serviceInfoService: ServiceInfoService,
                                                      val propertyDetailsService: PropertyDetailsService,
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val backLinkCacheConnector: BackLinkCacheConnector,
                                                      template: views.html.propertyDetails.propertyDetailsTaxAvoidance)
                                                     (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsTaxAvoidanceController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              val displayData = PropertyDetailsTaxAvoidance(propertyDetails.period.flatMap(_.isTaxAvoidance),
                propertyDetails.period.flatMap(_.taxAvoidanceScheme),
                propertyDetails.period.flatMap(_.taxAvoidancePromoterReference))
              currentBackLink.flatMap(backLink =>
                dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                  Ok(template(id,
                    propertyDetails.periodKey,
                    propertyDetailsTaxAvoidanceForm.fill(displayData),
                    AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                    serviceInfoContent,
                    backLink))
                }
              )
          }
        }
      }
    }
  }

  def editFromSummary(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                val displayData = PropertyDetailsTaxAvoidance(propertyDetails.period.flatMap(_.isTaxAvoidance),
                  propertyDetails.period.flatMap(_.taxAvoidanceScheme),
                  propertyDetails.period.flatMap(_.taxAvoidancePromoterReference))

                val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
                Future.successful(Ok(template(id,
                  propertyDetails.periodKey,
                  propertyDetailsTaxAvoidanceForm.fill(displayData),
                  mode,
                  serviceInfoContent,
                  AtedUtils.getSummaryBackLink(id, None))
                ))
              }
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          PropertyDetailsForms.validatePropertyDetailsTaxAvoidance(propertyDetailsTaxAvoidanceForm.bindFromRequest).fold(
            formWithError =>
              currentBackLink.map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink))),
            propertyDetails => {
              for {
                _ <- propertyDetailsService.saveDraftPropertyDetailsTaxAvoidance(id, propertyDetails)
                result <-
                  redirectWithBackLink(
                    propertyDetailsSupportingInfoController.controllerId,
                    controllers.propertyDetails.routes.PropertyDetailsSupportingInfoController.view(id),
                    Some(controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceController.view(id).url)
                  )
              } yield result
            }
          )
        }
      }
    }
  }
}
