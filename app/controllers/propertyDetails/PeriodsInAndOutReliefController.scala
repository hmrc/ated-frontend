/*
 * Copyright 2017 HM Revenue & Customs
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
import forms.PropertyDetailsForms._
import org.joda.time.LocalDate
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import utils.{AtedUtils, PeriodUtils}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.AtedConstants.SelectedPreviousReturn

import scala.concurrent.Future

trait PeriodsInAndOutReliefController extends PropertyDetailsHelpers with ClientHelper {

  def view(id: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                isPrevReturn match {
                  case Some(true) =>
                    Future.successful(Ok(views.html.propertyDetails.periodsInAndOutRelief(id, propertyDetails.periodKey,
                      periodsInAndOutReliefForm,
                      Nil,
                      AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                      backLink))
                    )
                  case _ =>
                    Future.successful(Ok(views.html.propertyDetails.periodsInAndOutRelief(id, propertyDetails.periodKey,
                      periodsInAndOutReliefForm,
                      PeriodUtils.getDisplayPeriods(propertyDetails.period),
                      AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                      backLink)))
                }
              }
            }
        }
      }
  }

  def deletePeriod(id: String, startDate: LocalDate) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        for {
          propertyDetails <- propertyDetailsService.deleteDraftPropertyDetailsPeriod(id, startDate)
          result <-
          currentBackLink.flatMap(backLink =>
            Future.successful(Ok(views.html.propertyDetails.periodsInAndOutRelief(id, propertyDetails.periodKey,
              periodsInAndOutReliefForm,
              PeriodUtils.getDisplayPeriods(propertyDetails.period),
              AtedUtils.getEditSubmittedMode(propertyDetails),
              backLink))
            ))
        } yield {
          result
        }
      }
  }

  def continue(id: String, periodKey: Int) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext(RedirectWithBackLink(
        PropertyDetailsTaxAvoidanceController.controllerId,
        controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceController.view(id),
        Some(controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id).url)
      ))
  }


}

object PeriodsInAndOutReliefController extends PeriodsInAndOutReliefController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  override val controllerId = "PeriodsInAndOutReliefController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
