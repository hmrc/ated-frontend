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

package controllers.propertyDetails

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class PeriodInReliefDatesController @Inject()(mcc: MessagesControllerComponents,
                                              authAction: AuthAction,
                                              serviceInfoService: ServiceInfoService,
                                              val dataCacheConnector: DataCacheConnector,
                                              val propertyDetailsService: PropertyDetailsService,
                                              val backLinkCacheConnector: BackLinkCacheConnector)
                                             (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PeriodInReliefDatesController"

  def add(id: String, periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        ensureClientContext(Future.successful(Ok(views.html.propertyDetails.periodInReliefDates(id,
          periodKey, periodInReliefDatesForm, serviceInfoContent, getBackLink(id, periodKey)))))
      }
    }
  }

  def save(id: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              val lineItems = propertyDetails.period.map(_.liabilityPeriods).getOrElse(Nil) ++ propertyDetails.period.map(_.reliefPeriods).getOrElse(Nil)
              PropertyDetailsForms.validatePropertyDetailsDatesInRelief(periodKey, periodInReliefDatesForm.bindFromRequest, lineItems).fold(
                formWithError => {
                  Future.successful(BadRequest(views.html.propertyDetails.periodInReliefDates(id, periodKey, formWithError, serviceInfoContent, getBackLink(id, periodKey))))
                },
                datesInRelief => {
                  for {
                    _ <- propertyDetailsService.addDraftPropertyDetailsDatesInRelief(id, datesInRelief)
                  } yield {
                    Redirect(controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id))
                  }
                }
              )
          }
        }
      }
    }
  }
  private def getBackLink(id: String, periodKey: Int) = {
    Some(controllers.propertyDetails.routes.PeriodChooseReliefController.add(id, periodKey).url)
  }
}


