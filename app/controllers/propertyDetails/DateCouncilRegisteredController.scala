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
import javax.inject.{Singleton, Inject}
import models.DateCouncilRegistered
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.{NewBuildCouncilRegisteredDate, SelectedPreviousReturn}
import utils.AtedUtils
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}
import org.joda.time.LocalDate

@Singleton
class DateCouncilRegisteredController @Inject()(val mcc: MessagesControllerComponents,
                                                       authAction: AuthAction,
                                                       serviceInfoService: ServiceInfoService,
                                                       val propertyDetailsService: PropertyDetailsService,
                                                       val dataCacheConnector: DataCacheConnector,
                                                       val backLinkCacheConnector: BackLinkCacheConnector,
                                                       template: views.html.propertyDetails.dateCouncilRegistered)
                                                      (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithDefaultFormBinding with StoreNewBuildDates {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = DateCouncilRegisteredControllerId

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                val dcr: Option[LocalDate] = propertyDetails.value.flatMap(_.localAuthRegDate)
                Future.successful(Ok(template(id,
                  propertyDetails.periodKey,
                  dateCouncilRegisteredForm.fill(DateCouncilRegistered(dcr)),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  serviceInfoContent,
                  backLink)
                ))
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
            PropertyDetailsForms.validateNewBuildCouncilRegisteredDate(periodKey, dateCouncilRegisteredForm.bindFromRequest).fold(
              formWithError =>
                currentBackLink.map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink))),
              form =>
                dataCacheConnector.saveFormData[DateCouncilRegistered](NewBuildCouncilRegisteredDate, form).flatMap{_ =>
                  storeNewBuildDatesFromCache(id).flatMap{ _ =>
                    redirectWithBackLink(
                      EarliestStartDateInUseControllerId,
                      controllers.propertyDetails.routes.EarliestStartDateInUseController.view(id),
                      Some(controllers.propertyDetails.routes.DateCouncilRegisteredController .view(id).url)
                    )
                  }
                }
            )
          }
        }
      }
    }
  }
}

