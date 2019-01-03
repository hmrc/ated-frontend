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

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedRegime, ClientHelper}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import org.joda.time.LocalDate
import services.PropertyDetailsService
import utils.AtedUtils
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait PeriodChooseReliefController extends PropertyDetailsHelpers with ClientHelper {


  def add(id: String, periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
        ensureClientContext(Future.successful(Ok(views.html.propertyDetails.periodChooseRelief(id, periodKey, periodChooseReliefForm, getBackLink(id)))))
  }

  def save(id: String, periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
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

  private def getBackLink(id: String) = {
    Some(controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id).url)
  }
}

object PeriodChooseReliefController extends PeriodChooseReliefController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "PeriodChooseReliefController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
