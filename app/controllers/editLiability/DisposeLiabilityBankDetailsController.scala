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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import forms.BankDetailForms
import forms.BankDetailForms._
import javax.inject.Inject
import models.BankDetails
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DisposeLiabilityReturnService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedUtils.sanitiseBankDetails

import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}

class DisposeLiabilityBankDetailsController @Inject()(mcc: MessagesControllerComponents,
                                                      disposeLiabilityReturnService: DisposeLiabilityReturnService,
                                                      authAction: AuthAction,
                                                      disposeLiabilitySummaryController: DisposeLiabilitySummaryController,
                                                      serviceInfoService: ServiceInfoService,
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val backLinkCacheConnector: BackLinkCacheConnector,
                                                      template: views.html.editLiability.disposeLiabilityBankDetails)
                                                     (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with ClientHelper with BackLinkController with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId = "DisposeLiabilityBankDetailsController"

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo) flatMap {
            case Some(x) =>
              currentBackLink.map { backLink =>
                val bankDetails = x.bankDetails.flatMap(_.bankDetails).fold(BankDetails())(a => a)
                Ok(template
                (bankDetailsForm.fill(bankDetails), oldFormBundleNo, serviceInfoContent, backLink)(authContext, implicitly, request, implicitly))
              }
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view))
          }
        }
      }
    }
  }

  def editFromSummary(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo) flatMap {
            case Some(x) =>
              Future.successful {
                val backLink = Some(controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo).url)
                val bankDetails = x.bankDetails.flatMap(_.bankDetails).fold(BankDetails())(a => a)
                Ok(template(bankDetailsForm.fill(bankDetails), oldFormBundleNo, serviceInfoContent, backLink))
              }
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view))
          }
        }
      }
    }
  }

  def save(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          BankDetailForms.validateBankDetails(bankDetailsForm.bindFromRequest()).fold(
            formWithErrors =>
              currentBackLink.map(backLink => BadRequest(template(formWithErrors, oldFormBundleNo, serviceInfoContent, backLink))),
            bankData => {
              disposeLiabilityReturnService.cacheDisposeLiabilityReturnBank(oldFormBundleNo, sanitiseBankDetails(bankData)) flatMap {
                _ => {
                  redirectWithBackLink(
                    disposeLiabilitySummaryController.controllerId,
                    controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo),
                    Some(controllers.editLiability.routes.DisposeLiabilityBankDetailsController.view(oldFormBundleNo).url)
                  )
                }
              }
            }
          )
        }
      }
    }
  }

}
