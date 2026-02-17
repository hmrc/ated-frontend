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
import controllers.ControllerIds
import controllers.auth.{AuthAction, ClientHelper}
import controllers.editLiability.EditLiabilityHasValueChangedController
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models.PropertyDetailsTitle
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsTitleController @Inject()(mcc: MessagesControllerComponents,
                                               authAction: AuthAction,
                                               editLiabilityHasValueChangedController: EditLiabilityHasValueChangedController,
                                               propertyDetailsOwnedBeforeController: PropertyDetailsOwnedBeforeController,
                                               serviceInfoService: ServiceInfoService,
                                               val propertyDetailsService: PropertyDetailsService,
                                               val dataCacheService: DataCacheService,
                                               val backLinkCacheService: BackLinkCacheService,
                                               template: views.html.propertyDetails.propertyDetailsTitle)
                                              (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with ControllerIds with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = propertyDetailsTitleId

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              currentBackLink.flatMap { backLink =>
                dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                  val displayData = propertyDetails.title.getOrElse(new PropertyDetailsTitle(""))
                  Ok(template(id, propertyDetails.periodKey, propertyDetailsTitleForm.fill(displayData),
                    AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn), serviceInfoContent,
                    backLink))
                }
              }
          }
        }
      }
    }
  }


  def editFromSummary(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
              val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
              Future.successful(
                Ok(template(
                  id,
                  propertyDetails.periodKey,
                  propertyDetailsTitleForm.fill(propertyDetails.title.getOrElse(PropertyDetailsTitle(""))),
                  mode,
                  serviceInfoContent,
                  AtedUtils.getSummaryBackLink(id, None))
                )
              )
            }
        }
      }
    }
  }

  def save(id: String, periodKey: Int, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsTitleForm.bindFromRequest().fold(
            formWithError => {
              currentBackLink.map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink)))
            },
            propertyDetails => {
              val backLink = Some(controllers.propertyDetails.routes.PropertyDetailsTitleController.view(id).url)
              for {
                _ <- propertyDetailsService.saveDraftPropertyDetailsTitle(id, propertyDetails)
                result <-
                  if (AtedUtils.isEditSubmittedMode(mode)) {
                    redirectWithBackLink(
                      editLiabilityHasValueChangedController.controllerId,
                      controllers.editLiability.routes.EditLiabilityHasValueChangedController.view(id),
                      backLink)
                  } else {
                    redirectWithBackLink(
                      propertyDetailsOwnedBeforeController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(id),
                      backLink)
                  }
              } yield result
            }
          )
        }
      }
    }
  }
}
