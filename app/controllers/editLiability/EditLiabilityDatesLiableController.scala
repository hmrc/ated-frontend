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

package controllers.editLiability

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import controllers.propertyDetails.{PropertyDetailsHelpers, PropertyDetailsTaxAvoidanceController}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class EditLiabilityDatesLiableController @Inject()(mcc: MessagesControllerComponents,
                                                   authAction: AuthAction,
                                                   propertyDetailsTaxAvoidanceController: PropertyDetailsTaxAvoidanceController,
                                                   serviceInfoService: ServiceInfoService,
                                                   val propertyDetailsService: PropertyDetailsService,
                                                   val dataCacheConnector: DataCacheConnector,
                                                   val backLinkCacheConnector: BackLinkCacheConnector)
                                                  (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId = "EditLiabilityDatesLiableController"

  def view(formBundleNo: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(formBundleNo) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              val liabilityPeriod = propertyDetails.period.flatMap(_.liabilityPeriods.headOption)

              val filledForm = liabilityPeriod match {
                case Some(lineItem) => periodDatesLiableForm.fill(PropertyDetailsDatesLiable(lineItem.startDate, lineItem.endDate))
                case _ => periodDatesLiableForm
              }
              currentBackLink.map(backLink =>
                Ok(views.html.editLiability.editLiabilityDatesLiable(formBundleNo, propertyDetails.periodKey, filledForm, serviceInfoContent, backLink))
              )
          }
        }
      }
    }
  }

  def save(formBundleNo: String, periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, periodDatesLiableForm.bindFromRequest, periodsCheck = false).fold(
            formWithError => {
              currentBackLink.map(backLink =>
                BadRequest(views.html.editLiability.editLiabilityDatesLiable(formBundleNo, periodKey, formWithError, serviceInfoContent, backLink))
              )
            },
            propertyDetails => {
              for {
                _ <- propertyDetailsService.saveDraftPropertyDetailsDatesLiable(formBundleNo, propertyDetails)
                result <-
                  redirectWithBackLink(
                    propertyDetailsTaxAvoidanceController.controllerId,
                    controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceController.view(formBundleNo),
                    Some(controllers.editLiability.routes.EditLiabilityDatesLiableController.view(formBundleNo).url)
                  )
              } yield result
            }
          )
        }
      }
    }
  }

}
