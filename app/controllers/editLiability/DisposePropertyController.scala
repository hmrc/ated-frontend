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
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import forms.AtedForms.disposeLiabilityForm
import javax.inject.Inject
import models.DisposeLiability
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DisposeLiabilityReturnService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class DisposePropertyController @Inject()(mcc: MessagesControllerComponents,
                                          disposeLiabilityReturnService: DisposeLiabilityReturnService,
                                          authAction: AuthAction,
                                          disposeLiabilityHasBankDetailsController: DisposeLiabilityHasBankDetailsController,
                                          serviceInfoService: ServiceInfoService,
                                          val dataCacheConnector: DataCacheConnector,
                                          val backLinkCacheConnector: BackLinkCacheConnector,
                                          template: views.html.editLiability.dataOfDisposal)
                                         (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkController with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "DisposePropertyController"

  def view(oldFormBundleNo: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          disposeLiabilityOpt <- disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo)
          serviceInfoContent <- serviceInfoService.getPartial
          result <-
          disposeLiabilityOpt match {
            case Some(x) =>
              currentBackLink.map { backLink =>
                val filledForm = disposeLiabilityForm.fill(x.disposeLiability.fold(DisposeLiability(periodKey = x.formBundleReturn.periodKey.toInt))(a => a))
                Ok(template(filledForm, oldFormBundleNo, serviceInfoContent, backLink, x.formBundleReturn.periodKey.toInt))
              }
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
          }
        } yield result
      }
    }
  }

  def editFromSummary(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          disposeLiabilityOpt <- disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo)
          serviceInfoContent <- serviceInfoService.getPartial
          result <-
          disposeLiabilityOpt.flatMap(_.disposeLiability) match {
            case Some(x) =>
              Future.successful {
                val backLink = Some(controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo).url)
                Ok(template(disposeLiabilityForm.fill(x), oldFormBundleNo, serviceInfoContent, backLink, x.periodKey))
              }
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
          }
        } yield result
      }
    }
  }

  def save(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          disposeLiabilityForm.bindFromRequest.fold(
            formWithErrors => {
              currentBackLink.map { backLink =>
                BadRequest(
                  template(
                    formWithErrors,
                    oldFormBundleNo,
                    serviceInfoContent,
                    backLink,
                    formWithErrors.data("periodKey").toInt
                  )
                )
              }
            },
            disposalDate => disposeLiabilityReturnService.cacheDisposeLiabilityReturnDate(oldFormBundleNo, disposalDate) flatMap {
              _ =>
                redirectWithBackLink(
                  disposeLiabilityHasBankDetailsController.controllerId,
                  controllers.editLiability.routes.DisposeLiabilityHasBankDetailsController.view(oldFormBundleNo),
                  Some(controllers.editLiability.routes.DisposePropertyController.view(oldFormBundleNo).url)
                )
            }
          )
        }
      }
    }
  }
}


