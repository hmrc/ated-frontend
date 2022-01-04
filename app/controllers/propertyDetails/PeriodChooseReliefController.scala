/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.PropertyDetailsForms._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class PeriodChooseReliefController @Inject()(mcc: MessagesControllerComponents,
                                             authAction: AuthAction,
                                             serviceInfoService: ServiceInfoService,
                                             val propertyDetailsService: PropertyDetailsService,
                                             val dataCacheConnector: DataCacheConnector,
                                             val backLinkCacheConnector: BackLinkCacheConnector,
                                             template: views.html.propertyDetails.periodChooseRelief)
                                            (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "PeriodChooseReliefController"

  def add(id: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        ensureClientContext(Future.successful(Ok(template(id, periodKey, periodChooseReliefForm, serviceInfoContent, getBackLink(id)))))
      }
    }
  }

  def save(id: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          periodChooseReliefForm.bindFromRequest.fold(
            formWithError =>
              Future.successful(
                BadRequest(template(id, periodKey, formWithError, serviceInfoContent, getBackLink(id)))
              ),
            chosenRelief => {
              for {
                _ <- propertyDetailsService.storeChosenRelief(chosenRelief)
              } yield {
                Redirect(controllers.propertyDetails.routes.PeriodInReliefDatesController.add(id, periodKey))
              }
            }
          )
        }
      }
    }
  }

  private def getBackLink(id: String) = {
    Some(controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id).url)
  }
}
