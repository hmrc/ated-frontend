/*
 * Copyright 2026 HM Revenue & Customs
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
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms.*

import javax.inject.Inject
import models.PropertyDetailsOwnedBefore
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.SelectedPreviousReturn
import utils.AtedUtils
import utils.AtedUtils.EDIT_FROM_SUMMARY

import scala.concurrent.{ExecutionContext, Future}


class PropertyDetailsOwnedBeforeController @Inject()(mcc: MessagesControllerComponents,
                                                     authAction: AuthAction,
                                                     propertyDetailsNewBuildController: PropertyDetailsNewBuildController,
                                                     propertyDetailsProfessionallyValuedController: PropertyDetailsProfessionallyValuedController,
                                                     serviceInfoService: ServiceInfoService,
                                                     val propertyDetailsService: PropertyDetailsService,
                                                     val dataCacheService: DataCacheService,
                                                     val backLinkCacheService: BackLinkCacheService,
                                                     template: views.html.propertyDetails.propertyDetailsOwnedBefore)
                                                    (using val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  given ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsOwnedBeforeController"


  def view(id: String, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              currentBackLink.flatMap { backLink =>
                dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                  dataCacheService.fetchAndGetData[String]("EditSummaryEntryController").map { entryController =>
                    val modeView = if (!mode.contains(EDIT_FROM_SUMMARY)) {
                      AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn)
                    } else {
                      mode
                    }
                    val isSummaryEntryPage =
                      mode.contains(EDIT_FROM_SUMMARY) &&
                        entryController.contains(controllerId)

                    val backLinkView =
                      if (isSummaryEntryPage) {
                        AtedUtils.getSummaryBackLink(id, Some(EDIT_FROM_SUMMARY))
                      } else {
                        backLink
                      }

                    val displayData = PropertyDetailsOwnedBefore(propertyDetails.value.flatMap(_.isOwnedBeforePolicyYear),
                      propertyDetails.value.flatMap(_.ownedBeforePolicyYearValue))
                    Ok(template(id,
                      propertyDetails.periodKey,
                      propertyDetailsOwnedBeforeForm(propertyDetails.periodKey).fill(displayData),
                      modeView,
                      serviceInfoContent,
                      backLinkView)
                    )
                  }
                }
              }
          }
        }
      }
    }
  }

  def editFromSummary(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              dataCacheService.fetchAndGetData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                for {
                  _ <- dataCacheService.saveFormData(
                    "EditSummaryEntryController",
                    controllerId
                  )
                } yield {
                  val displayData = PropertyDetailsOwnedBefore(propertyDetails.value.flatMap(_.isOwnedBeforePolicyYear),
                    propertyDetails.value.flatMap(_.ownedBeforePolicyYearValue))
                  val mode = AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn).getOrElse(EDIT_FROM_SUMMARY)
                  Ok(
                    template(id,
                      propertyDetails.periodKey,
                      propertyDetailsOwnedBeforeForm(propertyDetails.periodKey).fill(displayData),
                      Some(mode),
                      serviceInfoContent,
                      AtedUtils.getSummaryBackLink(id, None))
                  )
                }
              }
          }
        }
      }
    }
  }

      def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
        authAction.authorisedAction { implicit authContext =>
          ensureClientContext {
            serviceInfoService.getPartial.flatMap { serviceInfoContent =>
              PropertyDetailsForms.validatePropertyDetailsOwnedBefore(propertyDetailsOwnedBeforeForm(periodKey).bindFromRequest()).fold(
                formWithError => {
                  currentBackLink.map(backLink =>
                    BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink))
                  )
                },
                propertyDetails => {
                  for {
                    _ <- propertyDetailsService.saveDraftPropertyDetailsOwnedBefore(id, propertyDetails)
                    result <-
                      if (propertyDetails.isOwnedBeforePolicyYear.getOrElse(false)) {
                        redirectWithBackLink(
                          propertyDetailsProfessionallyValuedController.controllerId,
                          controllers.propertyDetails.routes.PropertyDetailsProfessionallyValuedController.view(id, mode),
                          Some(controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(id, mode).url))
                      } else {
                        redirectWithBackLink(
                          propertyDetailsNewBuildController.controllerId,
                          controllers.propertyDetails.routes.PropertyDetailsNewBuildController.view(id, mode),
                          Some(controllers.propertyDetails.routes.PropertyDetailsOwnedBeforeController.view(id, mode).url))
                      }
                  } yield result
                }
              )
            }
          }
        }
      }
    }
