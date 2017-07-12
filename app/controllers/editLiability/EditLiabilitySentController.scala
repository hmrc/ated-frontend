/*
 * Copyright 2017 HM Revenue & Customs
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

import config.FrontendDelegationConnector
import connectors.DataCacheConnector
import controllers.AtedBaseController
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import models.{EditLiabilityReturnsResponseModel, SubmitReturnsResponse}
import play.api.Logger
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import utils.AtedConstants._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import services.SubscriptionDataService

trait EditLiabilitySentController extends AtedBaseController
  with AtedFrontendAuthHelpers with DelegationAwareActions  with ClientHelper {

  def dataCacheConnector: DataCacheConnector
  def subscriptionDataService: SubscriptionDataService

  def view(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      dataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel](SubmitEditedLiabilityReturnsResponseFormId) map {
        case Some(submitResponse) =>
          submitResponse.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo) match {
            case Some(resp) =>
              val returnType = if (resp.amountDueOrRefund < BigDecimal(0)) "A" else if (resp.amountDueOrRefund > BigDecimal(0)) "F" else "C"
              Ok(views.html.editLiability.editLiabilitySent(oldFormBundleNo, returnType, resp.paymentReference, resp.amountDueOrRefund, resp.liabilityAmount))
            case None => Redirect(controllers.routes.AccountSummaryController.view())
          }
        case None =>
          Logger.warn("[EditLiabilitySentController][view] - Return Response not found in cache")
          throw new RuntimeException("Return Response not found in cache")
      }
  }

  def viewPrintFriendlyEditLilabilitySent(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      for {
        submittedResponse <- dataCacheConnector.fetchClientData[EditLiabilityReturnsResponseModel](SubmitEditedLiabilityReturnsResponseFormId)
        organisationName <- subscriptionDataService.getOrganisationName
      } yield {
        val x = submittedResponse.get.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo)
        val returnType = if (x.get.amountDueOrRefund < BigDecimal(0)) "A" else if (x.get.amountDueOrRefund > BigDecimal(0)) "F" else "C"
        Ok(views.html.editLiability.editLiabilitySentPrintFriendly(submittedResponse, returnType, organisationName, x.get.paymentReference, x.get.amountDueOrRefund, x.get.liabilityAmount))
      }
  }

}

object EditLiabilitySentController extends EditLiabilitySentController {
  override val dataCacheConnector = DataCacheConnector
  val delegationConnector = FrontendDelegationConnector
  val subscriptionDataService = SubscriptionDataService
}
