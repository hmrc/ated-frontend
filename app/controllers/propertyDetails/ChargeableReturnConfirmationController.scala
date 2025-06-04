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

package controllers.propertyDetails

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.AuthAction
import javax.inject.Inject
import models.SubmitReturnsResponse
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants._

import scala.concurrent.ExecutionContext

class ChargeableReturnConfirmationController @Inject()(mcc: MessagesControllerComponents,
                                                       subscriptionDataService: SubscriptionDataService,
                                                       authAction: AuthAction,
                                                       serviceInfoService: ServiceInfoService,
                                                       val dataCacheConnector: DataCacheConnector,
                                                       template: views.html.propertyDetails.chargeableReturnsConfirmation)
                                                      (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def confirmation : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        dataCacheConnector.fetchAndGetData[SubmitReturnsResponse](SubmitReturnsResponseFormId) map {
          case Some(submitResponse) =>
            Ok(template(submitResponse, serviceInfoContent))
          case None =>
            logger.warn("[ChargeableReturnConfirmationController][confirmation] - Return Response not found in cache")
            Redirect(controllers.routes.AccountSummaryController.view)
        }
      }
    }
  }

  def viewPrintFriendlyChargeableConfirmation : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        submitedResponse <- dataCacheConnector.fetchAndGetData[SubmitReturnsResponse](SubmitReturnsResponseFormId)
        organisationName <- subscriptionDataService.getOrganisationName
      } yield {
        Ok(views.html.propertyDetails.chargeableConfirmationPrintFriendly(submitedResponse, organisationName))
      }
    }
  }

}


