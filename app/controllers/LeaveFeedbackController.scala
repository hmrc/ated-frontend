/*
 * Copyright 2019 HM Revenue & Customs
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
import config.AtedFrontendAuditConnector
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, AtedSubscriptionNotNeededRegime}
import models.LeaveFeedback
import play.api._
import play.api.mvc._
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.AppName
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier



trait LeaveFeedbackController extends AtedBaseController with AtedFrontendAuthHelpers with Auditable {

  def view(returnUri: String) = AuthAction(AtedSubscriptionNotNeededRegime) {
    implicit atedContext =>
      Future.successful(Ok(views.html.feedback.leaveFeedback(LeaveFeedback.form, returnUri)))
  }

  def submitFeedback(returnUri: String): Action[AnyContent] = AuthAction(AtedSubscriptionNotNeededRegime) {
    implicit atedContext =>
      LeaveFeedback.form.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.feedback.leaveFeedback(formWithErrors, returnUri))),
        value => {
          auditFeedback(value, returnUri)
          Future.successful(Redirect(routes.LeaveFeedbackController.thanks(returnUri)))
        }
      )
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

  def thanks(returnUri: String) = AuthAction(AtedSubscriptionNotNeededRegime) {
    implicit atedContext =>
      Future.successful(Ok(views.html.feedback.thanks(returnUri)))
  }

}

object LeaveFeedbackController extends LeaveFeedbackController {
  val audit: Audit = new Audit(s"ATED:${AppName.appName}-Feedback", AtedFrontendAuditConnector)
  val appName = AppName.appName
}
