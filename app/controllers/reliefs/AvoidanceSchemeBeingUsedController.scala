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
import connectors.{BackLinkCacheService, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import forms.ReliefForms.isTaxAvoidanceForm

import javax.inject.Inject
import models.IsTaxAvoidance
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReliefsService, ServiceInfoService}
import uk.gov.hmrc.http.ForbiddenException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AtedUtils, PeriodUtils}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

class AvoidanceSchemeBeingUsedController @Inject()(mcc: MessagesControllerComponents,
                                                   serviceInfoService: ServiceInfoService,
                                                   authAction: AuthAction,
                                                   avoidanceSchemesController: AvoidanceSchemesController,
                                                   val reliefsService: ReliefsService,
                                                   val dataCacheConnector: DataCacheConnector,
                                                   val backLinkCacheConnector: BackLinkCacheService,
                                                   template: views.html.reliefs.avoidanceSchemeBeingUsed,
                                                   val templateInvalidPeriodKey: views.html.reliefs.invalidPeriodKey)
                                                  (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkController with ReliefHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId: String = "AvoidanceSchemeBeingUsedController"


  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          val atedRefNum = authContext.atedReferenceNumber
          for {
            backLink <- currentBackLink
            serviceInfoContent <- serviceInfoService.getPartial
            retrievedData <- reliefsService.retrieveDraftReliefs(atedRefNum, periodKey)
          } yield {
            val isAvoidanceScheme = IsTaxAvoidance(retrievedData.flatMap(_.reliefs.isAvoidanceScheme))
            Ok(template(periodKey, isTaxAvoidanceForm.fill(isAvoidanceScheme),
              PeriodUtils.periodStartDate(periodKey), serviceInfoContent, backLink))
          }
        }
      }
    } recover {
      case _: ForbiddenException     =>
        logger.warn("[AvoidanceSchemeBeingUsedController][view] Forbidden exception")
        authAction.unauthorisedUrl()
    }
  }

  def editFromSummary(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            retrievedData <- reliefsService.retrieveDraftReliefs(authContext.atedReferenceNumber, periodKey)
            result <- retrievedData.flatMap(_.reliefs.isAvoidanceScheme) match {
              case Some(true) =>
                redirectWithBackLink(
                  avoidanceSchemesController.controllerId,
                  controllers.reliefs.routes.AvoidanceSchemesController.view(periodKey),
                  Some(routes.ReliefsSummaryController.view(periodKey).url)
                )
              case _ =>
                Future.successful(Redirect(controllers.reliefs.routes.AvoidanceSchemeBeingUsedController.view(periodKey)))
            }
          } yield result
        }
      }
    } recover {
      case _: ForbiddenException     =>
        logger.warn("[AvoidanceSchemeBeingUsedController][editFromSummary] Forbidden exception")
        authAction.unauthorisedUrl()
    }
  }

  def send(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          validatePeriodKey(periodKey) {
            val data = AtedUtils.addParamsToRequest(Map("periodKey" -> ArrayBuffer(periodKey.toString).toSeq))
            isTaxAvoidanceForm.bindFromRequest(data.get).fold(
              formWithError =>
                currentBackLink.map(backLink =>
                  BadRequest(template(periodKey, formWithError, PeriodUtils.periodStartDate(periodKey), serviceInfoContent, backLink))
                ),
              isTaxAvoidance => {
                reliefsService.saveDraftIsTaxAvoidance(authContext.atedReferenceNumber, periodKey, isTaxAvoidance.isAvoidanceScheme.getOrElse(false)).
                  flatMap {
                    _ =>
                      redirectWithBackLink(
                        avoidanceSchemesController.controllerId,
                        controllers.reliefs.routes.AvoidanceSchemesController.view(periodKey),
                        Some(routes.AvoidanceSchemeBeingUsedController.view(periodKey).url)
                      )
                  }
              }
            )
          }
        }
      }
    } recover {
      case _: ForbiddenException     =>
        logger.warn("[AvoidanceSchemeBeingUsedController][send] Forbidden exception")
        authAction.unauthorisedUrl()
    }
  }

}


