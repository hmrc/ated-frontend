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

package controllers.propertyDetails

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._

import javax.inject.Inject
import models._
import play.api.i18n.{I18nSupport, Messages, MessagesImpl}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding

import scala.concurrent.{ExecutionContext, Future}

class PeriodDatesLiableController @Inject()(mcc: MessagesControllerComponents,
                                            authAction: AuthAction,
                                            propertyDetailsTaxAvoidanceController: PropertyDetailsTaxAvoidanceController,
                                            serviceInfoService: ServiceInfoService,
                                            val propertyDetailsService: PropertyDetailsService,
                                            val dataCacheConnector: DataCacheConnector,
                                            val backLinkCacheConnector: BackLinkCacheConnector,
                                            template: views.html.propertyDetails.periodDatesLiable)
                                           (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with I18nSupport with WithDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PeriodDatesLiableController"

  implicit lazy val messages: Messages = MessagesImpl(mcc.langs.availables.head, messagesApi)

  val dateFields = Seq(("startDate", Messages("ated.property-details-period.datesLiable.startDate.messageKey")),
    ("endDate", Messages("ated.property-details-period.datesLiable.endDate.messageKey")))

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              val liabilityPeriod = propertyDetails.period.flatMap(_.liabilityPeriods.headOption)

              val filledForm = liabilityPeriod match {
                case Some(lineItem) => periodDatesLiableForm.fill(PropertyDetailsDatesLiable(Some(lineItem.startDate), Some(lineItem.endDate)))
                case _ => periodDatesLiableForm
              }
              val mode = None
              getBackLink(id, mode).map { backLink =>
                Ok(template(id, propertyDetails.periodKey, filledForm,
                  getTitle(mode), mode, serviceInfoContent, backLink))
              }
          }
        }
      }
    }
  }

  def add(id: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          val mode = Some("add")
          getBackLink(id, mode).map { backLink =>
            Ok(template(id, periodKey, periodDatesLiableForm,
              getTitle(mode), mode, serviceInfoContent, backLink))
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val lineItems = propertyDetails.period.map(_.liabilityPeriods).getOrElse(Nil) ++ propertyDetails.period.map(_.reliefPeriods).getOrElse(Nil)
            PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, periodDatesLiableForm.bindFromRequest, mode.contains("add"), lineItems, dateFields).fold(
              formWithError => {
                getBackLink(id, mode).map { backLink =>
                  BadRequest(template(id, periodKey, formWithError,
                    getTitle(mode), mode, serviceInfoContent, backLink))
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
                      result <- ensureClientContext(redirectWithBackLink(
                        propertyDetailsTaxAvoidanceController.controllerId,
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
  }

  private def getBackLink(id: String, mode: Option[String])(implicit hc: HeaderCarrier) = {
    mode match {
      case Some("add") => Future.successful(Some(controllers.propertyDetails.routes.PeriodsInAndOutReliefController.view(id).url))
      case _ => currentBackLink
    }
  }

  private def getTitle(mode: Option[String]) = {
    mode match {
      case Some("add") => "ated.property-details-period.datesLiable.add.title"
      case _ => "ated.property-details-period.datesLiable.title"
    }
  }

}
