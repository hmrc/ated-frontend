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

package controllers

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.{AuthAction, ClientHelper}
import forms.AtedForms.YesNoQuestionExistingReturnsForm
import javax.inject.Inject
import models.SelectPeriod
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ServiceInfoService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.RetrieveSelectPeriodFormId
import utils.ReferrerUtils
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}

class ExistingReturnQuestionController @Inject()(mcc: MessagesControllerComponents,
                                                 authAction: AuthAction,
                                                 serviceInfoService: ServiceInfoService,
                                                 val dataCacheConnector: DataCacheConnector,
                                                 template: views.html.confirmPastReturn)
                                                (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  def view(periodKey: Int, returnType: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>

          val backLink = if (request.headers.get("referer").getOrElse("").endsWith("editSubmitted")) {
            ReferrerUtils.asRelativeUrl(request.headers.get("referer").get)
          } else {
            getBackLink(periodKey, returnType)
          }

          Future.successful(Ok(template(new YesNoQuestionExistingReturnsForm().yesNoQuestionForm, periodKey,
            returnType, serviceInfoContent, backLink)))
        }
      }
    }
  }

  def submit(periodKey: Int, returnType: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          dataCacheConnector.saveFormData[SelectPeriod](RetrieveSelectPeriodFormId, SelectPeriod(Some(periodKey.toString)))
          val form = new YesNoQuestionExistingReturnsForm
          form.yesNoQuestionForm.bindFromRequest().fold(
            formWithError =>
              Future.successful(BadRequest(template(formWithError, periodKey, returnType, serviceInfoContent, getBackLink(periodKey, returnType)))
              ),
            data => {
              val existingPastReturn = data.yesNo.getOrElse(false)
              if (existingPastReturn) {
                Future.successful(Redirect(controllers.propertyDetails.routes.SelectExistingReturnAddressController.view(periodKey, returnType)))
              } else {
                Future.successful(Redirect(controllers.propertyDetails.routes.AddressLookupController.view(None, periodKey)))
              }
            }
          )
        }
      }
    }
  }

  private def getBackLink(periodKey: Int, returnType: String) = Some(controllers.routes.ReturnTypeController.view(periodKey).url)
}

