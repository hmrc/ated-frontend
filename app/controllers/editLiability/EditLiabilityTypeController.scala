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

package controllers.editLiability

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import controllers.propertyDetails.{AddressLookupController, PropertyDetailsAddressController}
import forms.AtedForms._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DelegationService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class EditLiabilityTypeController @Inject()(mcc: MessagesControllerComponents,
                                            propertyDetailsAddressController: PropertyDetailsAddressController,
                                            addressLookupController: AddressLookupController,
                                            authAction: AuthAction,
                                            disposePropertyController: DisposePropertyController,
                                            val dataCacheConnector: DataCacheConnector,
                                            val backLinkCacheConnector: BackLinkCacheConnector)
                                           (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkController with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "EditLiabilityTypeController"

  def editLiability(oldFormBundleNo: String, periodKey: Int, editAllowed: Boolean) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        Future.successful(
          Ok(views.html.editLiability
            .editLiability(editLiabilityReturnTypeForm, oldFormBundleNo, periodKey, editAllowed, returnToFormBundle(oldFormBundleNo, periodKey)))
        )
      }
    }
  }

  def continue(oldFormBundleNo: String, periodKey: Int, editAllowed: Boolean) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        editLiabilityReturnTypeForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(
              views.html.editLiability.editLiability(formWithErrors, oldFormBundleNo, periodKey, editAllowed, returnToFormBundle(oldFormBundleNo, periodKey)))),
          data => {
            val backLink = Some(controllers.editLiability.routes.EditLiabilityTypeController.editLiability(oldFormBundleNo, periodKey, editAllowed).url)
            data.editLiabilityType match {
              case Some("CR") =>
                redirectWithBackLink(
                  propertyDetailsAddressController.controllerId,
                  controllers.propertyDetails.routes.PropertyDetailsAddressController.editSubmittedReturn(oldFormBundleNo),
                  backLink,
                  List(addressLookupController.controllerId)
                )
              case Some("DP") =>
                redirectWithBackLink(
                  disposePropertyController.controllerId,
                  controllers.editLiability.routes.DisposePropertyController.view(oldFormBundleNo),
                  backLink
                )
              case _ =>
                Future.successful(Redirect(controllers.editLiability.routes.EditLiabilityTypeController.editLiability(oldFormBundleNo, periodKey, editAllowed)))
            }
          }
        )
      }
    }
  }

  private def returnToFormBundle(oldFormBundleNo: String, periodKey: Int) = {
    Some(controllers.routes.FormBundleReturnController.view(oldFormBundleNo, periodKey).url)
  }
}
