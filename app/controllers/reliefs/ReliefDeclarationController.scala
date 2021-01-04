/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import javax.inject.Inject
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DelegationService, ReliefsService, ServiceInfoService}
import uk.gov.hmrc.http.ForbiddenException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}


class ReliefDeclarationController @Inject()(mcc: MessagesControllerComponents,
                                            authAction: AuthAction,
                                            serviceInfoService: ServiceInfoService,
                                            val reliefsService: ReliefsService,
                                            val delegationService: DelegationService,
                                            val dataCacheConnector: DataCacheConnector,
                                            val backLinkCacheConnector: BackLinkCacheConnector,
                                            template: views.html.reliefs.reliefDeclaration,
                                            templateError: views.html.global_error)
                                           (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkController with ClientHelper with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "ReliefDeclarationController"

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          currentBackLink.flatMap(
            backLink =>
              Future.successful(Ok(template(periodKey, serviceInfoContent, backLink)))
          )
        }
      }
    }
  }

  def submit(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
          reliefsService.submitDraftReliefs(authContext.atedReferenceNumber, periodKey) flatMap { response =>
            response.status match {
              case OK => Future.successful(Redirect(controllers.reliefs.routes.ReliefsSentController.view(periodKey)))
              case BAD_REQUEST if response.body.contains("Agent not Valid") => {
                serviceInfoService.getPartial.flatMap { serviceInfoContent =>
                  Future.successful(BadRequest(templateError("ated.client-problem.title",
                    "ated.client-problem.header", "ated.client-problem.message", None,
                    Some(appConfig.agentRedirectedToMandate), Some("ated.client-problem.HrefMessage"), None, serviceInfoContent, appConfig)))
                }
              }
              case NOT_FOUND => Future.successful(Redirect(controllers.reliefs.routes.ReliefsSentController.view(periodKey)))
            }
          }
      }
    } recover {
      case _: ForbiddenException     =>
        logger.warn("[ReliefDeclarationController][submit] Forbidden exception")
        authAction.unauthorisedUrl()
    }
  }
}
