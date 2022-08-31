/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.AtedForms.returnTypeForm
import javax.inject.Inject
import models.ReturnType
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ServiceInfoService, SummaryReturnsService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants._
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}

class ReturnTypeController @Inject()(mcc: MessagesControllerComponents,
                                     authAction: AuthAction,
                                     summaryReturnService: SummaryReturnsService,
                                     serviceInfoService: ServiceInfoService,
                                     val dataCacheConnector: DataCacheConnector,
                                     val backLinkCacheConnector: BackLinkCacheConnector,
                                     template: views.html.returnType)
                                    (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkController with ClientHelper with ControllerIds with WithDefaultFormBinding {

  val controllerId: String = "ReturnTypeController"
  implicit val ec: ExecutionContext = mcc.executionContext

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          currentBackLink.flatMap(backLink =>
            dataCacheConnector.fetchAndGetFormData[ReturnType](RetrieveReturnTypeFormId) map {
              case Some(data) => Ok(template(periodKey, returnTypeForm.fill(data), serviceInfoContent, backLink))
              case _ => Ok(template(periodKey, returnTypeForm, serviceInfoContent, backLink))
            }
          )
        }
      }
    }
  }

  def submit(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          returnTypeForm.bindFromRequest.fold(
            formWithError =>
              currentBackLink.map(backLink =>
                BadRequest(template(periodKey, formWithError, serviceInfoContent, backLink))
              ),
            returnTypeData => {
              dataCacheConnector.saveFormData[ReturnType](RetrieveReturnTypeFormId, returnTypeData)
              val returnUrl = Some(routes.ReturnTypeController.view(periodKey).url)
              summaryReturnService.getPreviousSubmittedLiabilityDetails(periodKey).flatMap { pastReturns =>
                (returnTypeData.returnType, pastReturns) match {
                  case (Some("CR"), Nil) =>
                    redirectWithBackLink(
                      addressLookupId,
                      controllers.propertyDetails.routes.AddressLookupController.view(None, periodKey),
                      returnUrl,
                      List(propertyDetailsAddressId)
                    )
                  case (Some("CR"), _) =>
                    redirectWithBackLink(
                      addressLookupId,
                      controllers.routes.ExistingReturnQuestionController.view(periodKey, returnType = "charge"),
                      returnUrl,
                      List(propertyDetailsAddressId)
                    )
                  case (Some("RR"), _) =>
                    redirectWithBackLink(
                      chooseReliefsControllerId,
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
}
