/*
 * Copyright 2018 HM Revenue & Customs
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

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import forms.AtedForms._
import models.SelectPeriod
import org.joda.time.LocalDate

import scala.concurrent.Future
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.PeriodUtils
import utils.AtedConstants._
import views.html.selectPeriod

trait SelectPeriodController extends BackLinkController
  with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  def view = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        val periods = PeriodUtils.getPeriods(new LocalDate(2015, 4, 1), LocalDate.now())
        dataCacheConnector.fetchAndGetFormData[SelectPeriod](RetrieveSelectPeriodFormId) map {
          case Some(data) => Ok(views.html.selectPeriod(selectPeriodForm.fill(data), periods, getBackLink))
          case _ => Ok(views.html.selectPeriod(selectPeriodForm, periods, getBackLink))
        }
      }
  }

  def submit = AuthAction(AtedRegime) {
    implicit atedContext =>
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

  private def getBackLink() = {
    Some(routes.AccountSummaryController.view().url)
  }
}

object SelectPeriodController extends SelectPeriodController {
  val delegationConnector = FrontendDelegationConnector
  override val controllerId = "SelectPeriodController"
  override val backLinkCacheConnector = BackLinkCacheConnector
  val dataCacheConnector = DataCacheConnector
}
