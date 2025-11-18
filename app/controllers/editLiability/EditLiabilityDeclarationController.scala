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
import connectors.{BackLinkCacheService, DataCacheService}
import controllers.auth.{AuthAction, ClientHelper}
import controllers.{BackLinkService, ControllerIds}
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ChangeLiabilityReturnService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class EditLiabilityDeclarationController @Inject()(mcc: MessagesControllerComponents,
                                                   changeLiabilityReturnService: ChangeLiabilityReturnService,
                                                   authAction: AuthAction,
                                                   serviceInfoService: ServiceInfoService,
                                                   val dataCacheService: DataCacheService,
                                                   val backLinkCacheService: BackLinkCacheService,
                                                   template: views.html.editLiability.editLiabilityDeclaration)
                                                  (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkService with ClientHelper with ControllerIds {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = editLiabilityDeclarationId

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo) flatMap {
            case Some(x) =>
              val returnType = getReturnType(x.calculated.flatMap(_.amountDueOrRefund))
              currentBackLink.map(backLink =>
                Ok(template(oldFormBundleNo, returnType, serviceInfoContent, backLink))
              )
            case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
          }
        }
      }
    }
  }

  def submit(oldFormBundleNo: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        changeLiabilityReturnService.submitDraftChangeLiability(oldFormBundleNo) map {
          response =>
            response.liabilityReturnResponse.find(_.oldFormBundleNumber == oldFormBundleNo) match {
              case Some(_) =>
                Redirect(controllers.editLiability.routes.EditLiabilitySentController.view(oldFormBundleNo))
              case None =>
                Redirect(controllers.routes.AccountSummaryController.view())
            }
        }
      }
    }
  }

  private def getReturnType(amountDueOrRefund: Option[BigDecimal]) = {
    amountDueOrRefund.fold("C")(a => if (a > 0) "F" else if (a < 0) "A" else "C")
  }
}
