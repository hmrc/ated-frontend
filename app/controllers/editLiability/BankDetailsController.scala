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
import controllers.auth.{AuthAction, ClientHelper}
import controllers.{BackLinkController, ControllerIds}
import forms.BankDetailForms
import forms.BankDetailForms.bankDetailsForm
import javax.inject.Inject
import models.BankDetails
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ChangeLiabilityReturnService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class BankDetailsController @Inject()(mcc: MessagesControllerComponents,
                                      changeLiabilityReturnService: ChangeLiabilityReturnService,
                                      authAction: AuthAction,
                                      val dataCacheConnector: DataCacheConnector,
                                      val backLinkCacheConnector: BackLinkCacheConnector)(implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkController with ClientHelper with ControllerIds {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = bankDetailsControllerId

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
        ensureClientContext {
          changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
            case Some(x) =>
              currentBackLink.map { backLink =>
                val bankDetails = x.bankDetails.flatMap(_.bankDetails).fold(BankDetails())(a => a)
                Ok(views.html.editLiability.bankDetails(bankDetailsForm.fill(bankDetails), oldFormBundleNo, backLink))
              }
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
          }
        }
      }
  }

  def save(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        BankDetailForms.validateBankDetails(bankDetailsForm.bindFromRequest).fold(
          formWithErrors =>
            currentBackLink.map(backLink => BadRequest(views.html.editLiability.bankDetails(formWithErrors, oldFormBundleNo, backLink))),
          bankData => {
            changeLiabilityReturnService.cacheChangeLiabilityReturnBank(oldFormBundleNo, bankData) flatMap { _ => {
              redirectWithBackLink(
                editLiabilitySummaryId,
                controllers.editLiability.routes.EditLiabilitySummaryController.viewSummary(oldFormBundleNo),
                Some(controllers.editLiability.routes.BankDetailsController.view(oldFormBundleNo).url)
              )
            }
            }
          }
        )
      }
    }
  }
}
