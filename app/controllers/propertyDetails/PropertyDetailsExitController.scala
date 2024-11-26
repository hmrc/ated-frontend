/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import controllers.auth.{AuthAction, ClientHelper}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import services._
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsExitController @Inject()(mcc: MessagesControllerComponents,
                                              authAction: AuthAction,
                                              val propertyDetailsService: PropertyDetailsService,
                                              val dataCacheConnector: DataCacheConnector,
                                              val backLinkCacheConnector: BackLinkCacheConnector,
                                              template: views.html.propertyDetails.propertyDetailsExit)
                                             (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsExitController"

  def view(): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        currentBackLink.flatMap { backLink => Future.successful(Ok(template(backLink)))}
      }
    }
  }
}

