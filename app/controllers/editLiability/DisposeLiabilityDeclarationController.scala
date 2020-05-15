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
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DisposeLiabilityReturnService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class DisposeLiabilityDeclarationController @Inject()(mcc: MessagesControllerComponents,
                                                      disposeLiabilityReturnService: DisposeLiabilityReturnService,
                                                      authAction: AuthAction,
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val backLinkCacheConnector: BackLinkCacheConnector)
                                                      (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkController with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "DisposeLiabilityDeclarationController"

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        currentBackLink.map(backLink =>
          Ok(views.html.editLiability.disposeLiabilityDeclaration(oldFormBundleNo, backLink))
        )
      }
    }
  }

  def submit(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        disposeLiabilityReturnService.submitDraftDisposeLiability(oldFormBundleNo) map {
          response =>
            response.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo) match {
              case Some(_) => Redirect(controllers.editLiability.routes.DisposeLiabilitySentController.view(oldFormBundleNo))
              case None => Redirect(controllers.routes.AccountSummaryController.view())
            }
        }
      }
    }
  }
}
