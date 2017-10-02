/*
 * Copyright 2017 HM Revenue & Customs
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
import models.Questionnaire
import play.api.Logger
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.audit.model.{EventTypes, Audit}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.http.HeaderCarrier

trait QuestionnaireController extends FrontendController with Auditable {

  def showQuestionnaire: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      Ok(views.html.questionnaire.feedbackQuestionnaire(Questionnaire.form))
  }

  def submitQuestionnaire: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      Questionnaire.form.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(views.html.questionnaire.feedbackQuestionnaire(formWithErrors))
        },
        value => {
          auditQuestionnaire(value)
          Redirect(routes.QuestionnaireController.feedbackThankYou())
        }
      )
  }

  private def auditQuestionnaire(value: Questionnaire)(implicit hc: HeaderCarrier) = {
    sendDataEvent("ated-exit-survey", detail = Map(
      "easyToUse" -> value.easyToUse.mkString,
      "satisfactionLevel" -> value.satisfactionLevel.mkString,
      "howCanWeImprove" -> value.howCanWeImprove.mkString,
      "status" -> EventTypes.Succeeded
    ))
  }

  def feedbackThankYou = UnauthorisedAction {
    implicit request =>
      Ok(views.html.questionnaire.feedbackThankYou())
  }

}

object QuestionnaireController extends QuestionnaireController {
  val audit: Audit = new Audit(s"ATED:${AppName.appName}-Questionnaire", AtedFrontendAuditConnector)
  val appName: String = AppName.appName
}
