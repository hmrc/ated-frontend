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

package controllers

import config.ApplicationConfig
import controllers.auth.{AuthAction, ClientHelper}
import forms.AtedForms._

import javax.inject.Inject
import models.SelectPeriod

import java.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, BackLinkService, DataCacheService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants._
import utils.PeriodUtils
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding

import scala.concurrent.{ExecutionContext, Future}

class SelectPeriodController @Inject()(mcc: MessagesControllerComponents,
                                       authAction: AuthAction,
                                       serviceInfoService: ServiceInfoService,
                                       val backLinkCacheService: BackLinkCacheService,
                                       val dataCacheService: DataCacheService,
                                       template: views.html.selectPeriod)
                                      (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkService with ClientHelper with ControllerIds with WithUnsafeDefaultFormBinding {

  implicit val ec : ExecutionContext = mcc.executionContext
  val controllerId = "SelectPeriodController"


  def currentDate: LocalDate = LocalDate.now()

  def view: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          val peakStartYear = PeriodUtils.calculatePeakStartYear(currentDate)
          val periods = PeriodUtils.getPeriods(peakStartYear)
          dataCacheService.fetchAndGetData[SelectPeriod](RetrieveSelectPeriodFormId) map {
            case Some(data) => Ok(template(selectPeriodForm.fill(data), periods, serviceInfoContent, getBackLink()))
            case _ => Ok(template(selectPeriodForm, periods, serviceInfoContent, getBackLink()))
          }
        }
      }
    }
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          val peakStartYear = PeriodUtils.calculatePeakStartYear()
          val periods = PeriodUtils.getPeriods(peakStartYear)
          selectPeriodForm.bindFromRequest().fold(
            formWithError => Future.successful(BadRequest(template(formWithError, periods, serviceInfoContent, getBackLink()))),
            periodData => {
              dataCacheService.saveFormData[SelectPeriod](RetrieveSelectPeriodFormId, periodData)
              redirectWithBackLink(
                returnTypeControllerId,
                controllers.routes.ReturnTypeController.view(periodData.period.get.toInt),
                Some(routes.SelectPeriodController.view.url)
              )
            }
          )
        }
      }
    }
  }

  private def getBackLink(): Option[String] = {
    Some(routes.PrevPeriodsSummaryController.view().url)
  }
}
