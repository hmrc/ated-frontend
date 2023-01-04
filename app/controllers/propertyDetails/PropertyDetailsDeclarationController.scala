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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsDeclarationController @Inject()(mcc: MessagesControllerComponents,
                                                     authAction: AuthAction,
                                                     serviceInfoService: ServiceInfoService,
                                                     val propertyDetailsService: PropertyDetailsService,
                                                     val dataCacheConnector: DataCacheConnector,
                                                     val backLinkCacheConnector: BackLinkCacheConnector,
                                                     template: views.html.propertyDetails.propertyDetailsDeclaration,
                                                     templateError: views.html.global_error)
                                                    (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId = "PropertyDetailsDeclarationController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(response) =>
              currentBackLink.map(backLink =>
                response.calculated match {
                  case Some(_) => Ok(template(id, serviceInfoContent, backLink))
                  case _ => Redirect(routes.PropertyDetailsSummaryController.view(id))
                }
              )
          }
        }
      }
    }
  }

  def submit(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(cacheResponse) =>
              cacheResponse.calculated match {
                case Some(_) =>
                  propertyDetailsService.submitDraftPropertyDetails(id) flatMap { response =>
                    response.status match {
                      case OK => Future.successful(Redirect(controllers.propertyDetails.routes.ChargeableReturnConfirmationController.confirmation))
                      case BAD_REQUEST if response.body.contains("Agent not Valid") =>
                        Future.successful(BadRequest(templateError("ated.client-problem.title",
                          "ated.client-problem.header", "ated.client-problem.message", None, Some(appConfig.agentRedirectedToMandate), None, None, serviceInfoContent)))
                    }
                  }
                case _ => Future.successful(Redirect(routes.PropertyDetailsSummaryController.view(id)))
              }
          }
        }
      }
    }
  }
}
