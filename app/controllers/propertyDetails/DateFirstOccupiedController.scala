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
import forms.{PropertyDetailsForms, ReliefForms}
import forms.PropertyDetailsForms._

import javax.inject.{Inject, Singleton}
import models.DateFirstOccupied
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import utils.AtedConstants.NewBuildFirstOccupiedDate
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding

import scala.concurrent.ExecutionContext
import org.joda.time.LocalDate
import play.api.i18n.{Messages, MessagesImpl}

@Singleton
class DateFirstOccupiedController @Inject()(mcc: MessagesControllerComponents,
                                                       authAction: AuthAction,
                                                       serviceInfoService: ServiceInfoService,
                                                       val propertyDetailsService: PropertyDetailsService,
                                                       val dataCacheConnector: DataCacheConnector,
                                                       val backLinkCacheConnector: BackLinkCacheConnector,
                                                       template: views.html.propertyDetails.dateFirstOccupied)
                                                      (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = DateFirstOccupiedControllerId

  implicit lazy val messages: Messages = MessagesImpl(mcc.langs.availables.head, messagesApi)

  val dateFields = Seq(("dateFirstOccupied", Messages("ated.property-details.first-occupied-date.messageKey")))

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                dataCacheConnector.fetchAndGetFormData[DateFirstOccupied](NewBuildFirstOccupiedDate).map { dateFirstOccupied =>
                  val dfo: Option[LocalDate] = dateFirstOccupied.map(_.dateFirstOccupied).getOrElse(propertyDetails.value.flatMap(_.newBuildDate))
                  Ok(template(id,
                    propertyDetails.periodKey,
                    dateFirstOccupiedForm.fill(DateFirstOccupied(dfo)),
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
  }

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction {
      implicit authContext => {
        ensureClientContext {
          serviceInfoService.getPartial.flatMap { serviceInfoContent =>
            PropertyDetailsForms.validateNewBuildFirstOccupiedDate(periodKey, dateFirstOccupiedForm.bindFromRequest, dateFields).fold(
              formWithError =>
                currentBackLink.map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink))),
              form =>
                dataCacheConnector.saveFormData[DateFirstOccupied](NewBuildFirstOccupiedDate, form).flatMap{_ =>
                  redirectWithBackLink(
                    DateCouncilRegisteredKnownControllerId,
                    controllers.propertyDetails.routes.DateCouncilRegisteredKnownController.view(id),
                    Some(controllers.propertyDetails.routes.DateFirstOccupiedController.view(id).url)
                  )
                }
            )
          }
        }
      }
    }
  }
}

