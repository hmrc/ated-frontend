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
import controllers.auth.{AuthAction, ClientHelper}
import controllers.{BackLinkService, ControllerIds}
import forms.BankDetailForms.hasUkBankAccountForm
import models.{BankDetails, HasUkBankAccount}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ChangeLiabilityReturnService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasUkBankAccountController @Inject()(mcc: MessagesControllerComponents,
                                           changeLiabilityReturnService: ChangeLiabilityReturnService,
                                           authAction: AuthAction,
                                           serviceInfoService: ServiceInfoService,
                                           val dataCacheService: DataCacheService,
                                           val backLinkCacheService: BackLinkCacheService,
                                           template: views.html.editLiability.hasUkBankAccount)
                                          (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkService with ClientHelper with ControllerIds with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = hasUkBankAccountControllerId

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
            case Some(propertyDetails) =>
              currentBackLink.map { backLink =>
                Ok(template(hasUkBankAccountForm.fill(
                  HasUkBankAccount(
                    propertyDetails.bankDetails
                    .flatMap(_.bankDetails)
                    .fold(BankDetails())(a => a)
                      .hasUKBankAccount)),
                  oldFormBundleNo,
                  serviceInfoContent,
                  backLink))
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
          changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
            case Some(propertyDetails) =>
              Future.successful {
                Ok(template(hasUkBankAccountForm.fill(HasUkBankAccount(
                  propertyDetails.bankDetails
                    .flatMap(_.bankDetails)
                    .fold(BankDetails())(a => a)
                    .hasUKBankAccount)),
                  oldFormBundleNo,
                  serviceInfoContent,
                  AtedUtils.getSummaryBackLink(oldFormBundleNo, AtedUtils.getEditSubmittedMode(propertyDetails))))
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
              currentBackLink.map(backLink =>
                BadRequest(template(formWithErrors,
                  oldFormBundleNo,
                  serviceInfoContent,
                  backLink))),
            bankData => {
              val backLink = Some(controllers.editLiability.routes.HasUkBankAccountController.view(oldFormBundleNo).url)
              val hasUkBankAccount = bankData.hasUkBankAccount.get
              changeLiabilityReturnService.cacheChangeLiabilityHasUkBankAccount(oldFormBundleNo, hasUkBankAccount) flatMap { _ => {
                if (hasUkBankAccount) {
                  redirectWithBackLink(
                    ukBankDetailsControllerId,
                    controllers.editLiability.routes.UkBankDetailsController.view(oldFormBundleNo),
                    backLink
                  )
                } else {
                  redirectWithBackLink(
                    nonUkBankDetailsControllerId,
                    controllers.editLiability.routes.NonUkBankDetailsController.view(oldFormBundleNo),
                    backLink
                  )
                }
              }
              }
            }
          )
        }
      }
    }
  }
}
