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

package controllers

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.AtedForms._
import models.SelectPeriod
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.DelegationService
import utils.AtedConstants._
import utils.PeriodUtils

import scala.concurrent.Future

trait SelectPeriodController extends BackLinkController with ClientHelper with AuthAction {

  def view: Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        val periods = PeriodUtils.getPeriods(new LocalDate(2015, 4, 1), LocalDate.now())
        dataCacheConnector.fetchAndGetFormData[SelectPeriod](RetrieveSelectPeriodFormId) map {
          case Some(data) => Ok(views.html.selectPeriod(selectPeriodForm.fill(data), periods, getBackLink))
          case _ => Ok(views.html.selectPeriod(selectPeriodForm, periods, getBackLink))
        }
      }
    }
  }

  def submit : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        val periods = PeriodUtils.getPeriods(new LocalDate(2015, 4, 1), LocalDate.now())
        selectPeriodForm.bindFromRequest.fold(
          formWithError => Future.successful(BadRequest(views.html.selectPeriod(formWithError, periods, getBackLink))),
          periodData => {
            dataCacheConnector.saveFormData[SelectPeriod](RetrieveSelectPeriodFormId, periodData)
            RedirectWithBackLink(
              ReturnTypeController.controllerId,
              controllers.routes.ReturnTypeController.view(periodData.period.get.toInt),
              Some(routes.SelectPeriodController.view().url)
            )
          }
        )
      }
    }
  }

  private def getBackLink(): Option[String] = {
    Some(routes.AccountSummaryController.view().url)
  }
}

object SelectPeriodController extends SelectPeriodController {
  val delegationService: DelegationService = DelegationService
  override val controllerId = "SelectPeriodController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
