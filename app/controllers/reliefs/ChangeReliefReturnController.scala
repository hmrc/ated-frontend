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

package controllers.reliefs

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import controllers.propertyDetails.AddressLookupController
import forms.AtedForms.editReliefForm
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DelegationService, ReliefsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class ChangeReliefReturnController @Inject()(mcc: MessagesControllerComponents,
                                             chooseReliefsController: ChooseReliefsController,
                                             authAction: AuthAction,
                                             addressLookupController: AddressLookupController,
                                             val reliefsService: ReliefsService,
                                             val dataCacheConnector: DataCacheConnector,
                                             val backLinkCacheConnector: BackLinkCacheConnector)
                                            (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkController with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId = "ChangeReliefReturnController"


  def viewChangeReliefReturn(periodKey: Int, formBundleNumber: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        currentBackLink.flatMap(backLink =>
          Future.successful(Ok(views.html.reliefs.changeReliefReturn(periodKey, formBundleNumber, editReliefForm, backLink)))
        )
      }
    }
  }

  def submit(periodKey: Int, formBundleNumber: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        editReliefForm.bindFromRequest.fold(
          formWithError =>
            currentBackLink.flatMap(backLink =>
              Future.successful(BadRequest(views.html.reliefs.changeReliefReturn(periodKey, formBundleNumber, formWithError, backLink)))
            ),
          editReliefData => {
            val returnUrl = Some(routes.ChangeReliefReturnController.viewChangeReliefReturn(periodKey, formBundleNumber).url)
            editReliefData.changeRelief match {
              case Some("changeDetails") =>
                redirectWithBackLink(
                  chooseReliefsController.controllerId,
                  controllers.reliefs.routes.ChooseReliefsController.view(periodKey),
                  returnUrl
                )
              case Some("createChargeable") =>
                redirectWithBackLink(
                  addressLookupController.controllerId,
                  controllers.propertyDetails.routes.AddressLookupController.view(None, periodKey),
                  returnUrl
                )
            }
          }
        )

      }
    }
  }

}
