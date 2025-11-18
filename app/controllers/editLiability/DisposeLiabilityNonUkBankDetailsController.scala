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
import connectors.{BackLinkCacheService, DataCacheService}
import controllers.BackLinkService
import controllers.auth.{AuthAction, ClientHelper}
import forms.BankDetailForms
import forms.BankDetailForms.bankDetailsForm
import models.BankDetails
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DisposeLiabilityReturnService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedUtils.sanitiseBankDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DisposeLiabilityNonUkBankDetailsController @Inject()(mcc: MessagesControllerComponents,
                                                           disposeLiabilityReturnService: DisposeLiabilityReturnService,
                                                           authAction: AuthAction,
                                                           disposeLiabilitySummaryController: DisposeLiabilitySummaryController,
                                                           serviceInfoService: ServiceInfoService,
                                                           val dataCacheService: DataCacheService,
                                                           val backLinkCacheService: BackLinkCacheService,
                                                           template: views.html.editLiability.disposeLiabilityNonUkBankDetails)
                                                          (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with ClientHelper with BackLinkService with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId = "DisposeLiabilityNonUkBankDetailsController"

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo) flatMap {
            case Some(x) =>
              currentBackLink.map { backLink =>
                val bankDetails = x.bankDetails.flatMap(_.bankDetails).fold(BankDetails())(a => a)
                Ok(template
                (bankDetailsForm.fill(bankDetails), oldFormBundleNo, serviceInfoContent, backLink)
                (authContext, implicitly, request, implicitly))
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
          BankDetailForms.validateBankDetails(controllerId, bankDetailsForm.bindFromRequest()).fold(
            formWithErrors =>
              currentBackLink.map(backLink => BadRequest(template(formWithErrors, oldFormBundleNo, serviceInfoContent, backLink))),
            bankData => {
              disposeLiabilityReturnService.cacheDisposeLiabilityReturnBank(oldFormBundleNo,
                sanitiseBankDetails(bankData).copy(hasUKBankAccount = Option(false))) flatMap {
                _ => {
                  redirectWithBackLink(
                    disposeLiabilitySummaryController.controllerId,
                    controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo),
                    Some(controllers.editLiability.routes.DisposeLiabilityNonUkBankDetailsController.view(oldFormBundleNo).url)
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

