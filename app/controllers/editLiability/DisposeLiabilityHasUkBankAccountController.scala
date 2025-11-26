/*
 * Copyright 2025 HM Revenue & Customs
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
import controllers.auth.{AuthAction, ClientHelper}
import controllers.ControllerIds
import forms.BankDetailForms.hasUkBankAccountForm
import models.{BankDetails, HasUkBankAccount}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, BackLinkService, DataCacheService, DisposeLiabilityReturnService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DisposeLiabilityHasUkBankAccountController @Inject()(mcc: MessagesControllerComponents,
                                                           disposeLiabilityReturnService: DisposeLiabilityReturnService,
                                                           authAction: AuthAction,
                                                           disposeLiabilityUkBankDetailsController: DisposeLiabilityUkBankDetailsController,
                                                           disposeLiabilityNonUkBankDetailsController: DisposeLiabilityNonUkBankDetailsController,
                                                           serviceInfoService: ServiceInfoService,
                                                           val dataCacheService: DataCacheService,
                                                           val backLinkCacheService: BackLinkCacheService,
                                                           template: views.html.editLiability.disposeLiabilityHasUkBankAccount)
                                                          (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkService with ClientHelper with ControllerIds with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = disposeLiabilityHasUkBankAccountController

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo) flatMap {
            case Some(disposeLiabilityReturn) =>
              Future.successful(Ok(template(hasUkBankAccountForm.fill(
                HasUkBankAccount(disposeLiabilityReturn.bankDetails.flatMap(_.bankDetails)
                  .fold(BankDetails())(a => a).hasUKBankAccount)),
                oldFormBundleNo,
                serviceInfoContent,
                Some(controllers.editLiability.routes.DisposeLiabilityHasBankDetailsController.view(oldFormBundleNo).url))))
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
            case Some(disposeLiabilityReturn) =>
              Future.successful {
                Ok(template(hasUkBankAccountForm.fill(
                  HasUkBankAccount(disposeLiabilityReturn.bankDetails.flatMap(_.bankDetails)
                    .fold(BankDetails())(a => a).hasUKBankAccount)),
                  oldFormBundleNo,
                  serviceInfoContent,
                  Some(controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo).url)))
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
          hasUkBankAccountForm.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(template(formWithErrors,
                oldFormBundleNo,
                serviceInfoContent,
                Some(controllers.editLiability.routes.DisposeLiabilityHasBankDetailsController.view(oldFormBundleNo).url)))),
            bankData => {
              val backLink = Some(controllers.editLiability.routes.DisposeLiabilityHasUkBankAccountController.view(oldFormBundleNo).url)
              val hasUkBankAccount = bankData.hasUkBankAccount.get
              disposeLiabilityReturnService.cacheDisposeLiabilityReturnHasUkBankAccount(oldFormBundleNo, hasUkBankAccount) flatMap { _ =>
                if (hasUkBankAccount) {
                  redirectWithBackLink(
                    disposeLiabilityUkBankDetailsController.controllerId,
                    controllers.editLiability.routes.DisposeLiabilityUkBankDetailsController.view(oldFormBundleNo),
                    backLink
                  )
                } else {
                  redirectWithBackLink(
                    disposeLiabilityNonUkBankDetailsController.controllerId,
                    controllers.editLiability.routes.DisposeLiabilityNonUkBankDetailsController.view(oldFormBundleNo),
                    backLink
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
