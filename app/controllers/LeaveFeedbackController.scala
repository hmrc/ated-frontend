/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.AuthAction
import javax.inject.Inject
import models.LeaveFeedback
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ServiceInfoService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}



class LeaveFeedbackController @Inject()(mcc: MessagesControllerComponents,
                                        authAction: AuthAction,
                                        serviceInfoService: ServiceInfoService,
                                        auditConnector: DefaultAuditConnector,
                                        template: views.html.feedback.leaveFeedback,
                                        templateThanks: views.html.feedback.thanks)
                                       (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with Auditable {

  val appName: String = "ated-frontend"
  val audit: Audit = new Audit(s"ATED:$appName-Feedback", auditConnector)
  implicit val ec : ExecutionContext = mcc.executionContext

  def view(returnUri: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedForNoEnrolments { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        Future.successful(Ok(template(LeaveFeedback.form, serviceInfoContent, returnUri)))
      }
    }
  }

  def submitFeedback(returnUri: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedForNoEnrolments { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        LeaveFeedback.form.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(template(formWithErrors, serviceInfoContent, returnUri))),
          value => {
            auditFeedback(value, returnUri)
            Future.successful(Redirect(routes.LeaveFeedbackController.thanks(returnUri)))
          }
        )
      }
    }
  }

  def auditFeedback(data: LeaveFeedback, returnUri: String)(implicit hc: HeaderCarrier): Unit = {
    sendDataEvent("ated-feedback", detail = Map(
      "summaryInfo" -> data.summaryInfo.mkString,
      "moreInfo" -> data.moreInfo.mkString,
      "experienceLevel" -> data.experienceLevel.toString,
      "referer" -> returnUri,
      "status" -> EventTypes.Succeeded
    ))
  }

  def thanks(returnUri: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedForNoEnrolments { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        Future.successful(Ok(templateThanks(returnUri, serviceInfoContent)))
      }
    }
  }
}
