/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.viewhelper.EditLiability.*

import javax.inject.Inject
import models.EditLiabilityReturnsResponseModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DataCacheService, DelegationService, ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.*

import scala.concurrent.{ExecutionContext, Future}

class EditLiabilitySentController @Inject()(mcc: MessagesControllerComponents,
                                            subscriptionDataService: SubscriptionDataService,
                                            authAction: AuthAction,
                                            serviceInfoService: ServiceInfoService,
                                            val delegationService: DelegationService,
                                            val dataCacheService: DataCacheService,
                                            template: views.html.editLiability.editLiabilitySent)
                                           (using val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with ClientHelper {

  given ec: ExecutionContext = mcc.executionContext

  def view(oldFormBundleNo: String)
          : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        dataCacheService.fetchAndGetData[EditLiabilityReturnsResponseModel](SubmitEditedLiabilityReturnsResponseFormId) map {
          case Some(submitResponse) =>
            submitResponse.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo) match {
              case Some(resp) =>
                val returnType = returnTypeFromAmount(resp.amountDueOrRefund)
                Ok(template(oldFormBundleNo, serviceInfoContent, returnType, resp.paymentReference,
                  resp.amountDueOrRefund, resp.liabilityAmount,
                  createHeadermessages(returnType, "ated.edit-liability.sent.title"),
                  createHeadermessages(returnType, "ated.edit-liability.sent.header")))
              case None => Redirect(controllers.routes.AccountSummaryController.view())
            }
          case None =>
            logger.warn("[EditLiabilitySentController][view] - Return Response not found in cache")
            throw new RuntimeException("Return Response not found in cache")
        }
      }
    }
  }

  def viewPrintFriendlyEditLiabilitySent(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>

      dataCacheService.fetchAndGetData[EditLiabilityReturnsResponseModel](SubmitEditedLiabilityReturnsResponseFormId).flatMap {
        case None =>
          logger.warn(
            s"[EditLiabilitySentController][viewPrintFriendlyEditLiabilitySent] " +
              s"No cached EditLiabilityReturnsResponseModel found for formBundleNo: $oldFormBundleNo"
          )
          Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))

        case Some(submittedResponse) =>
            submittedResponse.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo)

          match {
            case None =>
              logger.warn(
                s"[EditLiabilitySentController][viewPrintFriendlyEditLiabilitySent] " +
                  s"No Liability Return Response found in cache for formBundleNo: $oldFormBundleNo"
              )
              Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))

            case Some(liabilityReturn) =>
              subscriptionDataService.getOrganisationName.map { organisationNameOpt =>

                val returnType = returnTypeFromAmount(liabilityReturn.amountDueOrRefund)

                Ok(
                  views.html.editLiability.editLiabilitySentPrintFriendly(
                    Some(submittedResponse),
                    returnType,
                    organisationNameOpt,
                    liabilityReturn.paymentReference,
                    liabilityReturn.amountDueOrRefund,
                    liabilityReturn.liabilityAmount
                  )
                )
              }
          }
      }
    }
  }


}

