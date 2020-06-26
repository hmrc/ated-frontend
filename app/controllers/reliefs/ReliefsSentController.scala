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

package controllers.reliefs

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.AuthAction
import javax.inject.Inject
import models.SubmitReturnsResponse
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReliefsService, ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants._

import scala.concurrent.ExecutionContext

class ReliefsSentController @Inject()(mcc : MessagesControllerComponents,
                                      authAction: AuthAction,
                                      subscriptionDataService: SubscriptionDataService,
                                      serviceInfoService: ServiceInfoService,
                                      val dataCacheConnector: DataCacheConnector,
                                      val reliefsService: ReliefsService)
                                     (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) {

  implicit val ec: ExecutionContext = mcc.executionContext

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        dataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](SubmitReturnsResponseFormId) map {
          case Some(submitResponse) =>
            Ok(views.html.reliefs.reliefsSent(periodKey, serviceInfoContent, submitResponse))
          case None =>
            Redirect(controllers.reliefs.routes.ReliefDeclarationController.view(periodKey))
        }
      }
    }
  }

  def viewPrintFriendlyReliefSent(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        submittedResponse <- dataCacheConnector.fetchAndGetFormData[SubmitReturnsResponse](SubmitReturnsResponseFormId)
      } yield {
        Ok(views.html.reliefs.reliefsSentPrintFriendly(periodKey, submittedResponse))
      }
    }
  }
}
