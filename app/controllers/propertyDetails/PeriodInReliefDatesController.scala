/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}

import scala.concurrent.Future

trait PeriodInReliefDatesController extends PropertyDetailsHelpers with ClientHelper with AuthAction {

  def add(id: String, periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext(Future.successful(Ok(views.html.propertyDetails.periodInReliefDates(id,
        periodKey, periodInReliefDatesForm, getBackLink(id, periodKey)))))
    }
  }
  def save(id: String, periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val lineItems = propertyDetails.period.map(_.liabilityPeriods).getOrElse(Nil) ++ propertyDetails.period.map(_.reliefPeriods).getOrElse(Nil)
            PropertyDetailsForms.validatePropertyDetailsDatesInRelief(periodKey, periodInReliefDatesForm.bindFromRequest, lineItems).fold(
              formWithError => {
                Future.successful(BadRequest(views.html.propertyDetails.periodInReliefDates(id, periodKey, formWithError, getBackLink(id, periodKey))))
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
  private def getBackLink(id: String, periodKey: Int) = {
    Some(controllers.propertyDetails.routes.PeriodChooseReliefController.add(id, periodKey).url)
  }
}

object PeriodInReliefDatesController extends PeriodInReliefDatesController {
  val delegationService: DelegationService = DelegationService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val propertyDetailsService:PropertyDetailsService = PropertyDetailsService
  override val controllerId: String = "PeriodInReliefDatesController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
