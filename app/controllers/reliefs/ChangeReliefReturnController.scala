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

package controllers.reliefs

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.{AtedBaseController, BackLinkController, routes}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import controllers.propertyDetails.AddressLookupController
import services.{ReliefsService, SubscriptionDataService}
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import forms.AtedForms.editReliefForm

import scala.concurrent.Future

trait ChangeReliefReturnController extends BackLinkController with AtedBaseController with AtedFrontendAuthHelpers with DelegationAwareActions
with ClientHelper{

  def reliefsService: ReliefsService

  def viewChangeReliefReturn(periodKey: Int, formBundleNumber: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        currentBackLink.flatMap(backLink =>
          Future.successful(Ok(views.html.reliefs.changeReliefReturn(periodKey, formBundleNumber, editReliefForm, backLink)))
        )
      }
  }

  def submit(periodKey: Int, formBundleNumber: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        editReliefForm.bindFromRequest.fold(
          formWithError =>
            currentBackLink.flatMap(backLink =>
              Future.successful(BadRequest(views.html.reliefs.changeReliefReturn(periodKey, formBundleNumber, formWithError, backLink)))
            ),
          edtitReliefData => {
            val returnUrl = Some(routes.ChangeReliefReturnController.viewChangeReliefReturn(periodKey, formBundleNumber).url)
            edtitReliefData.changeRelief match {
              case Some("changeDetails") =>
                RedirectWithBackLink(
                  ChooseReliefsController.controllerId,
                  controllers.reliefs.routes.ChooseReliefsController.view(periodKey),
                  returnUrl
                )
              case Some("createChargeable") =>
                RedirectWithBackLink(
                  AddressLookupController.controllerId,
                  controllers.propertyDetails.routes.AddressLookupController.view(None, periodKey),
                  returnUrl
                )
            }
          }
        )

      }
  }




  }






object ChangeReliefReturnController extends ChangeReliefReturnController {
  val delegationConnector = FrontendDelegationConnector
  val reliefsService = ReliefsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "ChangeReliefReturnController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
