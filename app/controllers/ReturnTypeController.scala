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

package controllers

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import controllers.propertyDetails.{AddressLookupController, PropertyDetailsAddressController}
import controllers.reliefs.ChooseReliefsController
import forms.AtedForms.returnTypeForm
import models.ReturnType
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, SummaryReturnsService}
import utils.AtedConstants._

import scala.concurrent.Future

trait ReturnTypeController extends BackLinkController
  with AuthAction with ClientHelper {

  def summaryReturnService: SummaryReturnsService

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        currentBackLink.flatMap(backLink =>
          dataCacheConnector.fetchAndGetFormData[ReturnType](RetrieveReturnTypeFormId) map {
            case Some(data) => Ok(views.html.returnType(periodKey, returnTypeForm.fill(data), backLink))
            case _ => Ok(views.html.returnType(periodKey, returnTypeForm, backLink))
          }
        )
      }
    }
  }

  def submit(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        returnTypeForm.bindFromRequest.fold(
          formWithError =>
            currentBackLink.map(backLink =>
              BadRequest(views.html.returnType(periodKey, formWithError, backLink))
            ),
          returnTypeData => {
            dataCacheConnector.saveFormData[ReturnType](RetrieveReturnTypeFormId, returnTypeData)
            val returnUrl = Some(routes.ReturnTypeController.view(periodKey).url)
            summaryReturnService.getPreviousSubmittedLiabilityDetails(periodKey).flatMap { pastReturns =>
              (returnTypeData.returnType, pastReturns) match {
                case (Some("CR"), Nil) =>
                  RedirectWithBackLink(
                    AddressLookupController.controllerId,
                    controllers.propertyDetails.routes.AddressLookupController.view(None, periodKey),
                    returnUrl,
                    List(PropertyDetailsAddressController.controllerId)
                  )
                case (Some("CR"), _) =>
                  RedirectWithBackLink(
                    AddressLookupController.controllerId,
                    controllers.routes.ExistingReturnQuestionController.view(periodKey, returnType = "charge"),
                    returnUrl,
                    List(PropertyDetailsAddressController.controllerId)
                  )
                case (Some("RR"), _) =>
                  RedirectWithBackLink(
                    ChooseReliefsController.controllerId,
                    controllers.reliefs.routes.ChooseReliefsController.view(periodKey),
                    returnUrl
                  )
                case _ => Future.successful(Redirect(controllers.routes.ReturnTypeController.view(periodKey)))
              }
            }
          }
        )
      }
    }
  }
}

object ReturnTypeController extends ReturnTypeController {
  val delegationService: DelegationService = DelegationService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val summaryReturnService: SummaryReturnsService = SummaryReturnsService
  override val controllerId: String = "ReturnTypeController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
