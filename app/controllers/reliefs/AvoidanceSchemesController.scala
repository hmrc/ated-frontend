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

package controllers.reliefs

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import forms.ReliefForms._
import javax.inject.Inject
import models.TaxAvoidance
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReliefsService, ServiceInfoService}
import uk.gov.hmrc.http.ForbiddenException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.ExecutionContext

class AvoidanceSchemesController @Inject()(mcc: MessagesControllerComponents,
                                           reliefsSummaryController: ReliefsSummaryController,
                                           authAction: AuthAction,
                                           serviceInfoService: ServiceInfoService,
                                           val reliefsService: ReliefsService,
                                           val dataCacheConnector: DataCacheConnector,
                                           val backLinkCacheConnector: BackLinkCacheConnector,
                                           template: views.html.reliefs.avoidanceSchemes,
                                           val templateInvalidPeriodKey: views.html.reliefs.invalidPeriodKey)
                                          (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkController with ReliefHelpers with ClientHelper with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "AvoidanceSchemesController"

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        validatePeriodKey(periodKey) {
          reliefsService.retrieveDraftReliefs(authContext.atedReferenceNumber, periodKey).flatMap {
            case Some(x) if x.reliefs.isAvoidanceScheme.contains(true) =>
              currentBackLink.map(backLink =>
                Ok(template(x.periodKey, taxAvoidanceForm.fill(x.taxAvoidance), serviceInfoContent, backLink)(Some(x)))
              )
            case _ =>
              reliefsService.saveDraftTaxAvoidance(authContext.atedReferenceNumber, periodKey, TaxAvoidance())
              forwardBackLinkToNextPage(
                reliefsSummaryController.controllerId,
                controllers.reliefs.routes.ReliefsSummaryController.view(periodKey)
              )
          }
        }
      } recover {
        case _: ForbiddenException =>
          logger.warn("[AvoidanceSchemesController][view] Forbidden exception")
          authAction.unauthorisedUrl()
      }
    }
  }

  def submit(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          validateTaxAvoidance(taxAvoidanceForm.bindFromRequest(), periodKey).fold(
            formWithError => {
              for {
                backLink <- currentBackLink
                serviceInfoContent <- serviceInfoService.getPartial
                retrievedData <- reliefsService.retrieveDraftReliefs(authContext.atedReferenceNumber, periodKey)
              } yield {
                BadRequest(template(periodKey, formWithError, serviceInfoContent, backLink)(retrievedData))
              }
            },
            taxAvoidance => {
              for {
                _ <- reliefsService.saveDraftTaxAvoidance(authContext.atedReferenceNumber, periodKey, taxAvoidance)
                result <-
                redirectWithBackLink(
                  reliefsSummaryController.controllerId,
                  controllers.reliefs.routes.ReliefsSummaryController.view(periodKey),
                  Some(routes.AvoidanceSchemesController.view(periodKey).url)
                )
              } yield {
                result
              }
            }
          )
        }
      }
    } recover {
      case _: ForbiddenException     =>
        logger.warn("[AvoidanceSchemesController][submit] Forbidden exception")
        authAction.unauthorisedUrl()
    }
  }
}
