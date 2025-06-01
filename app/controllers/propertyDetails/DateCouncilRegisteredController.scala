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
import connectors.{BackLinkCacheService, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import models.{DateCouncilRegistered, DateFirstOccupiedKnown}
import java.time.LocalDate
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.{NewBuildCouncilRegisteredDate, NewBuildFirstOccupiedDateKnown, SelectedPreviousReturn}
import utils.AtedUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DateCouncilRegisteredController @Inject()(val mcc: MessagesControllerComponents,
                                                authAction: AuthAction,
                                                serviceInfoService: ServiceInfoService,
                                                val propertyDetailsService: PropertyDetailsService,
                                                val dataCacheConnector: DataCacheConnector,
                                                val backLinkCacheConnector: BackLinkCacheService,
                                                template: views.html.propertyDetails.dateCouncilRegistered)
                                                      (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithUnsafeDefaultFormBinding with StoreNewBuildDates {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = DateCouncilRegisteredControllerId

  implicit lazy val messages: Messages = MessagesImpl(mcc.langs.availables.head, messagesApi)

  val dateFields = Seq(("dateCouncilRegistered", Messages("ated.property-details.council-registered-date.messageKey")))

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                val dcr: Option[LocalDate] = propertyDetails.value.flatMap(_.localAuthRegDate)
                Ok(template(id,
                  propertyDetails.periodKey,
                  dateCouncilRegisteredForm.fill(DateCouncilRegistered(dcr)),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  serviceInfoContent,
                  backLink)
                )
              }
            }
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction {
      implicit authContext => {
        ensureClientContext {
          serviceInfoService.getPartial.flatMap { serviceInfoContent =>
            PropertyDetailsForms.validateNewBuildCouncilRegisteredDate(periodKey, dateCouncilRegisteredForm.bindFromRequest(), dateFields).fold(
              formWithError =>
                currentBackLink.map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink))),
              form =>
                dataCacheConnector.saveFormData[DateCouncilRegistered](NewBuildCouncilRegisteredDate, form).flatMap { _ =>
                    storeNewBuildDatesFromCache(id).flatMap { _ =>
                      dataCacheConnector.fetchAndGetFormData[DateFirstOccupiedKnown](NewBuildFirstOccupiedDateKnown).flatMap {
                        case Some(DateFirstOccupiedKnown(Some(true))) =>
                          redirectWithBackLink(
                            EarliestStartDateInUseControllerId,
                            controllers.propertyDetails.routes.EarliestStartDateInUseController.view(id),
                            Some(controllers.propertyDetails.routes.DateCouncilRegisteredController.view(id).url)
                          )
                        case _ =>
                          redirectWithBackLink(
                            NewBuildValueControllerId,
                            controllers.propertyDetails.routes.PropertyDetailsNewBuildValueController.view(id),
                            Some(controllers.propertyDetails.routes.DateCouncilRegisteredController.view(id).url)
                          )
                      }
                    }
                }
            )
          }
        }
      }
    }
  }
}

