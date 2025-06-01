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
import connectors.{BackLinkCacheService, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import models._
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants._
import utils.AtedUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsDateOfRevalueController @Inject()(mcc: MessagesControllerComponents,
                                                       authAction: AuthAction,
                                                       serviceInfoService: ServiceInfoService,
                                                       template: views.html.propertyDetails.propertyDetailsDateOfRevalue,
                                                       val propertyDetailsService: PropertyDetailsService,
                                                       val backLinkCacheConnector: BackLinkCacheService,
                                                       val dataCacheConnector: DataCacheConnector,
                                                       isFullTaxPeriodController: IsFullTaxPeriodController)
                                                      (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "PropertyDetailsDateOfRevalueController"
  val dateFields: (String, String) = ("dateOfRevalue", messages("ated.property-details-value.dateOfRevalue.messageKey"))

  implicit lazy val messages: Messages = MessagesImpl(mcc.langs.availables.head, messagesApi)

  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) => {
              currentBackLink.flatMap { backLink =>
                dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn).flatMap { isPrevReturn =>
                  dataCacheConnector.fetchAndGetFormData[DateOfRevalue](DateOfRevalueConstant).map { cachedDateOfRevalue =>
                    val dateOfRevalue = cachedDateOfRevalue.flatMap(_.dateOfRevalue)
                    Ok(template(id,
                      propertyDetails.periodKey,
                      propertyDetailsDateOfRevalueForm.fill(DateOfRevalue(dateOfRevalue)),
                      AtedUtils.getEditSubmittedMode(propertyDetails, isPrevReturn),
                      serviceInfoContent,
                      backLink))
                  }
                }
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
          validateDateOfRevalue(periodKey, propertyDetailsDateOfRevalueForm.bindFromRequest(), dateFields).fold(
            formWithError => {
              currentBackLink.map(backLink => BadRequest(template(id, periodKey, formWithError, mode, serviceInfoContent, backLink)))
            },
            dateOfRevalue => {
              dataCacheConnector.saveFormData[DateOfRevalue](DateOfRevalueConstant, dateOfRevalue)
              val propertyDetailsFuture: Future[PropertyDetailsRevalued] = for {
                hasPropertyBeenRevalued <- dataCacheConnector.fetchAndGetFormData[HasBeenRevalued](HasPropertyBeenRevalued)
                revaluedValue <- dataCacheConnector.fetchAndGetFormData[PropertyDetailsNewValuation](propertyDetailsNewValuationValue)
                dateOfChange <- dataCacheConnector.fetchAndGetFormData[DateOfChange](FortyThousandValueDateOfChange)
              } yield {
                PropertyDetailsRevalued(
                  isPropertyRevalued = hasPropertyBeenRevalued.flatMap(_.isPropertyRevalued),
                  revaluedValue = revaluedValue.flatMap(_.revaluedValue),
                  revaluedDate = dateOfRevalue.dateOfRevalue,
                  partAcqDispDate = dateOfChange.flatMap(_.dateOfChange)
                )
              }
              propertyDetailsFuture.flatMap { propertyDetails =>
                for {
                  _ <- propertyDetailsService.saveDraftPropertyDetailsRevalued(id, propertyDetails)
                  result <- redirectWithBackLink(
                    isFullTaxPeriodController.controllerId,
                    controllers.propertyDetails.routes.IsFullTaxPeriodController.view(id),
                    Some(controllers.propertyDetails.routes.PropertyDetailsDateOfRevalueController.view(id).url)
                  )
                } yield result
              }
            }
          )
        }
      }
    }
  }
}
