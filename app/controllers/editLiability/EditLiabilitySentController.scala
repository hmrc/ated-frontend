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
import connectors.DataCacheConnector
import controllers.auth.{AuthAction, ClientHelper}
import controllers.viewhelper.EditLiability._
import javax.inject.Inject
import models.EditLiabilityReturnsResponseModel
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DelegationService, ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants._

import scala.concurrent.ExecutionContext

class EditLiabilitySentController @Inject()(mcc: MessagesControllerComponents,
                                            subscriptionDataService: SubscriptionDataService,
                                            authAction: AuthAction,
                                            serviceInfoService: ServiceInfoService,
                                            val delegationService: DelegationService,
                                            val dataCacheConnector: DataCacheConnector)
                                           (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  def view(oldFormBundleNo: String)
          : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        dataCacheConnector.fetchAndGetFormData[EditLiabilityReturnsResponseModel](SubmitEditedLiabilityReturnsResponseFormId) map {
          case Some(submitResponse) =>
            submitResponse.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo) match {
              case Some(resp) =>
                val returnType = returnTypeFromAmount(resp.amountDueOrRefund)
                Ok(views.html.editLiability.editLiabilitySent(oldFormBundleNo, serviceInfoContent, returnType, resp.paymentReference,
                  resp.amountDueOrRefund, resp.liabilityAmount,
                  createHeadermessages(returnType, "ated.edit-liability.sent.title"),
                  createHeadermessages(returnType, "ated.edit-liability.sent.header")))
              case None => Redirect(controllers.routes.AccountSummaryController.view())
            }
          case None =>
            Logger.warn("[EditLiabilitySentController][view] - Return Response not found in cache")
            throw new RuntimeException("Return Response not found in cache")
        }
      }
    }
  }

  def viewPrintFriendlyEditLiabilitySent(oldFormBundleNo: String)
                                         : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>

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

