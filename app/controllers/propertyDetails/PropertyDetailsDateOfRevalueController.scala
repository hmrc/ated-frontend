/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import forms.PropertyDetailsForms._
import models.DateOfRevalue
import play.api.i18n.{Messages, MessagesImpl}
import play.twirl.api.HtmlFormat
import utils.AtedConstants.{DateOfRevalueConstant, SelectedPreviousReturn}
import utils.AtedUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsDateOfRevalueController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      serviceInfoService: ServiceInfoService,
                                                      template: views.html.propertyDetails.propertyDetailsDateOfRevalue,
                                                      val propertyDetailsService: PropertyDetailsService,
                                                      val backLinkCacheConnector: BackLinkCacheConnector,
                                                      val dataCacheConnector: DataCacheConnector,
                                                       isFullTaxPeriodController: IsFullTaxPeriodController)
                                                     (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsDateOfRevalueController"

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        if (appConfig.newRevaluedFeature) {
          serviceInfoService.getPartial.flatMap { serviceInfoContent =>
            propertyDetailsCacheResponse(id) {
              case PropertyDetailsCacheSuccessResponse(propertyDetails) => {
                currentBackLink.flatMap { backLink =>
                  dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).map { isPrevReturn =>
                    Ok(template(id,
                      propertyDetails.periodKey,
                      propertyDetailsDateOfRevalueForm.fill(DateOfRevalue(propertyDetails.value.flatMap(_.partAcqDispDate))),
                      AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                      serviceInfoContent,
                      backLink))
                  }
                }
              }
            }
          }
        } else Future.successful(Redirect(controllers.routes.HomeController.home()))
      }
    }
  }

  implicit lazy val messages: Messages = MessagesImpl(mcc.langs.availables.head, messagesApi)

  val dateFields: (String, String) = ("dateOfRevalue", messages("ated.property-details-value.dateOfRevalue.messageKey"))

  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        if (appConfig.newRevaluedFeature) {
          serviceInfoService.getPartial.flatMap { serviceInfoContent =>
            validateDateOfRevalue(periodKey, propertyDetailsDateOfRevalueForm.bindFromRequest(), dateFields).fold(
              formWithError => {
                currentBackLink.map(backLink => (BadRequest(template(id, periodKey, formWithError, None, HtmlFormat.empty, None))))
              },
              dateOfRevalue => {
                dataCacheConnector.saveFormData[DateOfRevalue](DateOfRevalueConstant, dateOfRevalue)
                redirectWithBackLink(
                  isFullTaxPeriodController.controllerId,
                  controllers.propertyDetails.routes.IsFullTaxPeriodController.view(id),
                  Some(controllers.propertyDetails.routes.PropertyDetailsDateOfRevalueController.view(id).url)
                )
              }
            )
          }
        } else Future.successful(Redirect(controllers.routes.HomeController.home()))
      }
    }
  }
}
