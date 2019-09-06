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
import forms.PropertyDetailsForms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, PropertyDetailsService}
import scala.concurrent.Future

trait PeriodChooseReliefController extends PropertyDetailsHelpers with ClientHelper with AuthAction {


  def add(id: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext(Future.successful(Ok(views.html.propertyDetails.periodChooseRelief(id, periodKey, periodChooseReliefForm, getBackLink(id)))))
    }
  }

  def save(id: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        periodChooseReliefForm.bindFromRequest.fold(
          formWithError =>
            Future.successful(
              BadRequest(views.html.propertyDetails.periodChooseRelief(id, periodKey, formWithError, getBackLink(id)))
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

  private def getBackLink(id: String) = {
    Some(controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id).url)
  }
}

object PeriodChooseReliefController extends PeriodChooseReliefController {
  val delegationService: DelegationService = DelegationService
  val propertyDetailsService: PropertyDetailsService.type = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector.type = DataCacheConnector
  override val controllerId: String = "PeriodChooseReliefController"
  override val backLinkCacheConnector: BackLinkCacheConnector.type = BackLinkCacheConnector
}
