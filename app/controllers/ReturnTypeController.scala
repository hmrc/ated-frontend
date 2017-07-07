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

package controllers

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import controllers.propertyDetails.{AddressLookupController, PropertyDetailsAddressController}
import controllers.reliefs.ChooseReliefsController
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import forms.AtedForms.returnTypeForm
import models.ReturnType
import utils.AtedFeatureSwitches._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import services.SummaryReturnsService
import utils.AtedConstants._

import scala.concurrent.Future

trait ReturnTypeController extends BackLinkController
  with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  def summaryReturnService: SummaryReturnsService

  def view(periodKey: Int) = AuthAction(AtedRegime) {
    implicit user =>
      ensureClientContext {
        currentBackLink.flatMap(backLink =>
          dataCacheConnector.fetchAndGetFormData[ReturnType](RetrieveReturnTypeFormId) map {
            case Some(data) => Ok(views.html.returnType(periodKey, returnTypeForm.fill(data), backLink))
            case _ => Ok(views.html.returnType(periodKey, returnTypeForm, backLink))
          }
        )
      }
  }



  def submit(periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        returnTypeForm.bindFromRequest.fold(
          formWithError =>
            currentBackLink.map(backLink =>
              BadRequest(views.html.returnType(periodKey, formWithError, backLink))
            ),
          returnTypeData => {
            dataCacheConnector.saveFormData[ReturnType](RetrieveReturnTypeFormId, returnTypeData)
            val returnUrl = Some(routes.ReturnTypeController.view(periodKey).url)
            summaryReturnService.getPreviousSubmittedLiabilityDetails.flatMap { pastReturns =>
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

object ReturnTypeController extends ReturnTypeController {
  val delegationConnector = FrontendDelegationConnector
  val dataCacheConnector = DataCacheConnector
  val summaryReturnService = SummaryReturnsService
  override val controllerId = "ReturnTypeController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
