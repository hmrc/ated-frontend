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
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models.PropertyDetailsOwnedBefore
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.{ExecutionContext, Future}


class PropertyDetailsOwnedBeforeController @Inject()(mcc: MessagesControllerComponents,
                                                     authAction: AuthAction,
                                                     propertyDetailsNewBuildController: PropertyDetailsNewBuildController,
                                                     propertyDetailsProfessionallyValuedController: PropertyDetailsProfessionallyValuedController,
                                                     val propertyDetailsService: PropertyDetailsService,
                                                     val dataCacheConnector: DataCacheConnector,
                                                     val backLinkCacheConnector: BackLinkCacheConnector)
                                                    (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsOwnedBeforeController"


  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                val displayData = PropertyDetailsOwnedBefore(propertyDetails.value.flatMap(_.isOwnedBeforePolicyYear),
                  propertyDetails.value.flatMap(_.ownedBeforePolicyYearValue))
                Future.successful(Ok(views.html.propertyDetails.propertyDetailsOwnedBefore(id,
                  propertyDetails.periodKey,
                  propertyDetailsOwnedBeforeForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  backLink)
                ))
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
            dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
              val displayData = PropertyDetailsOwnedBefore(propertyDetails.value.flatMap(_.isOwnedBeforePolicyYear),
                propertyDetails.value.flatMap(_.ownedBeforePolicyYearValue))
              val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
              Future.successful(Ok(views.html.propertyDetails.propertyDetailsOwnedBefore(id,
                propertyDetails.periodKey,
                propertyDetailsOwnedBeforeForm.fill(displayData),
                mode,
                AtedUtils.getSummaryBackLink(id, None))
              ))
            }
          }
        }
      }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        PropertyDetailsForms.validatePropertyDetailsOwnedBefore(propertyDetailsOwnedBeforeForm.bindFromRequest).fold(
          formWithError => {
            currentBackLink.map(backLink =>
              BadRequest(views.html.propertyDetails.propertyDetailsOwnedBefore(id, periodKey, formWithError, mode, backLink))
            )
          },
          propertyDetails => {
            for {
              _ <- propertyDetailsService.saveDraftPropertyDetailsOwnedBefore(id, propertyDetails)
              result <-
                if (propertyDetails.isOwnedBeforePolicyYear.getOrElse(false)){
                redirectWithBackLink(
                  propertyDetailsProfessionallyValuedController.controllerId,
                  controllers.propertyDetails.routes.PropertyDetailsProfessionallyValuedController.view(id),
                  Some(controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(id).url))
                } else {
                redirectWithBackLink(
                  propertyDetailsNewBuildController.controllerId,
                  controllers.propertyDetails.routes.PropertyDetailsNewBuildController.view(id),
                  Some(controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(id).url))
                }
            } yield result
          }
        )
      }
    }
  }

}
