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
import controllers.editLiability.EditLiabilityDatesLiableController
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils

import scala.concurrent.ExecutionContext

class PropertyDetailsInReliefController @Inject()(mcc: MessagesControllerComponents,
                                                  authAction: AuthAction,
                                                  periodsInAndOutReliefController: PeriodsInAndOutReliefController,
                                                  periodDatesLiableController: PeriodDatesLiableController,
                                                  editLiabilityDatesLiableController: EditLiabilityDatesLiableController,
                                                  val propertyDetailsService: PropertyDetailsService,
                                                  val dataCacheConnector: DataCacheConnector,
                                                  val backLinkCacheConnector: BackLinkCacheConnector)
                                                 (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  override val controllerId: String = "PropertyDetailsInReliefController"


  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val filledForm = periodsInAndOutReliefForm.fill(PropertyDetailsInRelief(propertyDetails.period.flatMap(_.isInRelief)))
            currentBackLink.flatMap(backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                Ok(views.html.propertyDetails.propertyDetailsInRelief(id, propertyDetails.periodKey, filledForm,
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn), backLink)
                )
              }
            )
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        periodsInAndOutReliefForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.map(backLink =>
              BadRequest(views.html.propertyDetails.propertyDetailsInRelief(id, periodKey, formWithError, mode, backLink))
            )
          },
          propertyDetails => {
            val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsInReliefController.view(id).url)
            for {
              _ <- propertyDetailsService.saveDraftPropertyDetailsInRelief(id, propertyDetails)
              result <-
              (propertyDetails.isInRelief.getOrElse(false), AtedUtils.isEditSubmittedMode(mode)) match {
                case (true, _) =>
                  redirectWithBackLink(periodsInAndOutReliefController.controllerId,
                    controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id),
                    backLink
                  )
                case (false, false) =>
                  redirectWithBackLink(periodDatesLiableController.controllerId,
                    controllers.propertyDetails.routes.PeriodDatesLiableController.view(id),
                    backLink
                  )
                case (false, true) =>
                  redirectWithBackLink(editLiabilityDatesLiableController.controllerId,
                    controllers.editLiability.routes.EditLiabilityDatesLiableController.view(id),
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
