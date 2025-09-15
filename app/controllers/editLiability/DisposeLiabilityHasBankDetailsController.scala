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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import forms.BankDetailForms.hasBankDetailsForm
import javax.inject.Inject
import models.HasBankDetails
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DisposeLiabilityReturnService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}


class DisposeLiabilityHasBankDetailsController @Inject()(mcc: MessagesControllerComponents,
                                                         disposeLiabilityReturnService: DisposeLiabilityReturnService,
                                                         authAction: AuthAction,
                                                         disposeLiabilityHasUkBankAccountController: DisposeLiabilityHasUkBankAccountController,
                                                         disposeLiabilitySummaryController: DisposeLiabilitySummaryController,
                                                         serviceInfoService: ServiceInfoService,
                                                         val dataCacheConnector: DataCacheConnector,
                                                         val backLinkCacheConnector: BackLinkCacheConnector,
                                                         template: views.html.editLiability.disposeLiabilityHasBankDetails)
                                                        (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkController with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "DisposeLiabilityDeclarationController"

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo) flatMap {
            case Some(x) =>
              currentBackLink.map { backLink =>
                val bankDetails = x.bankDetails.map(_.hasBankDetails)
                Ok(template(hasBankDetailsForm.fill(HasBankDetails(bankDetails)), oldFormBundleNo, serviceInfoContent, backLink))
              }
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
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
                val bankDetails = x.bankDetails.map(_.hasBankDetails)
                val backLink = Some(controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo).url)
                Ok(template(hasBankDetailsForm.fill(HasBankDetails(bankDetails)), oldFormBundleNo, serviceInfoContent, backLink))
              }
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
          }
        }
      }
    }
  }

  def save(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          hasBankDetailsForm.bindFromRequest().fold(
            formWithErrors =>
              currentBackLink.map(backLink =>
                BadRequest(template(formWithErrors, oldFormBundleNo, serviceInfoContent, backLink))
              ),
            bankData => {
              val hasBankDetails = bankData.hasBankDetails.getOrElse(false)
              val backLink = Some(controllers.editLiability.routes.DisposeLiabilityHasBankDetailsController.view(oldFormBundleNo).url)
              disposeLiabilityReturnService.cacheDisposeLiabilityReturnHasBankDetails(oldFormBundleNo, hasBankDetails) flatMap { _ =>
                if (hasBankDetails) {
                  disposeLiabilityReturnService.calculateDraftDisposal(oldFormBundleNo) flatMap {
                    case Some(_) =>
                      redirectWithBackLink(
                        disposeLiabilityHasUkBankAccountController.controllerId,
                        controllers.editLiability.routes.DisposeLiabilityHasUkBankAccountController.view(oldFormBundleNo),
                        backLink
                      )
                    case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
                  }
                } else {
                  redirectWithBackLink(
                    disposeLiabilitySummaryController.controllerId,
                    controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo),
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


