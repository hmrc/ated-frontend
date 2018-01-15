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

package controllers.propertyDetails

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedRegime, ClientHelper}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait PeriodInReliefDatesController extends PropertyDetailsHelpers with ClientHelper {

  def add(id: String, periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
        ensureClientContext(Future.successful(Ok(views.html.propertyDetails.periodInReliefDates(id, periodKey, periodInReliefDatesForm, getBackLink(id, periodKey)))))
  }

  def save(id: String, periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
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
  private def getBackLink(id: String, periodKey: Int) = {
    Some(controllers.propertyDetails.routes.PeriodChooseReliefController.add(id, periodKey).url)
  }
}

object PeriodInReliefDatesController extends PeriodInReliefDatesController {
  val delegationConnector = FrontendDelegationConnector
  val dataCacheConnector = DataCacheConnector
  val propertyDetailsService = PropertyDetailsService
  override val controllerId = "PeriodInReliefDatesController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
