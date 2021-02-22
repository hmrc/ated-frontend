/*
 * Copyright 2021 HM Revenue & Customs
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
import models.Reliefs
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReliefsService, ServiceInfoService}
import uk.gov.hmrc.http.ForbiddenException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedUtils._
import utils.{PeriodUtils, ReliefsUtils}

import javax.inject.Inject
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext

class ChooseReliefsController @Inject()(mcc: MessagesControllerComponents,
                                        authAction: AuthAction,
                                        avoidanceSchemeBeingUsedController: AvoidanceSchemeBeingUsedController,
                                        serviceInfoService: ServiceInfoService,
                                        val reliefsService: ReliefsService,
                                        val dataCacheConnector: DataCacheConnector,
                                        val backLinkCacheConnector: BackLinkCacheConnector,
                                        val templateInvalidPeriodKey: views.html.reliefs.invalidPeriodKey,
                                        template: views.html.reliefs.chooseReliefs)
                                       (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkController with ReliefHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  override val controllerId: String = "ChooseReliefsController"

  def view(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            backLink <- currentBackLink
            serviceInfoContent <- serviceInfoService.getPartial
            retrievedData <- reliefsService.retrieveDraftReliefs(authContext.atedReferenceNumber, periodKey)
          } yield {
            retrievedData match {
              case Some(reliefs) =>
                Ok(template(reliefs.periodKey, reliefsForm.fill(reliefs.reliefs), PeriodUtils.periodStartDate(periodKey), serviceInfoContent, backLink))
              case _ => Ok(template(periodKey, reliefsForm.fill(Reliefs(periodKey)), PeriodUtils.periodStartDate(periodKey), serviceInfoContent, backLink))
            }
          }
        }
      }
    }
  }

  def editFromSummary(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        validatePeriodKey(periodKey) {
          for {
            retrievedData <- reliefsService.retrieveDraftReliefs(authContext.atedReferenceNumber, periodKey)
            serviceInfoContent <- serviceInfoService.getPartial
          } yield {
            val backLink = Some(controllers.reliefs.routes.ReliefsSummaryController.view(periodKey).url)
            retrievedData match {
              case Some(reliefs) =>
                Ok(template(reliefs.periodKey, reliefsForm.fill(reliefs.reliefs), PeriodUtils.periodStartDate(periodKey), serviceInfoContent, backLink))
              case _ => Ok(template(periodKey, reliefsForm.fill(Reliefs(periodKey)), PeriodUtils.periodStartDate(periodKey), serviceInfoContent, backLink))
            }
          }
        }
      }
    } recover {
      case _: ForbiddenException =>
        logger.warn("[ChooseReliefsController][editFromSummary] Forbidden exception")
        authAction.unauthorisedUrl()
    }
  }

  def send(periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          validatePeriodKey(periodKey) {
            val data = addParamsToRequest(Map("periodKey" -> ArrayBuffer(periodKey.toString)))
            reliefsForm.bindFromRequest(ReliefsUtils.cleanDateTuples(data.get)).fold(
              formWithError =>
                currentBackLink.map { backLink =>
                  BadRequest(template(periodKey, formWithError, PeriodUtils.periodStartDate(periodKey), serviceInfoContent, backLink))
                },
              reliefs => {
                for {
                  _ <- reliefsService.saveDraftReliefs(authContext.atedReferenceNumber, periodKey, reliefs)
                  result <-
                    redirectWithBackLink(avoidanceSchemeBeingUsedController.controllerId,
                      controllers.reliefs.routes.AvoidanceSchemeBeingUsedController.view(periodKey),
                      Some(routes.ChooseReliefsController.view(periodKey).url))
                } yield {
                  result
                }
              }
            )
          }
        }
      }
    } recover {
      case _: ForbiddenException =>
        logger.warn("[ChooseReliefsController][send] Forbidden exception")
        authAction.unauthorisedUrl()
    }
  }
}
