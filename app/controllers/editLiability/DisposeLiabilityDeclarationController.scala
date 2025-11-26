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
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, BackLinkService, DataCacheService, DisposeLiabilityReturnService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

class DisposeLiabilityDeclarationController @Inject()(mcc: MessagesControllerComponents,
                                                      disposeLiabilityReturnService: DisposeLiabilityReturnService,
                                                      authAction: AuthAction,
                                                      serviceInfoService: ServiceInfoService,
                                                      val dataCacheService: DataCacheService,
                                                      val backLinkCacheService: BackLinkCacheService,
                                                      template: views.html.editLiability.disposeLiabilityDeclaration)
                                                      (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkService with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "DisposeLiabilityDeclarationController"

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          currentBackLink.map(backLink =>
            Ok(template(oldFormBundleNo, serviceInfoContent, backLink))
          )
        }
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
