/*
 * Copyright 2017 HM Revenue & Customs
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

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedRegime, ClientHelper, ExternalUrls}
import controllers.editLiability.EditLiabilitySummaryController
import forms.PropertyDetailsForms._
import models._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import utils.AtedUtils
import utils.AtedConstants._

import scala.concurrent.Future

trait PropertyDetailsSupportingInfoController extends PropertyDetailsHelpers with ClientHelper {

  def view(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
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

  def editFromSummary(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
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

  def save(id: String, periodKey: Int, mode: Option[String]) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsSupportingInfoForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.map(backLink => BadRequest(views.html.propertyDetails.propertyDetailsSupportingInfo(id, periodKey, formWithError, mode, backLink)))
          },
          propertyDetails => {
            val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsSupportingInfoController.view(id).url)
            for {
              isRequestValid <- propertyDetailsService.validateCalculateDraftPropertyDetails(id)
              cachedData <- dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn)
              _ <- propertyDetailsService.saveDraftPropertyDetailsSupportingInfo(id, propertyDetails)
              result <-
              if (AtedUtils.isEditSubmittedMode(mode) && cachedData.isEmpty) {
                RedirectWithBackLink(
                  EditLiabilitySummaryController.controllerId,
                  controllers.editLiability.routes.EditLiabilitySummaryController.view(id),
                  backLink)
              } else {
                  propertyDetailsService.calculateDraftPropertyDetails(id).flatMap { response =>
                    response.status match {
                      case OK =>
                        RedirectWithBackLink(
                          PropertyDetailsSummaryController.controllerId,
                          controllers.propertyDetails.routes.PropertyDetailsSummaryController.view(id),
                          backLink)
                      case BAD_REQUEST if response.body.contains("Agent not Valid") =>
                        Future.successful(BadRequest(views.html.global_error(Messages("ated.client-problem.title"),
                          Messages("ated.client-problem.header"), Messages("ated.client-problem.body", ExternalUrls.agentRedirectedToMandate))))
                    }
                  }
                }
            } yield result
          }
        )
      }
  }

}

object PropertyDetailsSupportingInfoController extends PropertyDetailsSupportingInfoController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "PropertyDetailsSupportingInfoController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
