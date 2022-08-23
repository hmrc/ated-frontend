/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.{AuthAction, ClientHelper}
import forms.AtedForms.YesNoQuestionDraftDeleteForm
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PropertyDetailsService, ReliefsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}

class DraftDeleteConfirmationController @Inject()(mcc: MessagesControllerComponents,
                                                  authAction: AuthAction,
                                                  propertyDetailsService: PropertyDetailsService,
                                                  reliefsService: ReliefsService,
                                                  serviceInfoService: ServiceInfoService,
                                                  val dataCacheConnector: DataCacheConnector,
                                                  template: views.html.confirmDeleteDraft)
                                                 (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with ClientHelper with WithDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  def view(id: Option[String], periodKey: Int, returnType: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          Future.successful(Ok(template(new YesNoQuestionDraftDeleteForm().yesNoQuestionForm, id, periodKey,
            returnType, serviceInfoContent, getBackLink(id, periodKey, returnType))))
        }
      }
    }
  }

  def submit(id: Option[String], periodKey: Int, returnType: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          val form = new YesNoQuestionDraftDeleteForm()
          form.yesNoQuestionForm.bindFromRequest.fold(
            formWithError =>
              Future.successful(BadRequest(template(formWithError, id, periodKey, returnType, serviceInfoContent, getBackLink(id, periodKey, returnType)))
              ),
            data => {
              val deleteDraft = data.yesNo.getOrElse(false)
              (deleteDraft, returnType) match {
                case (true, "charge") =>
                  propertyDetailsService.clearDraftReliefs(id.getOrElse(throw new RuntimeException("No id found for draft return")))
                  Future.successful(Redirect(controllers.routes.PeriodSummaryController.view(periodKey)))
                case (true, "relief") =>
                  reliefsService.deleteDraftReliefs(periodKey)
                  Future.successful(Redirect(controllers.routes.PeriodSummaryController.view(periodKey)))
                case (false, "charge") =>
                  Future.successful(Redirect(controllers.propertyDetails.routes.PropertyDetailsSummaryController
                    .view(id.getOrElse(throw new RuntimeException("No id found for draft return")))))
                case (false, "relief") => Future.successful(Redirect(controllers.reliefs.routes.ReliefsSummaryController.view(periodKey)))
                case _ => throw new RuntimeException("Couldn't delete draft")
              }
            }
          )
        }
      }
    }
  }

  private def getBackLink(id: Option[String], periodKey: Int, returnType: String) = {
    returnType match {
      case "relief" => Some(controllers.reliefs.routes.ReliefsSummaryController.view(periodKey).url)
      case "charge" => Some(controllers.propertyDetails.routes.PropertyDetailsSummaryController
        .view(id.getOrElse(throw new RuntimeException("No id found for draft return"))).url)
    }
  }
}
