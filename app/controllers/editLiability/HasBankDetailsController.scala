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
import forms.BankDetailForms.hasBankDetailsForm
import models.HasBankDetails
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, BackLinkService, ChangeLiabilityReturnService, DataCacheService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasBankDetailsController @Inject()(mcc: MessagesControllerComponents,
                                         changeLiabilityReturnService: ChangeLiabilityReturnService,
                                         authAction: AuthAction,
                                         bankDetailsController: HasUkBankAccountController,
                                         serviceInfoService: ServiceInfoService,
                                         val dataCacheService: DataCacheService,
                                         val backLinkCacheService: BackLinkCacheService,
                                         template: views.html.editLiability.hasBankDetails)
                                        (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkService with ClientHelper with ControllerIds with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = hasBankDetailsId

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
            case Some(x) =>
              currentBackLink.map { backLink =>
                Ok(template(hasBankDetailsForm.fill(HasBankDetails(x.bankDetails.map(_.hasBankDetails))),
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
            case Some(propertyDetails) =>
              Future.successful {
                Ok(template(hasBankDetailsForm.fill(HasBankDetails(propertyDetails.bankDetails.map(_.hasBankDetails))),
                  oldFormBundleNo,
                  serviceInfoContent,
                  AtedUtils.getSummaryBackLink(oldFormBundleNo,
                    AtedUtils.getEditSubmittedMode(propertyDetails)))
                )
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
            formWithErrors => currentBackLink.map(backLink => BadRequest(template(formWithErrors,
              oldFormBundleNo,
              serviceInfoContent,
              backLink))),
            bankData => {
              val hasBankDetails = bankData.hasBankDetails.getOrElse(false)
              val backLink = Some(controllers.editLiability.routes.HasBankDetailsController.view(oldFormBundleNo).url)
              changeLiabilityReturnService.cacheChangeLiabilityReturnHasBankDetails(oldFormBundleNo, hasBankDetails) flatMap { _ =>
                if (hasBankDetails) {
                  redirectWithBackLink(
                    bankDetailsController.controllerId,
                    controllers.editLiability.routes.HasUkBankAccountController.view(oldFormBundleNo),
                    backLink
                  )
                } else {
                  redirectWithBackLink(
                    editLiabilitySummaryId,
                    controllers.editLiability.routes.EditLiabilitySummaryController.viewSummary(oldFormBundleNo),
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
