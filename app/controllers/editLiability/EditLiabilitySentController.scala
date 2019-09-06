/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.AtedBaseController
import controllers.auth.{AuthAction, ClientHelper}
import controllers.viewhelper.EditLiability._
import models.EditLiabilityReturnsResponseModel
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, SubscriptionDataService}
import utils.AtedConstants._

trait EditLiabilitySentController extends AtedBaseController
  with AuthAction with ClientHelper {

  def dataCacheConnector: DataCacheConnector
  def subscriptionDataService: SubscriptionDataService

  def view(oldFormBundleNo: String)
          : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>

        dataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel](SubmitEditedLiabilityReturnsResponseFormId) map {
          case Some(submitResponse) =>
            submitResponse.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo) match {
              case Some(resp) =>
                val returnType = returnTypeFromAmount(resp.amountDueOrRefund)
                Ok(views.html.editLiability.editLiabilitySent(oldFormBundleNo, returnType, resp.paymentReference,
                  resp.amountDueOrRefund, resp.liabilityAmount,
                  createHeaderMessages(returnType, "ated.edit-liability.sent.title"),
                  createHeaderMessages(returnType, "ated.edit-liability.sent.header")))
              case None => Redirect(controllers.routes.AccountSummaryController.view())
            }
          case None =>
            Logger.warn("[EditLiabilitySentController][view] - Return Response not found in cache")
            throw new RuntimeException("Return Response not found in cache")
      }
    }
  }

  def viewPrintFriendlyEditLilabilitySent(oldFormBundleNo: String)
                                         : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>

        for {
          submittedResponse <- dataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel](SubmitEditedLiabilityReturnsResponseFormId)
          organisationName <- subscriptionDataService.getOrganisationName
        } yield {
          val x = submittedResponse.get.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo)
          val returnType = returnTypeFromAmount(x.get.amountDueOrRefund)
          Ok(views.html.editLiability.editLiabilitySentPrintFriendly(submittedResponse, returnType, organisationName,
            x.get.paymentReference, x.get.amountDueOrRefund, x.get.liabilityAmount))
        }
    }
  }

}

object EditLiabilitySentController extends EditLiabilitySentController {
  val delegationService: DelegationService = DelegationService
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val subscriptionDataService: SubscriptionDataService = SubscriptionDataService
}
