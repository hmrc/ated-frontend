/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.{BackLinkCacheService, DataCacheService}
import controllers.BackLinkService
import controllers.auth.{AuthAction, ClientHelper}
import controllers.propertyDetails.{AddressLookupController, PropertyDetailsAddressController}
import forms.AtedForms._
import javax.inject.Inject
import models.EditLiabilityReturnType
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MessagesRequest}
import services.ServiceInfoService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}

class EditLiabilityTypeController @Inject()(mcc: MessagesControllerComponents,
                                            propertyDetailsAddressController: PropertyDetailsAddressController,
                                            addressLookupController: AddressLookupController,
                                            authAction: AuthAction,
                                            disposePropertyController: DisposePropertyController,
                                            serviceInfoService: ServiceInfoService,
                                            val dataCacheService: DataCacheService,
                                            val backLinkCacheService: BackLinkCacheService,
                                            template: views.html.editLiability.editLiability)
                                           (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkService with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "EditLiabilityTypeController"

  def editLiability(oldFormBundleNo: String, periodKey: Int, editAllowed: Boolean) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          Future.successful(
            Ok(template(editLiabilityPrePop(editLiabilityReturnTypeForm), oldFormBundleNo, periodKey, editAllowed, serviceInfoContent, returnToFormBundle(oldFormBundleNo, periodKey)))
          )
        }
      }
    }
  }

  def editLiabilityPrePop(form: Form[EditLiabilityReturnType])(implicit request: MessagesRequest[AnyContent]): Form[EditLiabilityReturnType] = {
    request.getQueryString("disposal") match {
      case Some("true") => form.fill(EditLiabilityReturnType(editLiabilityType = Some("DP")))
      case Some("false") => form.fill(EditLiabilityReturnType(editLiabilityType = Some("CR")))
      case _ => form
    }
  }

  def continue(oldFormBundleNo: String, periodKey: Int, editAllowed: Boolean) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          editLiabilityReturnTypeForm.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(
                template(formWithErrors, oldFormBundleNo, periodKey, editAllowed, serviceInfoContent, returnToFormBundle(oldFormBundleNo, periodKey)))),
            data => {
              val backLink = Some(controllers.editLiability.routes.EditLiabilityTypeController.editLiability(oldFormBundleNo, periodKey, editAllowed).url)
              data.editLiabilityType match {
                case Some("CR") =>
                  redirectWithBackLink(
                    propertyDetailsAddressController.controllerId,
                    controllers.propertyDetails.routes.PropertyDetailsAddressController.editSubmittedReturn(oldFormBundleNo),
                    backLink.map(_.concat("&disposal=false")),
                    List(addressLookupController.controllerId)
                  )
                case Some("DP") =>
                  redirectWithBackLink(
                    disposePropertyController.controllerId,
                    controllers.editLiability.routes.DisposePropertyController.view(oldFormBundleNo),
                    backLink.map(_.concat("&disposal=true"))
                  )
                case _ =>
                  Future.successful(Redirect(controllers.editLiability.routes.EditLiabilityTypeController.editLiability(oldFormBundleNo, periodKey, editAllowed)))
              }
            }
          )
        }
      }
    }
  }

  private def returnToFormBundle(oldFormBundleNo: String, periodKey: Int) = {
    Some(controllers.routes.FormBundleReturnController.view(oldFormBundleNo, periodKey).url)
  }
}
