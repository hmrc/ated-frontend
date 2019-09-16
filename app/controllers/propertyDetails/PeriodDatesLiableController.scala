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
import models._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait PeriodDatesLiableController extends PropertyDetailsHelpers with ClientHelper with AuthAction {

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val liabilityPeriod = propertyDetails.period.flatMap(_.liabilityPeriods.headOption)

            val filledForm = liabilityPeriod match {
              case Some(lineItem) => periodDatesLiableForm.fill(PropertyDetailsDatesLiable(lineItem.startDate, lineItem.endDate))
              case _ => periodDatesLiableForm
            }
            val mode = None
            getBackLink(id, mode).map { backLink =>
              Ok(views.html.propertyDetails.periodDatesLiable(id, propertyDetails.periodKey, filledForm,
                getTitle(mode), mode, backLink))
            }
        }
      }
    }
  }

  def add(id: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        val mode = Some("add")
        getBackLink(id, mode).map { backLink =>
          Ok(views.html.propertyDetails.periodDatesLiable(id, periodKey, periodDatesLiableForm,
            getTitle(mode), mode, backLink))
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      propertyDetailsCacheResponse(id) {
        case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
          val lineItems = propertyDetails.period.map(_.liabilityPeriods).getOrElse(Nil) ++ propertyDetails.period.map(_.reliefPeriods).getOrElse(Nil)
          PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, periodDatesLiableForm.bindFromRequest, mode == Some("add"), lineItems).fold(
            formWithError => {
              getBackLink(id, mode).map { backLink =>
                BadRequest(views.html.propertyDetails.periodDatesLiable(id, periodKey, formWithError,
                  getTitle(mode), mode, backLink))
              }
            },
            propertyDetails => {
              mode match {
                case Some("add") =>
                  for {
                    _ <- propertyDetailsService.addDraftPropertyDetailsDatesLiable(id, propertyDetails)
                  } yield {
                    Redirect(controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id))
                  }
                case _ =>
                  for {
                    _ <- propertyDetailsService.saveDraftPropertyDetailsDatesLiable(id, propertyDetails)
                    result <- ensureClientContext(RedirectWithBackLink(
                      PropertyDetailsTaxAvoidanceController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsTaxAvoidanceController.view(id),
                      Some(controllers.propertyDetails.routes.PeriodDatesLiableController.view(id).url)
                    ))
                  } yield {
                    result
                  }
              }
            }
          )
      }
    }
  }

  private def getBackLink(id: String, mode: Option[String] = None)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier) = {
    mode match {
      case Some("add") => Future.successful(Some(controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id).url))
      case _ => currentBackLink
    }
  }

  private def getTitle(mode: Option[String] = None) = {
    mode match {
      case Some("add") => Messages("ated.property-details-period.datesLiable.add.title")
      case _ => Messages("ated.property-details-period.datesLiable.title")
    }
  }

}

object PeriodDatesLiableController extends PeriodDatesLiableController {
  val delegationService: DelegationService = DelegationService
  val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val controllerId = "PeriodDatesLiableController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
