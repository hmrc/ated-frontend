/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.DataCacheConnector
import controllers.auth.{AuthAction, ClientHelper}
import javax.inject.Inject
import models.EditLiabilityReturnsResponseModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants._

import scala.concurrent.ExecutionContext


class DisposeLiabilitySentController @Inject()(mcc: MessagesControllerComponents,
                                               subscriptionDataService: SubscriptionDataService,
                                               authAction: AuthAction,
                                               serviceInfoService: ServiceInfoService,
                                               val dataCacheConnector: DataCacheConnector,
                                               template: views.html.editLiability.disposeLiabilitySent)
                                              (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        dataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel](SubmitEditedLiabilityReturnsResponseFormId) map {
          case Some(submitResponse) =>
            submitResponse.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo) match {
              case Some(r) => Ok(template(oldFormBundleNo, serviceInfoContent, r.amountDueOrRefund, r.liabilityAmount, r.paymentReference))
              case None => Redirect(controllers.routes.AccountSummaryController.view)
            }
          case None =>
            throw new RuntimeException("Return Response not found in cache")
        }
      }
    }
  }

  def viewPrintFriendlyDisposeLiabilitySent(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        submittedResponse <- dataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel](SubmitEditedLiabilityReturnsResponseFormId)
        organisationName <- subscriptionDataService.getOrganisationName
      } yield {
        val x = submittedResponse.get.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo)
        val returnType = if (x.get.amountDueOrRefund < BigDecimal(0)) "A" else if (x.get.amountDueOrRefund > BigDecimal(0)) "F" else "C"
        Ok(views.html.editLiability.disposeLiabilitySentPrintFriendly(submittedResponse, returnType,
          organisationName, x.get.paymentReference, x.get.amountDueOrRefund, x.get.liabilityAmount))
      }
    }
  }
}
