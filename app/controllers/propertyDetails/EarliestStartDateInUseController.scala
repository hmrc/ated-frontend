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
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import scala.concurrent.ExecutionContext
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import services._
import org.joda.time.LocalDate

@Singleton
class EarliestStartDateInUseController @Inject()(mcc: MessagesControllerComponents,
                                                 authAction: AuthAction,
                                                 serviceInfoService: ServiceInfoService,
                                                 val propertyDetailsService: PropertyDetailsService,
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val backLinkCacheConnector: BackLinkCacheConnector,
                                                 view: views.html.propertyDetails.earliestStartDateInUse)
                                                (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId = EarliestStartDateInUseControllerId

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                val newBuildDate: LocalDate = propertyDetails.value.flatMap(_.newBuildDate).getOrElse(new LocalDate())
                val localRegDate: LocalDate = propertyDetails.value.flatMap(_.localAuthRegDate).getOrElse(new LocalDate())
                val dynamicDate = AtedUtils.getEarliestDate(newBuildDate, localRegDate)

                currentBackLink.map(backLink =>
                  Ok(view(id, dynamicDate, serviceInfoContent, AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn), backLink))
                )
              }
          }
        }
      }
    }
  }

  def continue(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction{ implicit authContext =>
      ensureClientContext {
        redirectWithBackLink(
          NewBuildValueControllerId,
          controllers.propertyDetails.routes.PropertyDetailsNewBuildValueController.view(id),
          Some(controllers.propertyDetails.routes.EarliestStartDateInUseController .view(id).url)
        )
      }
    }
  }

}
