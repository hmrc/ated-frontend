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
import forms.BankDetailForms
import forms.BankDetailForms.bankDetailsForm
import models.BankDetails
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, BackLinkService, ChangeLiabilityReturnService, DataCacheService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedUtils
import utils.AtedUtils.sanitiseBankDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NonUkBankDetailsController @Inject()(mcc: MessagesControllerComponents,
                                           changeLiabilityReturnService: ChangeLiabilityReturnService,
                                           authAction: AuthAction,
                                           serviceInfoService: ServiceInfoService,
                                           val dataCacheService: DataCacheService,
                                           val backLinkCacheService: BackLinkCacheService,
                                           template: views.html.editLiability.nonUkBankDetails)
                                          (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkService with ClientHelper with ControllerIds with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = nonUkBankDetailsControllerId

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
            case Some(x) =>
              currentBackLink.map { backLink =>
                val bankDetails = x.bankDetails.flatMap(_.bankDetails).fold(BankDetails())(a => a)
                Ok(template(bankDetailsForm.fill(bankDetails),
                  oldFormBundleNo,
                  serviceInfoContent,
                  backLink))
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
          changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
            case Some(x) =>
              Future.successful {
                val mode = AtedUtils.getEditSubmittedMode(x)
                val bankUkDetails = x.bankDetails.flatMap(_.bankDetails).fold(BankDetails())(a => a)
                Ok(template(bankDetailsForm.fill(bankUkDetails),
                  oldFormBundleNo,
                  serviceInfoContent,
                  AtedUtils.getSummaryBackLink(oldFormBundleNo, mode)))
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
          BankDetailForms.validateBankDetails(controllerId, bankDetailsForm.bindFromRequest()).fold(
            formWithErrors =>
              currentBackLink.map(backLink => BadRequest(template(formWithErrors,
                oldFormBundleNo,
                serviceInfoContent,
                backLink))),
            bankData => {
              changeLiabilityReturnService.cacheChangeLiabilityReturnBank(oldFormBundleNo,
                sanitiseBankDetails(bankData).copy(hasUKBankAccount = Option(false))) flatMap { _ => {
                redirectWithBackLink(
                  editLiabilitySummaryId,
                  controllers.editLiability.routes.EditLiabilitySummaryController.viewSummary(oldFormBundleNo),
                  Some(controllers.editLiability.routes.NonUkBankDetailsController.view(oldFormBundleNo).url)
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