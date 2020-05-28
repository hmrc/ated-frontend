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
import controllers.editLiability.EditLiabilitySummaryController
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants._
import utils.AtedUtils

import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsSupportingInfoController @Inject()(mcc: MessagesControllerComponents,
                                                        authAction: AuthAction,
                                                        editLiabilitySummaryController: EditLiabilitySummaryController,
                                                        propertyDetailsSummaryController: PropertyDetailsSummaryController,
                                                        val propertyDetailsService: PropertyDetailsService,
                                                        val dataCacheConnector: DataCacheConnector,
                                                        val backLinkCacheConnector: BackLinkCacheConnector)
                                                       (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsSupportingInfoController"


  def view(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val filledForm = propertyDetails.period.flatMap(_.supportingInfo) match {
              case Some(supportingInfo) => propertyDetailsSupportingInfoForm.fill(PropertyDetailsSupportingInfo(supportingInfo))
              case _ => propertyDetailsSupportingInfoForm
            }
            currentBackLink.flatMap(backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                Ok(views.html.propertyDetails.propertyDetailsSupportingInfo(id, propertyDetails.periodKey, filledForm,
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn), backLink))
              }
            )
        }
      }
    }
  }

  def editFromSummary(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
              val filledForm = propertyDetails.period.flatMap(_.supportingInfo) match {
                case Some(supportingInfo) => propertyDetailsSupportingInfoForm.fill(PropertyDetailsSupportingInfo(supportingInfo))
                case _ => propertyDetailsSupportingInfoForm
              }
              val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
              Future.successful(Ok(views.html.propertyDetails.propertyDetailsSupportingInfo(id, propertyDetails.periodKey, filledForm,
                mode, AtedUtils.getSummaryBackLink(id, None))))
            }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsSupportingInfoForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.map(backLink => BadRequest(views.html.propertyDetails.propertyDetailsSupportingInfo(id, periodKey, formWithError, mode, backLink)))
          },
          propertyDetails => {
            val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsSupportingInfoController.view(id).url)
            for {
              cachedData <- dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn)
              _ <- propertyDetailsService.validateCalculateDraftPropertyDetails(id, AtedUtils.isEditSubmittedMode(mode) && cachedData.isEmpty)
              _ <- propertyDetailsService.saveDraftPropertyDetailsSupportingInfo(id, propertyDetails)
              result <-
              if (AtedUtils.isEditSubmittedMode(mode) && cachedData.isEmpty) {
                redirectWithBackLink(
                  editLiabilitySummaryController.controllerId,
                  controllers.editLiability.routes.EditLiabilitySummaryController.view(id),
                  backLink)
              } else {
                propertyDetailsService.calculateDraftPropertyDetails(id).flatMap { response =>
                  response.status match {
                    case OK =>
                      redirectWithBackLink(
                        propertyDetailsSummaryController.controllerId,
                        controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(id),
                        backLink)
                    case BAD_REQUEST if response.body.contains("Agent not Valid") =>
                      Future.successful(BadRequest(views.html.global_error("ated.client-problem.title",
                        "ated.client-problem.header", "ated.client-problem.message", None, Some(appConfig.agentRedirectedToMandate), None, None, appConfig)))
                  }
                }
              }
            } yield result
          }
        )
      }
    }
  }
}
