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
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models.PropertyDetailsNewBuildValue
import org.joda.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import utils.AtedUtils.getEarliestDate
import views.html
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.ExecutionContext


class PropertyDetailsNewBuildValueController @Inject()(mcc: MessagesControllerComponents,
                                                       authAction: AuthAction,
                                                       propertyDetailsProfessionallyValuedController: PropertyDetailsProfessionallyValuedController,
                                                       serviceInfoService: ServiceInfoService,
                                                       val propertyDetailsService: PropertyDetailsService,
                                                       val dataCacheConnector: DataCacheConnector,
                                                       val backLinkCacheConnector: BackLinkCacheConnector,
                                                       template: html.propertyDetails.propertyDetailsNewBuildValue)
                                                      (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = NewBuildValueControllerId

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => currentBackLink.flatMap { backLink =>
              dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                val displayData = PropertyDetailsNewBuildValue(propertyDetails.value.flatMap(_.newBuildValue))

                val newBuildDate = propertyDetails.value.flatMap(_.newBuildDate).getOrElse(new LocalDate())
                val localRegDate = propertyDetails.value.flatMap(_.localAuthRegDate).getOrElse(new LocalDate())

                val dynamicDate = getEarliestDate(newBuildDate, localRegDate)

                Ok(template(id,
                  propertyDetails.periodKey,
                  propertyDetailsNewBuildValueForm.fill(displayData),
                  AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                  serviceInfoContent,
                  backLink,
                  dynamicDate)
                )
              }
            }
          }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String], date: LocalDate): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction {
      implicit authContext => {
        ensureClientContext {
          serviceInfoService.getPartial.flatMap { serviceInfoContent =>
            propertyDetailsNewBuildValueForm.bindFromRequest().fold(
              formWithError => {
                currentBackLink.map(backLink =>
                  BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink, date))
                )
              },
              propertyDetails => {
                for {
                  _ <- propertyDetailsService.saveDraftPropertyDetailsNewBuildValue(id, propertyDetails)
                  result <-
                    redirectWithBackLink(
                      propertyDetailsProfessionallyValuedController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsProfessionallyValuedController.view(id),
                      Some(controllers.propertyDetails.routes.PropertyDetailsNewBuildValueController.view(id).url)
                    )
                } yield result
              }
            )
          }
        }
      }
    }
  }

}

