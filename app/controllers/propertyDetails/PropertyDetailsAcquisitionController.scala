/*
 * Copyright 2019 HM Revenue & Customs
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
import models.PropertyDetailsAcquisition
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsAcquisitionController @Inject()(mcc: MessagesControllerComponents,
                                                     authAction: AuthAction,
                                                     isFullTaxPeriodController: IsFullTaxPeriodController,
                                                     propertyDetailsRevaluedController: PropertyDetailsRevaluedController,
                                                     val propertyDetailsService: PropertyDetailsService,
                                                     val dataCacheConnector: DataCacheConnector,
                                                     val backLinkCacheConnector: BackLinkCacheConnector)
                                                    (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsAcquisitionController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                val filledForm = propertyDetailsAcquisitionForm.fill(PropertyDetailsAcquisition(propertyDetails.value.flatMap(_.anAcquisition)))
                Ok(views.html.propertyDetails.propertyDetailsAcquisition(id,
                  propertyDetails.periodKey,
                  filledForm,
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  backLink)
                )
              }
            }
        }
      }
    }
  }

  def editFromSummary(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val mode = AtedUtils.getEditSubmittedMode(propertyDetails)
            val filledForm = propertyDetailsAcquisitionForm.fill(PropertyDetailsAcquisition(propertyDetails.value.flatMap(_.anAcquisition)))
            Future.successful(Ok(views.html.propertyDetails.propertyDetailsAcquisition(id,
              propertyDetails.periodKey,
              filledForm,
              mode,
              AtedUtils.getSummaryBackLink(id, None))
            ))
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsAcquisitionForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.map(backLink => BadRequest(views.html.propertyDetails.propertyDetailsAcquisition(id, periodKey, formWithError, mode, backLink)))
          },
          propertyDetails => {
            val anAcquisition = propertyDetails.anAcquisition.getOrElse(false)
            val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsAcquisitionController.view(id).url)
            for {
              _      <- propertyDetailsService.saveDraftPropertyDetailsAcquisition(id, anAcquisition)
              result <-
              if (anAcquisition) {
                redirectWithBackLink(
                  propertyDetailsRevaluedController.controllerId,
                  controllers.propertyDetails.routes.PropertyDetailsRevaluedController.view(id),
                  backLink
                )
              } else {
                redirectWithBackLink(
                  isFullTaxPeriodController.controllerId,
                  controllers.propertyDetails.routes.IsFullTaxPeriodController.view(id),
                  backLink
                )
              }
            } yield result
          }
        )
      }
    }
  }

}
